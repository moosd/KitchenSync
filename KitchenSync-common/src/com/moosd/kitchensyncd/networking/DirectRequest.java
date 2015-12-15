package com.moosd.kitchensyncd.networking;

import java.util.Collections;
import java.util.Set;

public class DirectRequest implements Runnable {

	protected Packet packet = null;
	protected String serverText = null;
	Hooks hooks = null;

	public DirectRequest(Packet packet, Hooks hooks) {
		super();
		// Store arguments
		this.packet = packet;
		this.hooks = hooks;
	}

	public void run() {
		// We want to process our fragments in the order they were sent, not the
		// order in which they were received
		Collections.sort(packet.parts);

		// Combine the fragments!
		byte[] combined = new byte[1024 * packet.num];
		int sz = packet.parts.size();
		for (int i = 0; i < sz; i++) {
			PartPacket pp = packet.parts.get(i);
			System.arraycopy(pp.data, 0, combined, i * 1024, 1024);
		}

		// Now execute the hooks we have for this packet type

		// 0 - execute on all packets
		Set<PacketHandler> pHandlers = hooks.getDirectHooks(0);

		if (pHandlers != null) {
			//synchronized (pHandlers) {
				for (PacketHandler hook : pHandlers) {
					hook.handle(packet.uidSender, packet.fromIp,
							packet.fromPort, combined);
				}
			//}
		}

		// Execute whatever is associated with the particular hook we have for
		// this type
		if (packet.type != 0) {
			pHandlers = hooks.getDirectHooks(packet.type);
			if (pHandlers != null) {
				//synchronized (pHandlers) {
					for (PacketHandler hook : pHandlers) {
						hook.handle(packet.uidSender, packet.fromIp,
								packet.fromPort, combined);
					}
				//}
			}
		}
	}
}
