package com.moosd.kitchensync;

import com.google.protobuf.InvalidProtocolBufferException;
import org.keyczar.Crypter;
import org.keyczar.KeyczarTool;
import org.keyczar.exceptions.KeyczarException;

import java.io.File;

public class Main {

    public static Crypter crypter;
    public static Networking net;

    public void initKeys() {
        if (!new File("/tmp/test/meta").exists()) {
            KeyczarTool.main(new String[]{"create", "--location=/tmp/test", "--purpose=crypt"});
            KeyczarTool.main(new String[]{"addkey", "--location=/tmp/test", "--status=primary"});
        }
    }

    public Main() throws KeyczarException, InvalidProtocolBufferException {
        System.out.print("* Initialising crypto...");
        initKeys();
        crypter = new Crypter("/tmp/test");
        System.out.println(" [DONE]");

        System.out.print("* Initialising network layer...");
        net = new Networking();
        System.out.println(" [DONE]");

        Message.Packet m = Message.Packet.newBuilder()
                .setId(1)
                .addEp(
                        Message.Packet.EchoPacket.newBuilder()
                                .setText("hello world")
                ).build();
        byte[] ciphertext = crypter.encrypt(m.toByteArray());

        System.out.println(Message.Packet.parseFrom(crypter.decrypt(ciphertext)).getEp(0).getText());

    }

    public static void main(String[] args) {
        try {
            new Main();
            } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
