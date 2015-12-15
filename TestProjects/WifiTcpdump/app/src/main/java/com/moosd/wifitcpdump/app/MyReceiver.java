package com.moosd.wifitcpdump.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Running tcpdumpd...");
        new Thread() {
            @Override
            public void run() {
                Command command = new Command(0, "/data/local/userinit.d/90tcpdumpd");
                try {
                    RootTools.getShell(true).add(command);
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.start();
    }
}
