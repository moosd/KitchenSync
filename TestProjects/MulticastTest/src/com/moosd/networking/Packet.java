package com.moosd.networking;

/**
 * Created by souradip on 12/12/15.
 */
public class Packet {
    int id = -1;
    int type = -1;
    String msg;

    public Packet(String indata) {
        String[] arr = indata.split(":");
        if(arr.length < 3) throw new RuntimeException("Error parsing packet "+indata);
        id=Integer.parseInt(arr[0]);
        type = Integer.parseInt(arr[1]);
        msg=indata.substring(arr[0].length() + arr[1].length() + 2).trim();
    }

    public Packet(int id, int type, String msg) {
        this.id = id;
        this.type = type;
        this.msg = msg;
    }

    public String toString() {
        return id+":"+type+":"+msg;
    }
}
