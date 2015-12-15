package com.moosd.kitchensyncd.networking;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.SerializationUtils;

import com.moosd.kitchensyncd.Constants;

public class DirectListener implements Runnable {

	DatagramSocket serverSocket = null;
	ServerSocket tcpServer = null;
	List<Packet> pendingPackets = null;
	Crypto crypto = null;
	Hooks hooks = null;
	ExecutorService executor = null;

	public int init(Crypto crypto, Hooks hooks, final int port) {
		// Store arguments
		this.crypto = crypto;
		this.hooks = hooks;

		// Init data structures
		pendingPackets = new LinkedList<Packet>();
		executor = Executors.newFixedThreadPool(10);

		// Create server socket
		try {
			serverSocket = new DatagramSocket(port);
			tcpServer = new ServerSocket(serverSocket.getLocalPort());
		} catch (Exception e) {
			throw new RuntimeException("Could not create server socket");
		}
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				while(true){
					try {
						final Socket connectionSocket = tcpServer.accept();
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
								 InputStream inFromClient = connectionSocket.getInputStream();
								 connectionSocket.setSoTimeout(Constants.SO_TIMEOUT*10);
								 DataInputStream in =
							               new DataInputStream(inFromClient);
								 /*BufferedReader d
						          = new BufferedReader(new InputStreamReader(inFromClient));*/
						 
								 //String inn = in.readLine();
								 //int len = Integer.parseInt(inn);
								 int len = in.readInt();
								// System.out.println("recv: "+len);
								 
								 byte[] dat = new byte[len];
								 
								 in.readFully(dat, 0, len);
								 
									PartPacket ppkt = (PartPacket) SerializationUtils
											.deserialize(Networking.me.crypto.decrypt(dat));
									String fromIp = connectionSocket.getInetAddress().getHostAddress();
									
									//executor.execute(new DirectRequest(new Packet(ppkt, IPAddress.getHostAddress()), Networking.me.hooks));
									
									Set<PacketHandler> pHandlers = Networking.me.hooks.getDirectHooks(0);

									if (pHandlers != null) {
										//synchronized (pHandlers) {
											for (PacketHandler hook : pHandlers) {
												hook.handle(ppkt.uidSender, fromIp,
														ppkt.sPort, ppkt.data);
											}
										//}
									}

									// Execute whatever is associated with the particular hook we have for
									// this type
									if (ppkt.type != 0) {
										pHandlers = Networking.me.hooks.getDirectHooks(ppkt.type);
										if (pHandlers != null) {
											//synchronized (pHandlers) {
												for (PacketHandler hook : pHandlers) {
													hook.handle(ppkt.uidSender,fromIp,
															ppkt.sPort, ppkt.data);
												}
											//}
										}
									}
									
									in.close();
									connectionSocket.close();
									
								} catch(Exception e){e.printStackTrace();}
							}
						}).start();
					} catch(Exception e) {e.printStackTrace();}
				}
			}
		}).start();

		// Return allocated port
		return serverSocket.getLocalPort();
	}

	public void run() {
		// Buffers to receive chunks in
		byte[] receiveData = new byte[2048];
		byte[] sendData = new byte[3048];

		while (true) {
			// Wait and receive a UDP packet
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try {
				serverSocket.receive(receivePacket);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			// Try and decrypt it - if this fails, silently drop it
			try {
				byte[] pkt = crypto.decrypt(receivePacket.getData());

				// It's for us! Now deserialise it into a fragment data
				// structure
				PartPacket ppkt = (PartPacket) SerializationUtils
						.deserialize(pkt);

				// Store some info about the sender
				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();

				// Check if we've received a fragment of a new packet or old
				// packet
				boolean handled = false;
				for (Packet pp : pendingPackets) {
					if (pp.uidSender.equals(ppkt.uidSender)
							&& pp.uidPacket.equals(ppkt.uidPacket)
							&& pp.num == ppkt.last) {
						if (!pp.hasId(ppkt.id))
							pp.addPart(ppkt);
						handled = true;
						break;
					}
					if (pp.parts.contains(ppkt)) {
						handled = true;
						break;
					}
				}

				// If it wasn't an old packet, it has to be a new packet!
				if (!handled) {
					pendingPackets.add(new Packet(ppkt, IPAddress.getHostAddress()));
				}

				// Acknowledge receipt of this packet, whether we handled it or
				// not.
				sendData = crypto.encrypt("RECV " + ppkt.id + " "
						+ ppkt.uidPacket);
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, IPAddress, port);
				serverSocket.send(sendPacket);

			} catch (Exception e) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
						null, e);
			}

			// Check if we have any complete packets to handle
			processCompletePackets();
			// If we haven't received any new fragments of a partially complete
			// packet in a while, drop it.
			cleanupOldPackets();
		}
	}

	public synchronized void processCompletePackets() {
		// Loop over the packets
		Iterator<Packet> it = pendingPackets.iterator();
		while (it.hasNext()) {
			Packet p = it.next();
			// Have we received the whole packet?
			if (p.isComplete()) {
				// Handle it in a new thread to speed things along and remove it
				executor.execute(new DirectRequest(p, hooks));
				it.remove();
			}
		}
	}

	public synchronized void cleanupOldPackets() {
		// Timeout a packet after 30 mins
		long tcmp = System.currentTimeMillis() - 1000 * 60 * 30;
		Iterator<Packet> it = pendingPackets.iterator();
		while (it.hasNext()) {
			Packet p = it.next();
			if (p.time < tcmp) {
				it.remove();
			}
		}
	}

	public synchronized void stop() {
		// Stop listening!
		this.serverSocket.close();
	}

}
