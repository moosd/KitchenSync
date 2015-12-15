package com.moosd.kitchensyncd.networking;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.SerializationUtils;

import com.moosd.kitchensyncd.Constants;

public class DirectSendThread extends Thread {

	DatagramSocket serverSocket = null;
	List<SPacket> pendingPackets = null;
	List<SPacket> dumpPackets = null;
	Crypto crypto = null;
	String uid = null;
	int sPort = 0;
	public String subnet = "192.168.9.";

	public DirectSendThread(Crypto crypto, String uid, int sPort) {
		super();
		this.crypto = crypto;
		this.uid = uid;
		this.sPort = sPort;
	}

	// Splits the data into chunks
	public static byte[][] divideArray(byte[] source, int chunksize) {
		byte[][] ret = new byte[(int) Math.ceil(source.length
				/ (double) chunksize)][chunksize];
		int start = 0;

		for (int i = 0; i < ret.length; i++) {
			if (start + chunksize > source.length) {
				System.arraycopy(source, start, ret[i], 0, source.length
						- start);
			} else {
				System.arraycopy(source, start, ret[i], 0, chunksize);
			}
			start += chunksize;
		}
		return ret;
	}

	// Send a packet directly to a node on the network
	public void send(String ip, int port, int type, byte[] data) {
		/*SPacket pkt = new SPacket(ip, port, type, uid);
		byte[][] sendData = divideArray(data, 1024);
		pkt.num = sendData.length;
		for (int i = 0; i < sendData.length; i++) {
			pkt.parts.add(new PartPacket(i, pkt.num, type, pkt.uidSender,
					pkt.uidPacket, sendData[i], sPort));
		}
		synchronized (pendingPackets) {
			pendingPackets.add(pkt);
		}*/
		try {
		Socket clientSocket = new Socket(ip, port);
		  DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		  byte[] dat = crypto.encrypt(SerializationUtils
					.serialize(new PartPacket(0, 1, type, Networking.me.instanceId,
							"aa", data, sPort)));
		  //outToServer.writeBytes(dat.length+"\n");
		  outToServer.writeInt(dat.length);
		  //System.out.println("send:"+dat.length);
		  outToServer.write(dat);
		  outToServer.flush();
		  clientSocket.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public String getIpRange() {
		String ip  = subnet, prefix = "/24";
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				// filters out 127.0.0.1 and inactive interfaces
				if (iface.isLoopback() || !iface.isUp())
					continue;
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for(int i=0;i<addresses.size();i++) {
					InterfaceAddress iaddr= addresses.get(i);
					InetAddress addr = iaddr.getAddress();
					// obtain all ips in subnet, test if reachable, then dump to them
					String iip = addr.getHostAddress();
					if(addr instanceof Inet4Address) {ip = iip;
						if(iaddr.getNetworkPrefixLength() >= 19)
							prefix = "/"+iaddr.getNetworkPrefixLength();
					}
				}
			}
			return ip+prefix;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void dump(int port, int type, byte[] data)  {
		try {
			//SubnetUtils utils = new SubnetUtils(getIpRange());
			//String[] allIps = utils.getInfo().getAllAddresses();
			BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));

			String line = null;
			List<String> iplist = new LinkedList<String>();
			while ((line = br.readLine()) != null) {
				iplist.add(line.split(" ")[0]);
			}

			br.close();
			String[] allIps = iplist.toArray(new String[iplist.size()]);

			dumpPackets = new LinkedList<SPacket>();

			for (int i = 1; i < allIps.length; i++) {
				String sendto = allIps[i];
				SPacket pkt = new SPacket(sendto, port, type, uid);
				byte[][] sendData = divideArray(data, 1024);
				pkt.num = sendData.length;
				for (int ix = 0; ix < sendData.length; ix++) {
					pkt.parts.add(new PartPacket(ix, pkt.num, type, pkt.uidSender,
							pkt.uidPacket, sendData[ix], sPort));
				}
				synchronized (dumpPackets) {
					dumpPackets.add(pkt);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	    /*
	    String[] bits = ip.split("\\.");
	    subnet = bits[0] + "." + bits[1] + "." + bits[2] + ".";

		int more = 1;
		int less = 0;
		if(!prefix.equals("/24")) {
			more = 2;
			less = -1;
		}
			int b = Integer.parseInt(bits[2]);
			String a = bits[0]+"."+bits[1]+".";

			for(int i = less; i < more; i++) {
				subnet = a+(b+i)+".";
				if((b+i) < 0) continue;

				for (int ii = 2; ii < 255; ii++) {
					//System.out.println(subnet+ii);
					SPacket pkt = new SPacket(subnet + ii, port, type, uid);
					byte[][] sendData = divideArray(data, 1024);
					pkt.num = sendData.length;
					for (int ix = 0; ix < sendData.length; ix++) {
						pkt.parts.add(new PartPacket(ix, pkt.num, type, pkt.uidSender,
								pkt.uidPacket, sendData[ix], sPort));
					}
					synchronized (dumpPackets) {
						dumpPackets.add(pkt);
					}
				}
			}
*/
		dumpAndRun();
	}

	// reliable sending.
	public void run() {
		byte[] sendData = new byte[3048];
		pendingPackets = new LinkedList<SPacket>();

		while (true) {
			synchronized (pendingPackets) {
				int longest = 0;
				int ppsz = pendingPackets.size();

				for (int i = 0; i < ppsz; i++) {
					SPacket p = pendingPackets.get(i);
					int psiz = p.parts.size();
					if (psiz > longest)
						longest = psiz;
				}

				for (int i = 0; i < longest; i++) {
					for (int ii = 0; ii < ppsz; ii++) {
						SPacket p = pendingPackets.get(ii);
						if (p.parts.size() > i) {
							PartPacket pp = p.parts.get(i);

							if (pp.sent)
								continue;

							// Make a UDP connection to the client
							DatagramSocket clientSocket = null;
							try {
								clientSocket = new DatagramSocket();
								clientSocket.setSoTimeout(Constants.SO_TIMEOUT);
							} catch (Exception e) {
								// If we timeout, we can't send this right now!
								// Let's try the client again later, move onto
								// another packet.
								Logger.getLogger(getClass().getName()).log(
										Level.SEVERE, null, e);
								break;
							}

							try {
								/*InetAddress IPAddress = InetAddress
										.getByName(p.sendTo);
								sendData = crypto.encrypt(SerializationUtils
										.serialize(pp));

								DatagramPacket sendPacket = new DatagramPacket(
										sendData, sendData.length, IPAddress,
										p.sendPort);
								clientSocket.send(sendPacket);

								byte[] receiveData = new byte[1024];

								DatagramPacket receivePacket = new DatagramPacket(
										receiveData, receiveData.length);
								try {
									clientSocket.receive(receivePacket);
									String dataIn = new String(
											crypto.decrypt(receivePacket
													.getData()));

									if (dataIn.startsWith("RECV ")) {
										String[] arr = dataIn.split(" ");
										synchronized (pendingPackets) {
											for (Packet pack : pendingPackets) {
												if (arr[2]
														.equals(pack.uidPacket)) {
													Iterator<PartPacket> it = pack.parts
															.iterator();
													while (it.hasNext()) {
														PartPacket ppack = it
																.next();
														if (ppack.id == Integer
																.parseInt(arr[1])) {
															if (!ppack.sent)
																p.numSent++;
															ppack.sent = true;
														}
													}
												}
											}
										}
									}
								} catch (Exception e) {
									// clientSocket.close();
								}*/
								pp.sent=true;
								p.numSent++;
								clientSocket.close();

							} catch (Exception e) {
								Logger.getLogger(getClass().getName()).log(
										Level.SEVERE, null, e);
							}
						}
					}
				}

				for (int i = 0; i < pendingPackets.size(); i++) {
					SPacket p = pendingPackets.get(i);
					if (p.numSent == p.parts.size()) {
						pendingPackets.remove(i);
						i--;
						continue;
					}

					long timeNow = System.currentTimeMillis();
					if (p.sendTime + Constants.PACKET_TIMEOUT < timeNow) {
						pendingPackets.remove(i);
						i--;
						continue;
					}
				}
			}

			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
		}
	}

	// fire and forget
	public void dumpAndRun() {
		byte[] sendData = new byte[3048];

		synchronized (dumpPackets) {
			int longest = 0;
			int ppsz = dumpPackets.size();

			for (int i = 0; i < ppsz; i++) {
				SPacket p = dumpPackets.get(i);
				int psiz = p.parts.size();
				if (psiz > longest)
					longest = psiz;
			}

			for (int i = 0; i < longest; i++) {
				for (int ii = 0; ii < ppsz; ii++) {
					SPacket p = dumpPackets.get(ii);
					if (p.parts.size() > i) {
						PartPacket pp = p.parts.get(i);

						if (pp.sent)
							continue;

						// Make a UDP connection to the client
						DatagramSocket clientSocket = null;
						try {
							clientSocket = new DatagramSocket();
							//clientSocket.setSoTimeout(Constants.SO_TIMEOUT*2);
						} catch (Exception e) {
							// If we timeout, we can't send this right now!
							// Let's try the client again later, move onto
							// another packet.
							pp.sent = true;
							break;
						}

						try {
							InetAddress IPAddress = InetAddress
									.getByName(p.sendTo);
							//System.out.println("Sending to "+p.sendTo);
							sendData = crypto.encrypt(SerializationUtils
									.serialize(pp));

							DatagramPacket sendPacket = new DatagramPacket(
									sendData, sendData.length, IPAddress,
									p.sendPort);
							clientSocket.send(sendPacket);

							pp.sent = true;
							p.numSent++;
							clientSocket.close();

						} catch(SocketException e){
							e.printStackTrace();
							/*if (e.getMessage().contains("UNREACH")) {
								System.out.println("ABORT DUMP AND RUN");
								dumpPackets = new LinkedList<SPacket>();
								return;
							}*/
						} catch(Exception e) {
							Logger.getLogger(getClass().getName()).log(
									Level.SEVERE, null, e);
						}
					}
				}
			}

			for (int i = 0; i < dumpPackets.size(); i++) {
				SPacket p = dumpPackets.get(i);
				if (p.numSent == p.parts.size()) {
					dumpPackets.remove(i);
					i--;
					continue;
				}

				long timeNow = System.currentTimeMillis();
				if (p.sendTime + Constants.PACKET_TIMEOUT < timeNow) {
					dumpPackets.remove(i);
					i--;
					continue;
				}
			}
		}
	}

	class SPacket extends Packet {
		String sendTo;
		int sendPort;
		long sendTime;

		int numSent = 0;

		SPacket(String sendTo, int sendPort, int type, String uid) {
			super(new RandomString(32).nextString(), uid, type);
			this.sendTo = sendTo;
			this.sendPort = sendPort;
			sendTime = System.currentTimeMillis();
			numSent = 0;
		}
	}
}
