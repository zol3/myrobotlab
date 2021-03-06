package org.myrobotlab.service;

import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.headtracking.OculusHeadTracking;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.oculus.OculusDisplay;
import org.myrobotlab.opencv.OpenCVFilterAffine;
import org.myrobotlab.opencv.OpenCVFilterResize;
import org.myrobotlab.opencv.OpenCVFilterTranspose;
import org.myrobotlab.service.data.OculusData;
import org.myrobotlab.service.interfaces.OculusDataPublisher;
import org.myrobotlab.service.interfaces.PointPublisher;
import org.slf4j.Logger;

import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.TrackingState;

/**
 * The OculusRift service for MyRobotLab.
 * 
 * Currently this service only exposed the head tracking information
 * from the rift.  The Yaw, Pitch and Roll are exposed. 
 * Yaw - twist around vertical axis (look left/right)
 * Pitch - twist around horizontal axis  (look up/down)   
 * Roll - twist around axis in front of you  (tilt head left/right)
 * 
 * Coming soon, lots of great stuff...
 * 
 * @author kwatters
 *
 */
// TODO: implement publishOculusRiftData ... 
public class OculusRift extends Service implements OculusDataPublisher, PointPublisher {

	public static final String RIGHT_OPEN_CV = "rightOpenCV";
	public static final String LEFT_OPEN_CV = "leftOpenCV";
	private static final long serialVersionUID = 1L;
	private static final float RAD_TO_DEGREES = 57.2957795F;
	public final static Logger log = LoggerFactory.getLogger(OculusRift.class);

	// Rift stuff.
	protected Hmd hmd;

	private boolean initialized = false;
	private RiftFrame lastRiftFrame = new RiftFrame();

	private OpenCVFilterAffine leftAffine = new OpenCVFilterAffine("left");
	private OpenCVFilterAffine rightAffine = new OpenCVFilterAffine("right");

	private boolean calibrated = false;
	// Two OpenCV services, one for the left eye, one for the right eye.
	transient public OpenCV leftOpenCV;
	transient public OpenCV rightOpenCV;
	private OculusDisplay display;

	// TODO: make these configurable...
	private int leftCameraIndex = 0;
	private int rightCameraIndex = 1;


	private HmdDesc hmdDesc;

	transient public OculusHeadTracking headTracker = null;

	// TODO: remove this!  this is if you only have 1 video source.
	private	boolean mirrorLeftCamera = true;

	public static class RiftFrame{
		public SerializableImage left;	
		public SerializableImage right;	
	}


	public OculusRift(String reservedKey) {
		super(reservedKey);
	}

	@Override
	public void startService() {
		super.startService();
		initContext();
	}


	// Boradcast the state of the peers to notify the gui.
	public void broadcastState() {
		// notify the gui
		leftOpenCV.broadcastState();
		rightOpenCV.broadcastState();		
	}

	private void setupRift() {

		// Initalize the JNA library/ head mounted device.
		Hmd.initialize();
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}

		hmd = Hmd.create();


		if (null == hmd) {
			throw new IllegalStateException("Unable to initialize HMD");
		}
		hmdDesc = hmd.getDesc();

		hmd.configureTracking();


	}

	private void initContext() {

		if (!initialized) {
			log.info("Init the rift.");
			// Init the rift..
			setupRift();
			//OvrLibrary.INSTANCE.ovr_Initialize();
			//hmd = Hmd.create(0); 
			//int requiredSensorCaps = 0;
			//int supportedSensorCaps = OvrLibrary.ovrSensorCaps.ovrSensorCap_Orientation;
			// TODO: what errors/exceptions might be thrown here?  not sure how JNA exposes that info.
			//hmd.startSensor(supportedSensorCaps, requiredSensorCaps);
			log.info("Created HMD Oculus Rift Sensor");
			initialized = true;
			// now that we have the hmd. lets start up the polling thread.
			headTracker = new OculusHeadTracking(hmd, hmdDesc);
			headTracker.oculus = this;
			headTracker.start();
			// create and start the two open cv services..
			leftOpenCV = new OpenCV(getName() + "." + LEFT_OPEN_CV);
			rightOpenCV = new OpenCV(getName() + "." + RIGHT_OPEN_CV);
			leftOpenCV.startService();
			rightOpenCV.startService();
			leftOpenCV.setCameraIndex(leftCameraIndex);
			rightOpenCV.setCameraIndex(rightCameraIndex);
			// create msg routes from opencv services
			// a bit kludgy because OpenCV is old :P
			subscribe(leftOpenCV.getName(), "publishDisplay");
			subscribe(rightOpenCV.getName(), "publishDisplay");
			// Add some filters to rotate the images (cameras are mounted on their sides.)
			// TODO: use 1 filter per eye for the rotations.  (might not be exactly 90degree rotation)
			// TODO: replace with Affine filter.
			//OpenCVFilterResize leftResizeFilter = new OpenCVFilterResize("lrf");
			//OpenCVFilterResize rightResizeFilter = new OpenCVFilterResize("rrf");
			
			// TODO: pick these on the resoultion of the rift.
			int w = 512;
			int h = w; 
			//leftResizeFilter.setDestHeight(h);
			//leftResizeFilter.setDestWidth(w);
			
			//rightResizeFilter.setDestHeight(h);
			//rightResizeFilter.setDestWidth(w);

			
			//leftOpenCV.addFilter(leftResizeFilter);
			//rightOpenCV.addFilter(rightResizeFilter);
			
			boolean addTransposeEyes = true;
			if (addTransposeEyes) {
				OpenCVFilterTranspose t1 = new OpenCVFilterTranspose("t1"); 
				t1.flipCode = 1; 
				OpenCVFilterTranspose t2 = new OpenCVFilterTranspose("t2"); 
				t2.flipCode = 1; 
				//float leftAngle = 180;
				//float rightAngle = 0;
				//leftAffine.setAngle(leftAngle);
				//rightAffine.setAngle(rightAngle);
				//rotate 270
				leftOpenCV.addFilter(t1);
				// rotate 90
				rightOpenCV.addFilter(t2);
				leftAffine.setAngle(90);
			}
			
			// if we specify some per eye transforms
			// if we specify some per eye transforms
			//leftAffine.setDx(200);
			leftAffine.setDy(0);
			leftAffine.setAngle(90);
			//rightAffine.setDx(200);
			rightAffine.setDy(0);
			rightAffine.setAngle(90);
			
			// the affine is always on top i guess
			leftOpenCV.addFilter(leftAffine);
			rightOpenCV.addFilter(rightAffine);

			leftOpenCV.setDisplayFilter("left");
			rightOpenCV.setDisplayFilter("right");

			// lets set the grabbers
			// TODO: remove me!
			//rightOpenCV.setFrameGrabberType("org.myrobotlab.opencv.SlideShowFrameGrabber");
			//rightOpenCV.setInputSource(OpenCV.INPUT_SOURCE_IMAGE_DIRECTORY);
			
			//leftOpenCV.setFrameGrabberType("org.myrobotlab.opencv.SlideShowFrameGrabber");
			//leftOpenCV.setInputSource(OpenCV.INPUT_SOURCE_IMAGE_DIRECTORY);
			
			//opencvRight.setFrameGrabberType("org.myrobotlab.opencv.SlideShowFrameGrabber");
			// opencvRight.setInputSource(INPUT_SOURCE_IMAGE_DIRECTORY);
			
			// start the cameras.
			leftOpenCV.capture();
			rightOpenCV.capture();
			// Now turn on the camras.
			// set camera index

			// Now that the Rift and OpenCV has been setup.
			display = new OculusDisplay();
			// on publish frame we'll update the current frame in the rift..
			// synchronization issues maybe?
			display.run();

		} else {
			log.info("Rift interface already initialized.");
		}
	}

	public void onDisplay(SerializableImage frame){

		// if we're only one camera
		// the left frame is both frames.
		if (mirrorLeftCamera) {
			// if we're mirroring the left camera
			if ("left".equals(frame.getSource())) {
				lastRiftFrame.left = frame;
 				lastRiftFrame.right = frame;
			}
		} else if ("left".equals(frame.getSource())){
			lastRiftFrame.left = frame;
		} else if ("right".equals(frame.getSource())){
			lastRiftFrame.right = frame;
		} else {
			error("unknown source %s", frame.getSource());
		}

		if (!calibrated) {
			if (leftAffine.getLastClicked() != null && rightAffine.getLastClicked() != null) {
				// calibrate!
				double deltaY = (leftAffine.getLastClicked().getY() - rightAffine.getLastClicked().getY())/2.0;
				leftAffine.setDy(-deltaY);
				rightAffine.setDy(deltaY);
				System.out.println("Delta Y calibrated " + deltaY);
				calibrated=true;
			}
		}

		// update the oculus display with the last rift frame
		if (display != null) {
			display.setCurrentFrame(lastRiftFrame);
		} else {
			log.warn("The Oculus Display was null.");
		}
		invoke("publishRiftFrame", lastRiftFrame);
	}



	@Override
	public void stopService() {
		super.stopService();
		// TODO: validate proper life cycle.
		if (headTracker != null) {
			// TODO: ?
			headTracker.stop();
		}

		if (hmd != null){
			hmd.destroy();
			Hmd.shutdown();
		}
	}


	/**
	 * Resets orientation of the head tracking
	 * Makes the current orientation the straight ahead orientation.
	 * Use this to align your perspective.
	 */
	public void resetSensor() {
		//hmd.
		if (initialized) {
			// ?
			hmd.recenterPose();
			// hmd.resetSensor();
		} else {
			log.info("Sensor not initalized.");
		}
	}

	/**
	 * Log the head tracking info to help with debugging.
	 */
	public void logOrientation() {
		TrackingState trackingState = hmd.getTrackingState(0);
		OvrVector3f position = trackingState.HeadPose.Pose.Position;
		position.x *= 100.0f;
		position.y *= 100.0f;
		position.z *= 100.0f;
		System.out.println((int)position.x + ", " + (int)position.y + " " + (int)position.z);

		float w = trackingState.HeadPose.Pose.Orientation.w;
		float x = trackingState.HeadPose.Pose.Orientation.x;
		float y = trackingState.HeadPose.Pose.Orientation.y;
		float z = trackingState.HeadPose.Pose.Orientation.z;

		log.info("Roll: " + z*RAD_TO_DEGREES);
		log.info("Pitch:"+ x*RAD_TO_DEGREES);
		log.info("Yaw:"+ y*RAD_TO_DEGREES );
	}

	public float getYaw() {
		TrackingState trackingState = hmd.getTrackingState(0);
		float y = trackingState.HeadPose.Pose.Orientation.y * RAD_TO_DEGREES;;
		return y;
	}

	public float getRoll() {
		TrackingState trackingState = hmd.getTrackingState(0);
		float z = trackingState.HeadPose.Pose.Orientation.z * RAD_TO_DEGREES;
		return z;
	}

	public float getPitch() {
		TrackingState trackingState = hmd.getTrackingState(0);
		float x = trackingState.HeadPose.Pose.Orientation.x * RAD_TO_DEGREES;
		return x;
	}

	public void addRiftFrameListener(Service service){
		addListener("publishRiftFrame", service.getName(), "onRiftFrame");
	}

	public RiftFrame publishRiftFrame(RiftFrame frame){
		return frame;
	}


	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		OculusRift rift = (OculusRift) Runtime.createAndStart("oculus", "OculusRift");

		while (true) {
			float roll = rift.getRoll();
			rift.leftAffine.setAngle(-roll+180);
			rift.rightAffine.setAngle(-roll);
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		rift.logOrientation();
	}


	public int getLeftCameraIndex() {
		return leftCameraIndex;
	}

	public void setLeftCameraIndex(int leftCameraIndex) {
		this.leftCameraIndex = leftCameraIndex;
	}

	public int getRightCameraIndex() {
		return rightCameraIndex;
	}

	public void setRightCameraIndex(int rightCameraIndex) {
		this.rightCameraIndex = rightCameraIndex;
	}

	@Override
	public OculusData publishOculusData(OculusData data) {
		// grab the last published data (if we need it somewhere)
		//		if (data != null) {
		//			System.out.println("Oculus Data: "  + data.toString());
		//		}
		// TODO: make this a proper callback / subscribe..
		if (display != null) {
			display.updateOrientation(data);
		}
		// return the data to the mrl framework to be published.
		return data;
	}

	@Override
	public List<Point> publishPoints(List<Point> points) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(OculusRift.class.getCanonicalName());
		meta.addDescription("The Oculus Rift Head Tracking Service");
		meta.addCategory("video","control", "sensor");
		// make sure the open cv instance share each others streamer..
		//meta.sharePeer("leftOpenCV.streamer", "streamer", "VideoStreamer", "shared left streamer");
		//meta.sharePeer("rightOpenCV.streamer", "streamer", "VideoStreamer", "shared right streamer");
		
		meta.addPeer("leftOpenCV", "OpenCV", "Left Eye Camera");
		meta.sharePeer("rightOpenCV", "leftOpenCV", "OpenCV", "Right Eye sharing left eye camera");
		meta.addPeer("rightOpenCV", "OpenCV", "Right Eye Camera");
		meta.addDependency("org.saintandreas.jovr", "0.7");
		return meta;
	}


}

