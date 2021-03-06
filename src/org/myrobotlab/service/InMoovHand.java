package org.myrobotlab.service;

import java.io.IOException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.LeapMotion.Hand;
import org.myrobotlab.service.LeapMotion.LeapData;
import org.myrobotlab.service.interfaces.LeapDataListener;
import org.slf4j.Logger;

/**
 * InMoovHand - The Hand sub service for the InMoov Robot.
 * This service has 6 servos controlled by an arduino.
 * thumb,index,majeure,ringFinger,pinky, and wrist
 * 
 * There is also leap motion support.
 */
public class InMoovHand extends Service implements LeapDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovHand.class);

	/**
	 * peer services
	 */
	transient public LeapMotion leap;
	transient public Servo thumb;
	transient public Servo index;
	transient public Servo majeure;
	transient public Servo ringFinger;
	transient public Servo pinky;
	transient public Servo wrist;
	transient public Arduino arduino;
	private String side;

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			
			InMoov i01 = (InMoov)Runtime.start("i01", "InMoov");
			InMoovHand hand =i01.startRightHand("COM15");
			Arduino arduino = (Arduino)Runtime.getService("i01.right");
			arduino.pinMode(13, Arduino.OUTPUT);
			arduino.digitalWrite(13, 1);
			
			InMoovHand rightHand = new InMoovHand("r01");
			Runtime.createAndStart("gui", "GUIService");
			rightHand.connect("COM15");
			rightHand.startService();
			Runtime.createAndStart("webgui", "WebGui");
			// rightHand.connect("COM12"); TEST RECOVERY !!!
			rightHand.close();
			rightHand.open();
			rightHand.openPinch();
			rightHand.closePinch();
			rightHand.rest();
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 */

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	// FIXME make
	// .isValidToStart() !!! < check all user data !!!

	public InMoovHand(String n) {
		super(n);
		thumb = (Servo) createPeer("thumb");
		index = (Servo) createPeer("index");
		majeure = (Servo) createPeer("majeure");
		ringFinger = (Servo) createPeer("ringFinger");
		pinky = (Servo) createPeer("pinky");
		wrist = (Servo) createPeer("wrist");
		arduino = (Arduino) createPeer("arduino");

		thumb.setRest(2);
		index.setRest(2);
		majeure.setRest(2);
		ringFinger.setRest(2);
		pinky.setRest(2);
		wrist.setRest(90);

		// connection details
		thumb.setPin(2);
		index.setPin(3);
		majeure.setPin(4);
		ringFinger.setPin(5);
		pinky.setPin(6);
		wrist.setPin(7);

		thumb.setController(arduino);
		index.setController(arduino);
		majeure.setController(arduino);
		ringFinger.setController(arduino);
		pinky.setController(arduino);
		wrist.setController(arduino);
	}

	/**
	 * attach all the servos - this must be re-entrant and accomplish the
	 * re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attach() {
		sleep(InMoov.attachPauseMs);
		thumb.attach();
		sleep(InMoov.attachPauseMs);
		index.attach();
		sleep(InMoov.attachPauseMs);
		majeure.attach();
		sleep(InMoov.attachPauseMs);
		ringFinger.attach();
		sleep(InMoov.attachPauseMs);
		pinky.attach();
		sleep(InMoov.attachPauseMs);
		wrist.attach();
		return true;
	}

	public void bird() {
		moveTo(150, 180, 0, 180, 180, 90);
	}

	@Override
	public void broadcastState() {
		// notify the gui
		thumb.broadcastState();
		index.broadcastState();
		majeure.broadcastState();
		ringFinger.broadcastState();
		pinky.broadcastState();
		wrist.broadcastState();
	}

	public void close() {
		moveTo(130, 180, 180, 180, 180);
	}

	public void closePinch() {
		moveTo(130, 140, 180, 180, 180);
	}

	// FIXME FIXME - this method must be called
	// user data needed
	/**
	 * connect - user data needed
	 * 
	 * @param port
	 * @return
	 * @throws IOException
	 */
	public boolean connect(String port) throws IOException {

		if (arduino == null) {
			error("arduino is invalid");
			return false;
		}

		arduino.connect(port);

		if (!arduino.isConnected()) {
			error("arduino %s not connected", arduino.getName());
			return false;
		}

		attach();
		setSpeed(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
		rest();
		sleep(2000);
		setSpeed(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
		broadcastState();
		return true;
	}

	public void count() {
		one();
		sleep(1);
		two();
		sleep(1);
		three();
		sleep(1);
		four();
		sleep(1);
		five();
	}

	public void detach() {
		thumb.detach();
		sleep(InMoov.attachPauseMs);
		index.detach();
		sleep(InMoov.attachPauseMs);
		majeure.detach();
		sleep(InMoov.attachPauseMs);
		ringFinger.detach();
		sleep(InMoov.attachPauseMs);
		pinky.detach();
		sleep(InMoov.attachPauseMs);
		wrist.detach();
	}

	public void devilHorns() {
		moveTo(150, 0, 180, 180, 0, 90);
	}

	public void five() {
		open();
	}

	public void four() {
		moveTo(150, 0, 0, 0, 0, 90);
	}


	public long getLastActivityTime() {

		long lastActivityTime = Math.max(index.getLastActivityTime(), thumb.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, index.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, majeure.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, ringFinger.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, pinky.getLastActivityTime());
		lastActivityTime = Math.max(lastActivityTime, wrist.getLastActivityTime());

		return lastActivityTime;

	}

	public String getScript(String inMoovServiceName) {
		return String.format("%s.moveHand(\"%s\",%d,%d,%d,%d,%d,%d)\n", inMoovServiceName, side, thumb.getPos(), index.getPos(), majeure.getPos(), ringFinger.getPos(),
				pinky.getPos(), wrist.getPos());
	}

	public String getSide() {
		return side;
	}

	public void hangTen() {
		moveTo(0, 180, 180, 180, 0, 90);
	}

	public boolean isAttached() {
		boolean attached = false;
		attached |= thumb.isAttached();
		attached |= index.isAttached();
		attached |= majeure.isAttached();
		attached |= ringFinger.isAttached();
		attached |= pinky.isAttached();
		attached |= wrist.isAttached();
		return attached;
	}

	public void map(int minX, int maxX, int minY, int maxY) {
		thumb.map(minX, maxX, minY, maxY);
		index.map(minX, maxX, minY, maxY);
		majeure.map(minX, maxX, minY, maxY);
		ringFinger.map(minX, maxX, minY, maxY);
		pinky.map(minX, maxX, minY, maxY);
	}

	// TODO - waving thread fun
	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky) {
		moveTo(thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s.moveTo %d %d %d %d %d %d", getName(), thumb, index, majeure, ringFinger, pinky, wrist));
		}
		this.thumb.moveTo(thumb);
		this.index.moveTo(index);
		this.majeure.moveTo(majeure);
		this.ringFinger.moveTo(ringFinger);
		this.pinky.moveTo(pinky);
		if (wrist != null)
			this.wrist.moveTo(wrist);
	}

	public void ok() {
		moveTo(150, 180, 0, 0, 0, 90);
	}

	public void one() {
		moveTo(150, 0, 180, 180, 180, 90);
	}

	@Override
	public LeapData onLeapData(LeapData data) {

		if (!data.frame.isValid()) {
			// TODO: we could return void here? not sure
			// who wants the return value form this method.
			log.info("Leap data frame not valid.");
			return data;
		}
		Hand h;
		if ("right".equalsIgnoreCase(side)) {
			if (data.frame.hands().rightmost().isValid()) {
				h = data.rightHand;
			} else {
				log.info("Right hand frame not valid.");
				// return this hand isn't valid
				return data;
			}
		} else if ("left".equalsIgnoreCase(side)) {
			if (data.frame.hands().leftmost().isValid()) {
				h = data.leftHand;
			} else {
				log.info("Left hand frame not valid.");
				// return this frame isn't valid.
				return data;
			}
		} else {
			// side could be null?
			log.info("Unknown Side or side not set on hand (Side = {})", side);
			// we can default to the right side?
			// TODO: come up with a better default or at least document this
			// behavior.
			if (data.frame.hands().rightmost().isValid()) {
				h = data.rightHand;
			} else {
				log.info("Right(unknown) hand frame not valid.");
				// return this hand isn't valid
				return data;
			}
		}

		// If the hand data came from a valid frame, update the finger postions.
		// move all fingers
		if (index != null && index.isAttached()) {
			index.moveTo(h.index);
		} else {
			log.debug("Index finger isn't attached or is null.");
		}
		if (thumb != null && thumb.isAttached()) {
			thumb.moveTo(h.thumb);
		} else {
			log.debug("Thumb isn't attached or is null.");
		}
		if (pinky != null && pinky.isAttached()) {
			pinky.moveTo(h.pinky);
		} else {
			log.debug("Pinky finger isn't attached or is null.");
		}
		if (ringFinger != null && ringFinger.isAttached()) {
			ringFinger.moveTo(h.ring);
		} else {
			log.debug("Ring finger isn't attached or is null.");
		}
		if (majeure != null && majeure.isAttached()) {
			majeure.moveTo(h.middle);
		} else {
			log.debug("Middle(Majeure) finger isn't attached or is null.");
		}

		return data;
	}

	public void open() {
		rest();
	}

	public void openPinch() {
		moveTo(0, 0, 180, 180, 180);
	}

	// ----- initialization end --------
	// ----- movements begin -----------

	public void release() {
		detach();
		thumb.releaseService();
		index.releaseService();
		majeure.releaseService();
		ringFinger.releaseService();
		pinky.releaseService();
		wrist.releaseService();
	}

	public void rest() {
		// initial positions
		setSpeed(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

		thumb.rest();
		index.rest();
		majeure.rest();
		ringFinger.rest();
		pinky.rest();
		wrist.rest();
	}

	@Override
	public boolean save() {
		super.save();
		thumb.save();
		index.save();
		majeure.save();
		ringFinger.save();
		pinky.save();
		wrist.save();
		return true;
	}

	public void setPins(int thumb, int index, int majeure, int ringFinger, int pinky, int wrist) {
		log.info(String.format("setPins %d %d %d %d %d %d", thumb, index, majeure, ringFinger, pinky, wrist));
		this.thumb.setPin(thumb);
		this.index.setPin(index);
		this.majeure.setPin(majeure);
		this.ringFinger.setPin(ringFinger);
		this.pinky.setPin(pinky);
		this.wrist.setPin(wrist);
	}

	public void setRest(int thumb, int index, int majeure, int ringFinger, int pinky) {
		setRest(thumb, index, majeure, ringFinger, pinky, null);
	}

	public void setRest(int thumb, int index, int majeure, int ringFinger, int pinky, Integer wrist) {
		log.info(String.format("setRest %d %d %d %d %d %d", thumb, index, majeure, ringFinger, pinky, wrist));
		this.thumb.setRest(thumb);
		this.index.setRest(index);
		this.majeure.setRest(majeure);
		this.ringFinger.setRest(ringFinger);
		this.pinky.setRest(pinky);
		if (wrist != null) {
			this.wrist.setRest(wrist);
		}
	}

	public void setSide(String side) {
		this.side = side;
	}

	public void setSpeed(Double thumb, Double index, Double majeure, Double ringFinger, Double pinky, Double wrist) {
		this.thumb.setSpeed(thumb);
		this.index.setSpeed(index);
		this.majeure.setSpeed(majeure);
		this.ringFinger.setSpeed(ringFinger);
		this.pinky.setSpeed(pinky);
		this.wrist.setSpeed(wrist);
	}

	public void startLeapTracking() throws Exception {
		if (leap == null) {
			leap = (LeapMotion) startPeer("leap");
		}
		this.index.map(90, 0, this.index.getMin(), this.index.getMax());
		this.thumb.map(90, 50, this.thumb.getMin(), this.thumb.getMax());
		this.majeure.map(90, 0, this.majeure.getMin(), this.majeure.getMax());
		this.ringFinger.map(90, 0, this.ringFinger.getMin(), this.ringFinger.getMax());
		this.pinky.map(90, 0, this.pinky.getMin(), this.pinky.getMax());
		leap.addLeapDataListener(this);
		leap.startTracking();
		return;
	}

	@Override
	public void startService() {
		super.startService();
		thumb.startService();
		index.startService();
		majeure.startService();
		ringFinger.startService();
		pinky.startService();
		wrist.startService();
		arduino.startService();
	}

	public void stopLeapTracking() {
		leap.stopTracking();
		this.index.map(this.index.getMin(), this.index.getMax(), this.index.getMin(), this.index.getMax());
		this.thumb.map(this.thumb.getMin(), this.thumb.getMax(), this.thumb.getMin(), this.thumb.getMax());
		this.majeure.map(this.majeure.getMin(), this.majeure.getMax(), this.majeure.getMin(), this.majeure.getMax());
		this.ringFinger.map(this.ringFinger.getMin(), this.ringFinger.getMax(), this.ringFinger.getMin(), this.ringFinger.getMax());
		this.pinky.map(this.pinky.getMin(), this.pinky.getMax(), this.pinky.getMin(), this.pinky.getMax());
		this.rest();
		return;
	}

	public void test() {

		if (arduino == null) {
			error("arduino is null");
		}

		if (!arduino.isConnected()) {
			error("arduino not connected");
		}

		thumb.moveTo(thumb.getPos() + 2);
		index.moveTo(index.getPos() + 2);
		majeure.moveTo(majeure.getPos() + 2);
		ringFinger.moveTo(ringFinger.getPos() + 2);
		pinky.moveTo(pinky.getPos() + 2);
		wrist.moveTo(wrist.getPos() + 2);
			

		info("test completed");
	}

	public void three() {
		moveTo(150, 0, 0, 0, 180, 90);
	}

	public void thumbsUp() {
		moveTo(0, 180, 180, 180, 180, 90);
	}

	public void two() {
		victory();
	}

	public void victory() {
		moveTo(150, 0, 0, 180, 180, 90);
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

		ServiceType meta = new ServiceType(InMoovHand.class.getCanonicalName());
		meta.addDescription("an easier way to create gestures for InMoov");
		meta.addCategory("robot");
		
		meta.addPeer("thumb", "Servo", "Thumb servo");
		meta.addPeer("index", "Servo", "Index servo");
		meta.addPeer("majeure", "Servo", "Majeure servo");
		meta.addPeer("ringFinger", "Servo", "RingFinger servo");
		meta.addPeer("pinky", "Servo", "Pinky servo");
		meta.addPeer("wrist", "Servo", "Wrist servo");
		meta.addPeer("arduino", "Arduino", "Arduino controller for this arm");
		meta.addPeer("leap", "LeapMotion", "Leap Motion Service");

		
		return meta;
	}

}
