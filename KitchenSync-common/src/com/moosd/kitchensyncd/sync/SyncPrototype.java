package com.moosd.kitchensyncd.sync;

public abstract class SyncPrototype implements Runnable {
	
	public boolean upload = false;
	
	public void run() {
		if(upload)
			triggerUpload();
		else
			sync();
	}

	public abstract void sync();
	public abstract void triggerUpload();

	//public abstract int getSyncMode();
}
