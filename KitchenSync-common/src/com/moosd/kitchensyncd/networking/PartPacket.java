package com.moosd.kitchensyncd.networking;
import java.io.Serializable;

public class PartPacket implements Serializable, Comparable<PartPacket> {
	private static final long serialVersionUID = 2205534923679508140L;
	public int id;
	int last;
	int type;
	String uidSender;
	String uidPacket;
	byte[] data;
	transient boolean sent;
	int sPort;

	public PartPacket(int id, int last, int type, String sender, String packet, byte[] data, int sPort) {
		this.id = id;
		this.last = last;
		this.type = type;
		this.uidSender = sender;
		this.uidPacket = packet;
		this.data = data;
		this.sPort = sPort;
		sent = false;
	}

	@Override
	public int compareTo(PartPacket other) throws ClassCastException {
		PartPacket another = (PartPacket) other;
		return (last == another.last && type == another.type
				&& uidSender.equals(another.uidSender)
				&& uidPacket.equals(another.uidPacket) ? id - another.id : -1);
	}
}