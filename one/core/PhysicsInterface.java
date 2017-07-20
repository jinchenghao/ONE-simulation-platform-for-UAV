package core;

import report.Report;
import core.ConnectionListener;
import core.DTNHost;

public class PhysicsInterface extends Report implements ConnectionListener {

	public void hostsConnected(DTNHost h1, DTNHost h2) {
		if (isWarmup()) {
			addWarmupID(connectionString(h1, h2));
			return;
		}
		newEvent();
		System.out.println(createTimeStamp() + " CONN " + connectionString(h1, h2) + " up");
	}

	public void hostsDisconnected(DTNHost h1, DTNHost h2) {
		String conString = connectionString(h1, h2);

		if (isWarmup() || isWarmupID(conString)) {
			removeWarmupID(conString);
			return;
		}

		System.out.println(createTimeStamp() + " CONN " + conString + " down");
	}
	
	private String createTimeStamp() {
		return String.format("%.2f", getSimTime());
	}
	
	private String connectionString(DTNHost h1, DTNHost h2) {
		double dx = h1.getLocation().getX() - h2.getLocation().getX();
		double dy = h1.getLocation().getY() - h2.getLocation().getY();
		String disString = Double.toString(Math.sqrt(dx*dx + dy*dy));
		
//		return Double.toString(Math.sqrt(dx*dx + dy*dy));
		
		if (h1.getAddress() < h2.getAddress()) {
		    return h1.getAddress() + " " + h2.getAddress() + "->" + disString;
		}
		else {
		    return h2.getAddress() + " " + h1.getAddress() + "->" + disString;
		}
	}

}
