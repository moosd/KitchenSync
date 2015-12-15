package com.moosd.kitchensyncd.networking;


public abstract class PacketHandler {
	public abstract void handle(String senderUid, String senderIp,
			int senderPort, byte[] data);
}