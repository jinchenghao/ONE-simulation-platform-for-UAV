/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.awt.Dialog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import movement.MovementModel;
import movement.Path;
import routing.MessageRouter;
import routing.RoutingInfo;

/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
	private static int nextAddress = 0;
	private int address;

	private Coord location; 	// where is the host
	private Coord destination;	// where is it going

	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private List<NetworkInterface> net;
	private ModuleCommunicationBus comBus;
	
	/**************金程皓*****************/
	//自定义属性
	private int group_master ;
	private Coord routelocation;
	private double groupNodeCorn;
	private double groupNodeDistance;
	private int groupSpeedLevel;

	static {
		DTNSim.registerForReset(DTNHost.class.getCanonicalName());
		reset();
	}
	/**
	 * Creates a new DTNHost.
	 * @param msgLs Message listeners
	 * @param movLs Movement listeners
	 * @param groupId GroupID of this host
	 * @param interf List of NetworkInterfaces for the class
	 * @param comBus Module communication bus object
	 * @param mmProto Prototype of the movement model of this host
	 * @param mRouterProto Prototype of the message router of this host
	 */
	public DTNHost(List<MessageListener> msgLs,
			List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf,
			ModuleCommunicationBus comBus, 
			MovementModel mmProto, MessageRouter mRouterProto,
			int group_master ,double groupNodeCorn ,double group_Node_Distance,
			int group_address , int group_Speed_Level) {
		this.comBus = comBus;
		this.location = new Coord(0,0);
		this.address = group_address;
		this.name = groupId + Integer.toBinaryString((group_address & 0xff00) >> 8) +"."+ 
		Integer.toBinaryString(group_address & 0x00ff);
		this.net = new ArrayList<NetworkInterface>();
		
		/****************金程皓*********************/
		//自定义属性group_master为每个组中子节点的标记信息
		this.group_master = group_master;
		//子无人机的角度
		this.groupNodeCorn = groupNodeCorn;
		//保存路径上的上一节点，方便计算斜率（只对子节点有用）
		this.routelocation = new Coord(0, 0);
		//子节点距离master的距离
		this.groupNodeDistance = group_Node_Distance;
		//速度级别
		this.groupSpeedLevel = group_Speed_Level;

		for (NetworkInterface i : interf) {
			NetworkInterface ni = i.replicate();
			ni.setHost(this);
			net.add(ni);
		}	

		// TODO - think about the names of the interfaces and the nodes
		//this.name = groupId + ((NetworkInterface)net.get(1)).getAddress();

		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototypes
		this.movement = mmProto.replicate();
		this.movement.setComBus(comBus);
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();
		this.routelocation = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}
	}
	
	/**
	 * Returns a new network interface address and increments the address for
	 * subsequent calls.
	 * @return The next address.
	 */
	private synchronized static int getNextAddress() {
		return nextAddress++;	
	}

	/**
	 * Reset the host and its interfaces
	 */
	public static void reset() {
		nextAddress = 0;
	}

	/**
	 * Returns true if this node is active (false if not)
	 * @return true if this node is active (false if not)
	 */
	public boolean isActive() {
		return this.movement.isActive();
	}

	/**
	 * Set a router for this host
	 * @param router The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.address;
	}
	
	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}
	
    /**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * @param con  The connection object whose state changed
	 */
	public void connectionUp(Connection con) {
		this.router.changedConnection(con);
	}

	public void connectionDown(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a copy of the list of connections this host has with other hosts
	 * @return a copy of the list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		List<Connection> lc = new ArrayList<Connection>();

		for (NetworkInterface i : net) {
			lc.addAll(i.getConnections());
		}

		return lc;
	}

	/**
	 * Returns the current location of this host. 
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no
	 * path is in use at the moment.
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
	}


	/**
	 * Sets the Node's location overriding any location set by movement model
	 * @param location The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty
	 * buffer but can be over 100 if a created message is bigger than buffer 
	 * space that could be freed.
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		double bSize = router.getBufferSize();
		double freeBuffer = router.getFreeBufferSize();
		return 100*((bSize-freeBuffer)/bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Returns the interface objects of the node
	 */
	public List<NetworkInterface> getInterfaces() {
		return net;
	}

	/**
	 * Find the network interface based on the index
	 */
	protected NetworkInterface getInterface(int interfaceNo) {
		NetworkInterface ni = null;
		try {
			ni = net.get(interfaceNo-1);
		} catch (IndexOutOfBoundsException ex) {
			System.out.println("No such interface: "+interfaceNo);
			System.exit(0);
		}
		return ni;
	}

	/**
	 * Find the network interface based on the interfacetype
	 */
	protected NetworkInterface getInterface(String interfacetype) {
		for (NetworkInterface ni : net) {
			if (ni.getInterfaceType().equals(interfacetype)) {
				return ni;
			}
		}
		return null;	
	}

	/**
	 * Force a connection event
	 */
	public void forceConnection(DTNHost anotherHost, String interfaceId, 
			boolean up) {
		NetworkInterface ni;
		NetworkInterface no;

		if (interfaceId != null) {
			ni = getInterface(interfaceId);
			no = anotherHost.getInterface(interfaceId);

			assert (ni != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
			assert (no != null) : "Tried to use a nonexisting interfacetype "+interfaceId;
		} else {
			ni = getInterface(1);
			no = anotherHost.getInterface(1);
			
			assert (ni.getInterfaceType().equals(no.getInterfaceType())) : 
				"Interface types do not match.  Please specify interface type explicitly";
		}
		
		if (up) {
			ni.createConnection(no);
		} else {
			ni.destroyConnection(no);
		}
	}

	/**
	 * for tests only --- do not use!!!
	 */
	public void connect(DTNHost h) {
		System.err.println(
				"WARNING: using deprecated DTNHost.connect(DTNHost)" +
		"\n Use DTNHost.forceConnection(DTNHost,null,true) instead");
		forceConnection(h,null,true);
	}

	/**
	 * Updates node's network layer and router.
	 * @param simulateConnections Should network layer be updated too
	 */
	public void update(boolean simulateConnections) {
		if (!isActive()) {
			return;
		}
		
		if (simulateConnections) {
			for (NetworkInterface i : net) {
				i.update();
			}
		}
		this.router.update();
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is
	 * not time to move yet
	 * @param timeIncrement How long time the node moves
	 */
	public void move(double timeIncrement) {	
		//判断是否是主节点，如果是主节点则不需计算，按轨迹飞行即可
		if(this.group_master == 0){ 
			masterMove(timeIncrement);
		}
		
		//如果不是主节点，则需根据主节点提供的坐标计算自己的位置
		else{ 
			childMove(timeIncrement);
		}
	}	

	/**
	 * Sets the next destination and speed to correspond the next waypoint
	 * on the path.
	 * @return True if there was a next waypoint to set, false if node still
	 * should wait
	 */
	private boolean setNextWaypoint() {
		//System.out.println(this.address+"get in!!!!!!!!!!!!!!!!!!");
		//if(this.master == 0) System.out.println(this.address+"我是普通节点");
		if (path == null) {
			path = movement.getPath();
		}
		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}
		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Sends a message from this host to another host
	 * @param id Identifier of the message
	 * @param to Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		this.router.sendMessage(id, to);
	}

	/**
	 * Start receiving a message from another host
	 * @param m The message
	 * @param from Who the message is from
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int receiveMessage(Message m, DTNHost from) {
		int retVal = this.router.receiveMessage(m, from); 

		if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this);	// add this node on the messages path
		}

		return retVal;	
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * @param con The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 */
	public void messageTransferred(String id, DTNHost from) {
		this.router.messageTransferred(id, from);
	}

	/**
	 * Informs the host that a message transfer was aborted.
	 * @param id Identifier of the message
	 * @param from From who the message was from
	 * @param bytesRemaining Nrof bytes that were left before the transfer
	 * would have been ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * @param m The message to create
	 */
	public void createNewMessage(Message m) {
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * @param id Identifier of the message
	 * @param drop True if the message is deleted because of "dropping"
	 * (e.g. buffer is full) or false if it was deleted for some other reason
	 * (e.g. the message got delivered to final destination). This effects the
	 * way the removing is reported to the message listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * @param otherHost The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}
	
	private void masterMove(double timeIncrement){
		double possibleMovement;
		double distance;
		double dx, dy;
		if (!isActive() || SimClock.getTime() < this.nextTimeToMove) {
			return; 
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				return; // no more waypoints left
			}
			distance = this.location.distance(this.destination);
			this.speed = distance/700;
		}
		
		// move towards the point for possibleMovement amount
		dx = (possibleMovement/distance) * (this.destination.getX() -
				this.location.getX());
		dy = (possibleMovement/distance) * (this.destination.getY() -
				this.location.getY());
		this.location.translate(dx, dy);
	}
	
	private void childMove(double timeIncrement){
		double possibleMovement;
		double distance;
		double dx, dy;
		Coord ordinarydestination = new Coord(0, 0);
		double[] ans,ansv,ansc = null;
		
		if (!isActive() || SimClock.getTime() < this.nextTimeToMove) {
			return; 
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		if(this.destination.equals(this.routelocation)) { //目标节点和上次相同
			ordinarydestination.setLocation(this.location);
			distance = this.location.distance(ordinarydestination);
		}
		
		else{
		ordinarydestination = getcoord_new_coord(); //获得子节点下一位置坐标	
		distance = this.location.distance(ordinarydestination);
		}

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(ordinarydestination); // snap to destination
			this.routelocation.setLocation(this.destination.getX(),this.destination.getY());
			possibleMovement -= distance;
			
			if (!setNextWaypoint()) { // get a new waypoint
				
				return; // no more waypoints left
			}
			if(this.destination.equals(routelocation)) {
				ordinarydestination = ordinarydestination;
			}
			else{
				ordinarydestination = getcoord_new_coord();
			}
			distance = this.location.distance(ordinarydestination);
			this.speed = distance/700; //计算速度
		}
		// move towards the point for possibleMovement amount
		dx = (possibleMovement/distance) * (ordinarydestination.getX() -
				this.location.getX());
		dy = (possibleMovement/distance) * (ordinarydestination.getY() -
				this.location.getY());
		this.location.translate(dx, dy);
	}

	
	/**
	 * 子节点计算坐标的函数
	 * @return 子节点的坐标
	 */
	private Coord getcoord_node (){
		Coord ordinarydestination;
		double[] ans,ansv,ansc;
		ans = function(this.routelocation.getX(), this.routelocation.getY(),
					this.destination.getX(), this.destination.getY());
		ansv = vertical(ans, this.destination.getX(), this.destination.getY());
		ansc = coordnat(ansv, this.destination.getX(), 
					this.destination.getY(),this.group_master*100);
		ordinarydestination = new Coord(ansc[0],ansc[1]);
		return ordinarydestination;
		
	}
	
	/**
	 * 求经过两点直线的k,b
	 * @param x1 y1 第一个点
	 * @param x2 y2 第二个点
	 * @return 直线的k,b
	 */
	public static double[] function(double x1,double y1,double x2,double y2){
		double k = (y1 - y2) / (x1 - x2);
		double b;
		if((x1 - x2) >= 0) b = -1;
		else b = 1;
		double[] ans = {k,b};
		return ans ;
	}
	
	/**
	 * 求与某直线垂直的直线的k，b
	 * @param ans 已知直线的k，b
	 * @param x1 y1 经过的点
	 * @return 新直线的k和b
	 */
	public static double[] vertical (double[] ans,double x1,double y1){
		double k = -1/ans[0];
		double b = y1 - x1*k;
		double[] ansv = {k,b};
		return ansv;
	}
	
	/**
	 * 计算其他节点的坐标
	 * @param ansv 直线的k,b
	 * @param x1 y1 圆心坐标
	 * @return 得到点的坐标
	 */
	public static double[] coordnat (double[] ansv,double x1,double y1,int r){
		double horn;
		double[] ansc = new double [2];
		if(ansv[0] < 0){
			horn = Math.atan(Math.abs(ansv[0]));
			ansc[0] = x1 - Math.abs(Math.cos(horn))*r;
			ansc[1] = y1 + Math.abs(Math.sin(horn))*r;
		}
		if(ansv[0] >= 0){
			horn = Math.atan(Math.abs(ansv[0]));
			ansc[0] = x1 + Math.abs(Math.cos(horn))*r;
			ansc[1] = y1 + Math.abs(Math.sin(horn))*r;
		}
		
		return ansc;
	}
	
	/**
	 * 版本2
	 * @param ans
	 * @param corn
	 * @return
	 */
	public static double[] new_coord(double[] ans,double x1,double y1,Double corn,double r){
		double horn = Math.atan(ans[0]) - corn;
		double[] ansc = new double [2];
			ansc[0] = x1 - ans[1]*r*Math.cos(horn);
			ansc[1] = y1 - ans[1]*r*Math.sin(horn);
		return ansc;
	}
	
	private Coord getcoord_new_coord(){
		Coord ordinarydestination;
		double[] ans,ansc;
		ans = function(this.routelocation.getX(), this.routelocation.getY(),
					this.destination.getX(), this.destination.getY());
		ansc = new_coord(ans, this.destination.getX(), this.destination.getY(),
				this.groupNodeCorn,this.groupNodeDistance);
		ordinarydestination = new Coord(ansc[0],ansc[1]);
		return ordinarydestination;
	}

}
