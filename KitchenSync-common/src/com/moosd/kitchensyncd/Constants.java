package com.moosd.kitchensyncd;

import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.PacketHandler;
import com.moosd.kitchensyncd.sync.SyncScheduler;

public class Constants {
	public static int DTGM_ALL = 0;
	public static int DIRECT_ALL = 0;

	public static int TIME_BTWN_SYNC = 1000 * 60 * 20;

	public static int SO_TIMEOUT = 500;
	public static int SYNC_WAIT_BEFORE_REQUESTING = 1000 *5;

	public static int PACKET_TIMEOUT = 1000 * 60 * 30;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// Init and test networking layer
			
			final Networking net = new Networking("TESTEST_123");
			System.out.println("IsConnected: "+net.isConnected());
			net.hooks.addDtgmHook(Constants.DTGM_ALL, new PacketHandler() {
				@Override
				public void handle(String senderUid, String senderIp,
						int senderPort, byte[] data) {
					System.out.println("[" + senderUid + "] "
							+ new String(data));
					net.directSend(senderIp, senderPort, 1, "Moop!".getBytes());
				}
			});
			
			net.hooks.addDirectHook(1, new PacketHandler() {
				@Override
				public void handle(String senderUid, String senderIp,
						int senderPort, byte[] data) {
					System.out.println("[" + senderUid + "] DIRECT:: "
							+ new String(data));
				}
			});
			net.broadcast(Constants.DTGM_ALL, "hi there!");
			
			// Init sync scheduler
			final SyncScheduler scheduler = new SyncScheduler(net);
			
			// Add sync processes
			
			// Now trigger a sync!
			scheduler.triggerSync();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*public static void main(String[] args) {
		try {
			// Init and test networking layer
			
			final Networking net = new Networking("TESTEST123__");
			System.out.println("IsConnected: "+net.isConnected());
			net.hooks.addDtgmHook(Constants.DTGM_ALL, new PacketHandler() {
				@Override
				public void handle(String senderUid, String senderIp,
						int senderPort, byte[] data) {
					System.out.println("[" + senderUid + "] "
							+ new String(data));
					String bigstring = "";
					for(int i=0; i<100;i++) {
						bigstring+="aaa";
					}
					System.out.println("Constructed");
					net.directSend(senderIp, senderPort, 1, bigstring.getBytes());
				}
			});
			
			net.hooks.addDirectHook(1, new PacketHandler() {
				@Override
				public void handle(String senderUid, String senderIp,
						int senderPort, byte[] data) {
					System.out.println("[" + senderUid + "] DIRECT:: "
							+ new String(data));
				}
			});
			net.broadcast(Constants.DTGM_ALL, "hi there!");
			
			// Init sync scheduler
			final SyncScheduler scheduler = new SyncScheduler(net);
			
			// Add sync processes
			
			// Now trigger a sync!
			scheduler.triggerSync();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
}
