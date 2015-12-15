package com.moosd.kitchensyncd.networking;

import java.io.Serializable;

public class BroadcastPacket implements Serializable {
	private static final long serialVersionUID = -688147949097367811L;
	
	transient String fromIp;
	int fromPort;
	byte[] data;
	String senderUid;
	int type;

	public BroadcastPacket(int sendPort, byte[] data, String senderUid, int type) {
		this.data = data;
		this.senderUid = senderUid;
		fromPort = sendPort;
		this.type = type;
	}
}
