package org.myrobotlab.service.interfaces;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;

import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Status;

public interface ServiceInterface extends Messaging, LoggingSink, NameProvider {


	/**
	 * this is a local method which adds a request from some
	 * foreign service with address information (otherService/callback) for a topic callback
	 * Adds an entry on the notify list
	 * 
	 * @param localTopic
	 * @param otherService
	 * @param callback
	 */
	public void addListener(String localTopic, String otherService, String callback);

	public void removeListener(String localTopic, String otherService, String callback);

	public String[] getDeclaredMethodNames();

	public Method[] getDeclaredMethods();

	public URI getInstanceId();

	public String[] getMethodNames();

	public Method[] getMethods();

	public ArrayList<MRLListener> getNotifyList(String key);

	public ArrayList<String> getNotifyListKeySet();
	
	public Outbox getOutbox();

	// Deprecate - just use class
	public String getSimpleName();

	// Deprecate ?? What is this??
	public String getType();

	public boolean hasDisplay();

	public boolean hasPeers();

	public boolean load();

	/**
	 * recursive release - releases all peers and their peers etc. then releases
	 * this service
	 */
	public void releasePeers();

	public void releaseService();


	/**
	 * asked by the framework - to determine if the service needs to be secure
	 * 
	 * @return
	 */
	public boolean requiresSecurity();

	public boolean save();

	public void setInstanceId(URI uri);

	public void setName(String prefix);

	public void startService();

	public void stopService();

	public String clearLastError();

	public boolean hasError();

	public Status getLastError();

	public void broadcastState();
	
	public Object invoke(Message msg);

	public void out(String method, Object retobj);
	
	public boolean isRuntime();
	
	// FIXME - meta data needs to be infused into instance
	public String getDescription();
}
