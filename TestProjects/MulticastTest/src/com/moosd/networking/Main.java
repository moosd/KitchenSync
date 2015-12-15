package com.moosd.networking;

public class Main {

    public static void main(String[] args) {
        Networking n = new Networking("TESTEST123");
        n.broadcast(2, "hello world");
    }
}
