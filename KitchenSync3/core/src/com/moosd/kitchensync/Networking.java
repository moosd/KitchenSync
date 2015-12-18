package com.moosd.kitchensync;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by souradip on 08/12/15.
 */
public class Networking {
    public static Server server;

    public Networking() {
        try {
            server = new Server();
            server.start();
            server.bind(54555, 54777);
            server.getKryo().register(ByteArray.class);

            server.addListener(new Listener() {
                public void received(Connection connection, Object object) {
                    if (object instanceof ByteArray) {
                        try {
                            Message.Packet request = receive(object);
                            System.out.println(request.getEp(0).getText());

                            Message.Packet m = Message.Packet.newBuilder()
                                    .setId(1)
                                    .addEp(
                                            Message.Packet.EchoPacket.newBuilder()
                                                    .setText("thanks")
                                    ).build();
                            send(connection, m);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(Message.Packet m) {

    }

    public static void send(Connection connection, Message.Packet msg) {
        try {
            ByteArray ba = new ByteArray();
            ba.data = Main.crypter.encrypt(msg.toByteArray());
            connection.sendTCP(ba);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Message.Packet receive(Object object) {
        try {
            return Message.Packet.parseFrom(Main.crypter.decrypt(((ByteArray) object).data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // init network connection, announce presence

    // receive other presences

    // receive other connections, do stuff

    public static class ByteArray {
        byte[] data;
    }
}
