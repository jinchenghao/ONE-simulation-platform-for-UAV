package core;

import java.util.List;

public class UAVNet {
	private static int nextAddress = 0;
	private int group_address;

	private int[] group_master ;
	private double[] groupNodeCorn;
	private double[] group_NodeDistance;
	private List<DTNHost> hosts;
	private String group_ID;
	private int group_Speed_Level;
	
	public UAVNet(String groupID,int groupSpeedLevel){
		//子网号
		this.group_address = getNextAddress();
		//子网名
		this.group_ID = groupID;
		//速度等级
		this.group_Speed_Level = groupSpeedLevel;
	}

	private synchronized static int getNextAddress() {
		return nextAddress++;	
	}

	public int[] getGroup_master() {
		return group_master;
	}

	public void setGroup_master(String groupMaster) {
		String[] group_MAS = groupMaster.split(",");
		this.group_master = new int [group_MAS.length];
		for (int i=0;i<group_MAS.length;i++)
			this.group_master[i] = Integer.valueOf(group_MAS[i]);
	}

	public double[] getGroupNodeCorn() {
		return groupNodeCorn;
	}

	public void setGroupNodeCorn(String groupNodeCorn) {
		String[] group_Corn = groupNodeCorn.split(",");
		this.groupNodeCorn = new double[group_Corn.length];
		for (int i=0;i<group_Corn.length;i++)
		this.groupNodeCorn[i] = Double.valueOf(group_Corn[i])*Math.PI/180;
	}

	public double[] getGroup_NodeDistance() {
		return group_NodeDistance;
	}

	public void setGroup_NodeDistance(String group_NodeDistance) {
		String[] group_Dis = group_NodeDistance.split(",");
		this.group_NodeDistance = new double[group_Dis.length];
		for (int i=0;i<group_Dis.length;i++)
		this.group_NodeDistance[i] = Double.valueOf(group_Dis[i]);
	}

	public List<DTNHost> getHosts() {
		return hosts;
	}

	public void setHosts(List<DTNHost> hosts) {
		this.hosts = hosts;
	}

	public int getGroup_address() {
		return group_address;
	}

	public String getGroup_ID() {
		return group_ID;
	}

	public int getGroup_Speed_Level() {
		return group_Speed_Level;
	}

	public void setGroup_Speed_Level(int group_Speed_Level) {
		this.group_Speed_Level = group_Speed_Level;
	}
	
	
	
}
