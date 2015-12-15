package com.moosd.kitchensyncd.networking;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class Networking {

	public static Networking me;

	public static int PORT = 1940;

	public int serverPort = 0;
	public String instanceId = null;
	public Crypto crypto = null;
	public Hooks hooks = null;
	public DirectSendThread directSend = null;

	public Networking(String key) throws GeneralSecurityException,
			UnsupportedEncodingException {
		init(new RandomString(32).nextString(), key, 0);
	}

	public Networking(String key, int port) throws GeneralSecurityException,
			UnsupportedEncodingException {
		init(new RandomString(32).nextString(), key, port);
	}

	public Networking(String uid, String key) throws GeneralSecurityException,
			UnsupportedEncodingException {
		init(uid, key, 0);
	}

	public void init(String uid, String key, int port) throws GeneralSecurityException,
			UnsupportedEncodingException {
		// Store arguments
		instanceId = uid;
		me = this;

		// Init subsystems
		crypto = new Crypto(key);
		hooks = new Hooks();

		// Init direct receiver
		DirectListener directListen = new DirectListener();
		serverPort = directListen.init(crypto, hooks, port);
		new Thread(directListen).start();

		// Init direct sender
		directSend = new DirectSendThread(crypto, uid, serverPort);
		directSend.start();

		// Init datagram receiver
		new Thread(new DatagramListener(crypto, uid, hooks)).start();
		
		System.out.println("[NET] Networking system operational.");
	}

	public void directSend(String ip, int port, int type, byte[] data) {
		directSend.send(ip, port, type, data);
	}

	public void broadcast(BroadcastPacket s) throws InvalidCipherTextException,
			IllegalBlockSizeException, BadPaddingException {
		try {
			DatagramSocket c = new DatagramSocket();
			c.setBroadcast(true);

			byte[] sendData = crypto.encrypt(SerializationUtils.serialize(s));
/*
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length,
						InetAddress.getByName("255.255.255.255"), PORT);
				c.send(sendPacket);
			} catch (Exception e) {
			}
*/
			// Broadcast the message over all the network interfaces
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaces
						.nextElement();

				if (networkInterface.isLoopback() || !networkInterface.isUp()) {
					continue; // Don't want to broadcast to the loopback
								// interface
				}

				for (InterfaceAddress interfaceAddress : networkInterface
						.getInterfaceAddresses()) {
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if (broadcast == null) {
						continue;
					}

					// Send the broadcast package!
					try {
						DatagramPacket sendPacket = new DatagramPacket(
								sendData, sendData.length, broadcast, PORT);
						c.send(sendPacket);
					} catch (Exception e) {
					}
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isConnected() {
		try {
		Enumeration<NetworkInterface> interfaces = NetworkInterface
				.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = (NetworkInterface) interfaces
					.nextElement();
			if(!networkInterface.isLoopback() && networkInterface.isUp()) return true;
		}
		} catch(SocketException e){e.printStackTrace();}
		//System.out.println("WARN :: Network interfaces all down!");
		return false;
	}

	public void broadcast(int type, String data)
			throws InvalidCipherTextException, IllegalBlockSizeException,
			BadPaddingException {
		broadcast(new BroadcastPacket(serverPort, data.getBytes(), instanceId, type));
	}
	public void broadcast(int type, byte[] data)
			throws InvalidCipherTextException, IllegalBlockSizeException,
			BadPaddingException {
		broadcast(new BroadcastPacket(serverPort, data, instanceId, type));
	}
}
