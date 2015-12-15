package com.moosd.networking;

import java.net.InetAddress;

/**
 * Created by souradip on 12/12/15.
 */
public abstract class PacketHandler {
    int type;

    public PacketHandler(int type) {
        this.type = type;
    }

    public abstract void run(InetAddress sender, String msg);
}
