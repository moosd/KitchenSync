package com.moosd.kitchensyncd.sync.proto;

import java.io.Serializable;



public class SyncRequest implements Serializable {
	public Serializable payload;
	public int type;

	public SyncRequest(int type) {
		this.type = type;
	}
}