package com.moosd.networking;

import java.io.IOException;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Created by souradip on 12/12/15.
 */
public class Networking {
    final static int PORT = 8888;

    public Crypto crypto;
    Random randomGenerator;
    LinkedList<PacketHandler> handlers;

    LinkedHashMap<Integer, String> pastPkts;

    public Networking(String key) {
        try {
            crypto = new Crypto(key);
            randomGenerator = new Random();
            handlers = new LinkedList<PacketHandler>();
            pastPkts = new LinkedHashMap<Integer, String>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                    return this.size() > 10;
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InetAddress getBroadcast() throws SocketException {
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {
                        //System.out.println(interfaceAddress.getAddress());
                        return interfaceAddress.getBroadcast();
                    }
                }
            }
        }
        return null;
    }


    public void broadcast(int type, String message) {
        try {
            Packet p = new Packet(randomGenerator.nextInt(200), type, message);
            DatagramSocket socket = new DatagramSocket(PORT + 1);
            socket.setBroadcast(true);
            byte[] msg = crypto.encrypt(p.toString());

            DatagramPacket packet = new DatagramPacket(msg, msg.length,
                    getBroadcast(), PORT);
            socket.send(packet);

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public void handle(PacketHandler ph) {
        handlers.add(ph);
    }

    public void startReceive() {
        new Thread() {
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(PORT);
                    socket.setBroadcast(true);
                    while (true) {
                        byte[] buf = new byte[1024];
                        final DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                        socket.receive(msgPacket);
                        try {
                            String msg = new String(crypto.decrypt(buf)).trim();
                            System.out.println(msg);
                            final Packet pkt = new Packet(msg);
                            if (!pastPkts.containsValue(pkt.id)) {
                                pastPkts.put(pkt.id, "r");

                                for (final PacketHandler h : handlers) {
                                    if (h.type == pkt.type || h.type == 0) {
                                        new Thread() {
                                            public void run() {
                                                try {
                                                    h.run(msgPacket.getAddress(), pkt.msg);

                                                } catch (
                                                        Exception e
                                                        )

                                                {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }.start();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
