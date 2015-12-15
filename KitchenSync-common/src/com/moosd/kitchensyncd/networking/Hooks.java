package com.moosd.kitchensyncd.networking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Hooks {

	Map<Integer, Set<PacketHandler>> packetHooks;
	Map<Integer, Set<PacketHandler>> datagramHooks;

	public Hooks() {
		packetHooks = new HashMap<Integer, Set<PacketHandler>>();
		datagramHooks = new HashMap<Integer, Set<PacketHandler>>();
	}

	public void addDirectHook(int type, PacketHandler hook) {
		Set<PacketHandler> pHandler = packetHooks.get(type);
		if (pHandler == null) {
			pHandler = new HashSet<PacketHandler>();
			packetHooks.put(type, pHandler);
		}
		pHandler.add(hook);
	}


	public void addDtgmHook(int type, PacketHandler hook) {
		Set<PacketHandler> pHandler = datagramHooks.get(type);
		if (pHandler == null) {
			pHandler = new HashSet<PacketHandler>();
			datagramHooks.put(type, pHandler);
		}
		pHandler.add(hook);
	}

	public Set<PacketHandler> getDirectHooks(int type) {
		return packetHooks.get(type);
	}
	public Set<PacketHandler> getDtgmHooks(int type) {
		return datagramHooks.get(type);
	}
}