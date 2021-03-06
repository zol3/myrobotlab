package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SerialDataListener;
import org.slf4j.Logger;

/**
 * 
 * VirtualDevice - This is a virtual serial port device that can be used to
 * redirect serial data over a network for example.
 * Blender service requires this so the serial commands to an inmoov and be
 * pumped over the network to blender, rather than over the serial port to 
 * an actual arduino.
 *
 */
public class VirtualDevice extends Service implements SerialDataListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(VirtualDevice.class);
	
	transient Serial uart;
	transient Python logic;	
	
	transient BlockingQueue<Message> msgs = new LinkedBlockingQueue<Message>();
	//transient BlockingQueue<Object> data = new LinkedBlockingQueue<Object>();


	public VirtualDevice(String n) {
		super(n);
		uart = (Serial)createPeer("uart");
		logic = (Python)createPeer("logic");
	}
	
	public void startService(){
		super.startService();
		uart = (Serial)startPeer("uart");
		logic = (Python)startPeer("logic");
		
		uart.addByteListener(this);
	}
	
	public Python getLogic(){
		return logic;
	}
	
	public Serial getUART(){
		return uart;
	}
	
	public String createVirtualPort(String portName) throws IOException{
		// first create and connect on the virtual UART side
		return uart.connectVirtualNullModem(portName);
	}
	

	public String createVirtualArduino(String portName) throws IOException {
		createVirtualPort(portName);
		String newCode = FileIO.resourceToString("VirtualDevice/Arduino.py");
		logic.loadScript("Arduino.py", newCode);
		logic.execAndWait();
		return portName;
	}
	
	/* WRONG - the "service" which handles this should
	 * delegate the event - relaying is a unecessary activity
	public String publishLoadedScript(String script){
		return script;
	}
	*/

	@Override
	public Integer onByte(Integer b) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String onConnect(String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String onDisconnect(String portName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * preProcessHook is used to intercept messages and process or route them
	 * before being processed/invoked in the Service.
	 * 
	 * @throws
	 * 
	 * @see org.myrobotlab.framework.Service#preProcessHook(org.myrobotlab.framework.Message)
	 */
	@Override
	public boolean preProcessHook(Message msg) {
		try {
			msgs.put(msg);
			//log.info(String.format("%d msg %s ", msgs.size(), msg));		
		} catch (Exception e) {
			Logging.logError(e);
		}
		return false;
	}
	

	public void clear() {
		//data.clear();
		msgs.clear();
	}
	
	public BlockingQueue<Message> getMsgs() {
		return msgs;
	}

	public Message getMsg(long timeout) throws InterruptedException {
		Message msg = msgs.poll(timeout, TimeUnit.MILLISECONDS);
		return msg;
	}
	

	public ArrayList<Message> waitForMsgs(int count) throws InterruptedException, IOException {
		return waitForMsgs(count, 1000, 100);
	}

	public ArrayList<Message> waitForMsgs(int count, int timeout) throws InterruptedException, IOException {
		return waitForMsgs(count, timeout, 100);
	}

	public ArrayList<Message> waitForMsgs(int count, int timeout, int pollInterval) throws InterruptedException, IOException {
		ArrayList<Message> ret = new ArrayList<Message>();
		long start = System.currentTimeMillis();
		long now = start;
		
		while (ret.size() < count) {
			now = System.currentTimeMillis();
			Message msg = msgs.poll(pollInterval, TimeUnit.MILLISECONDS);
			if (msg != null) {
				ret.add(msg);
			}
			if (now - start > timeout){
				String error = String.format("waited %d ms received %d messages expecting %d in less than %d ms", now - start, ret.size(), count, timeout);
				log.error(error);
				throw new IOException(error);
			}
		}

		log.info(String.format("returned %d msgs in %s ms", ret.size(), now - start));
		return ret;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
			
			String portName = "vport";
			Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
			//Serial serial = arduino.getSerial();

			VirtualDevice virtual = (VirtualDevice) Runtime.start("virtual", "VirtualDevice");
			virtual.createVirtualArduino(portName);
			
			//Runtime.start("webgui", "WebGui");
			/*
			boolean done = true;
			if (done){
				return;
			}
			*/
			//Python logic = virtual.getLogic();			
			
			/*
			Serial uart = virtual.getUART();
			uart.setCodec("arduino");
			Codec codec = uart.getRXCodec();
			codec.setTimeout(1000);
			uart.setTimeout(100); // don't want to hang when decoding results...
			*/
			
			arduino.setBoardMega();

			arduino.connect(portName);
		
			//Runtime.start("gui", "GUIService");
			
			Runtime.start("webgui", "WebGui");
			

		} catch (Exception e) {
			Logging.logError(e);
		}
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

		ServiceType meta = new ServiceType(VirtualDevice.class.getCanonicalName());
		meta.addDescription("A service which can create virtual devices, like the virtual Arduino");
		meta.addCategory("testing");
		// put peer definitions in
		meta.addPeer("uart", "Serial", "uart");
		meta.addPeer("logic", "Python", "logic to implement");

		return meta;
	}
		
}
