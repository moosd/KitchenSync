package com.moosd.networking;

import java.net.InetAddress;

public class Client {
    public static void main(String[] args) {
        Networking n = new Networking("TESTEST123");
        n.handle(new PacketHandler(1) {
            @Override
            public void run(InetAddress ip, String msg) {
                System.out.println(msg);
            }
        });
        n.startReceive();
    }
}
