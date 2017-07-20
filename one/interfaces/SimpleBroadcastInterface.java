/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package interfaces;

import java.util.Collection;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.NetworkInterface;
import core.Settings;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SimpleBroadcastInterface extends NetworkInterface {
	/**
	 * Reads the interface settings from the Settings file
	 *  
	 */
	public SimpleBroadcastInterface(Settings s)	{
		super(s);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SimpleBroadcastInterface(SimpleBroadcastInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new SimpleBroadcastInterface(this);
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()  
				&& anotherInterface.getHost().isActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
			// new contact within range
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Updates the state of current connections (ie tears down connections
	 * that are out of range).
	 */
	//int i=1;
	public void update() {
		// First break the old ones
		optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
				i++;
/***********jinchenghao chaged***********************/
				//System.out.println(con.getSpeed());
				double newspeed = get_nowspeed(this, anotherInterface, 
						this.transmitRange);
				con.update_speed(newspeed);		
			}
		}
		// Then find new possible connections
		Collection<NetworkInterface> interfaces =
			optimizer.getNearInterfaces(this);
		for (NetworkInterface i : interfaces) {
			connect(i);
		}
		//System.out.println(i++);
	}

	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {    			
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SimpleBroadcastInterface " + super.toString();
	}
	
	/**
	 * 计算当前两个节点的距离
	 * 根据距离求出传输速度，目前按照 now_speed = stting_speed - (setting_speed/setting_Range) * now_distance
	 */
	double get_nowspeed(NetworkInterface from_interface,NetworkInterface to_interface,double transmitSpeed){
		
		int conSpeed = to_interface.getTransmitSpeed();
		if (conSpeed > this.transmitSpeed) {
			conSpeed = this.transmitSpeed; 
		}
		//速度随时间变化模型
		double distance = get_nowdistance(from_interface.getHost(),to_interface.getHost());
		if(distance <= this.transmitRange/4) conSpeed = conSpeed;
		else if(distance > this.transmitRange/4 && distance <= 3*this.transmitRange/4) conSpeed = conSpeed/2;
		else conSpeed = conSpeed/4;
		
		double speed = conSpeed - conSpeed *distance/transmitRange;
		return speed;
	}

	private double get_nowdistance(DTNHost h1, DTNHost h2) {
		// TODO Auto-generated method stub
		double dx = h1.getLocation().getX() - h2.getLocation().getX();
		double dy = h1.getLocation().getY() - h2.getLocation().getY();
		double distance = Math.sqrt(dx*dx + dy*dy);
		return distance;
	}

}
