package com.moosd.kitchensyncd.sync;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.moosd.kitchensyncd.Constants;
import com.moosd.kitchensyncd.networking.Networking;

public class SyncScheduler implements Runnable {
	Networking net = null;
	List<SyncPrototype> syncProcesses;
	ExecutorService executor = null;
	public static SyncScheduler sched = null;

	public SyncScheduler(Networking net) {
		this.net = net;
		sched = this;
		executor = Executors.newCachedThreadPool();
		syncProcesses = new LinkedList<SyncPrototype>();
		new Thread(this).start();
		System.out.println("[SYNC] Sync framework operational.");
	}

	public void addSyncProcess(SyncPrototype proto) {
		synchronized (syncProcesses) {
			syncProcesses.add(proto);
		}
	}

	public void triggerSync() {
		synchronized (syncProcesses) {
			for (SyncPrototype p : syncProcesses) {
				p.upload = false;
				executor.execute(p);
			}
		}
	}

	public void triggerUpload() {
		
		synchronized (syncProcesses) {
			for (SyncPrototype p : syncProcesses) {
				p.upload = true;
				executor.execute(p);
			}
		}
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(100);
				if(net.isConnected()) {
					triggerSync();
					Thread.sleep(Constants.TIME_BTWN_SYNC);
				}
			} catch (Exception e) {
			}
		}
	}

}
