package com.moosd.kitchensyncd.networking;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.SerializationUtils;

public class DatagramListener implements Runnable {
	DatagramSocket socket = null;
	Crypto crypto = null;
	String uid = null;
	Hooks hooks = null;
	ExecutorService executor = null;

	public DatagramListener(Crypto crypto, String uid, Hooks hooks) {
		super();
		this.crypto = crypto;
		this.uid = uid;
		this.hooks = hooks;
		executor = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(null);
			socket.setBroadcast(true);
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress("0.0.0.0", Networking.PORT));

			while (true) {
				// Receive a packet
				byte[] recvBuf = new byte[15000];
				DatagramPacket packet = new DatagramPacket(recvBuf,
						recvBuf.length);
				socket.receive(packet);

				BroadcastPacket m = null;
				try {
					m = (BroadcastPacket) SerializationUtils.deserialize(crypto
							.decrypt(packet.getData()));
					m.fromIp = packet.getAddress().getHostAddress();

					if (!m.senderUid.equals(uid)) {
						executor.execute(new BroadcastRequest(m));
					}
				} catch (Exception e) {
				}

				m = null;
			}
		} catch (IOException ex) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
		}
	}

	class BroadcastRequest implements Runnable {

		BroadcastPacket bp;

		BroadcastRequest(BroadcastPacket bp) {
			super();
			this.bp = bp;
		}

		@Override
		public void run() {
			//System.out.println("INCOMING BDCAST!");
			Set<PacketHandler> pHandlers = hooks.getDtgmHooks(0);

			if (pHandlers != null) {
				synchronized (pHandlers) {
					for (PacketHandler hook : pHandlers) {
						hook.handle(bp.senderUid, bp.fromIp, bp.fromPort,
								bp.data);
					}
				}
			}

			if (bp.type != 0) {
				pHandlers = hooks.getDtgmHooks(bp.type);
				if (pHandlers != null) {
					synchronized (pHandlers) {
						for (PacketHandler hook : pHandlers) {
							hook.handle(bp.senderUid, bp.fromIp, bp.fromPort,
									bp.data);
						}
					}
				}
			}
		}

	}
}
