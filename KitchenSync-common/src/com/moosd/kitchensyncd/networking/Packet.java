package com.moosd.kitchensyncd.networking;

import java.util.LinkedList;
import java.util.List;


public class Packet {
	List<PartPacket> parts;
	String uidPacket, uidSender;
	int type, num;
	long time;
	String fromIp;
	int fromPort;

	public Packet(String uidPacket, String uidSender, int type) {
		// Init data structs
		parts = new LinkedList<PartPacket>();
		// Store arguments
		this.uidPacket = uidPacket;
		this.uidSender = uidSender;
		this.type = type;
	}

	public Packet(PartPacket p, String ip) {
		// Init data structs
		parts = new LinkedList<PartPacket>();
		// Init variables using the initial packet fragment
		uidPacket = p.uidPacket;
		uidSender = p.uidSender;
		fromIp = ip;
		fromPort = p.sPort;
		type = p.type;
		num = p.last;
		time = System.currentTimeMillis();
		addPart(p);
	}

	boolean isComplete() {
		// Is the number of fragments we have == the number that was sent?
		return (parts.size() == num);
	}

	boolean hasId(int i) {
		// Loop over the fragments to see if we have a particular fragment
		for (PartPacket pp : parts) {
			if (i == pp.id)
				return true;
		}
		return false;
	}

	void addPart(PartPacket p) {
		// Add the fragment to our collection
		parts.add(p);
		time = System.currentTimeMillis();
	}
}