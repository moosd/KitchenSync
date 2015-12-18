package com.moosd.kitchensyncd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

public class WifiReceiver extends BroadcastReceiver {
    public WifiReceiver() {
    }

    public static boolean isConnectedViaWifi(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (isConnectedViaWifi(context)) {
            try {
                Thread.sleep(2000);
                // announce presence and update everyone's arp cache
                Command command = new Command(0, "/data/nmap/busybox arping -q -c 3 -U -I wlan0 $(ip addr show dev wlan0|grep 'inet '|cut -d' ' -f6|cut -d'/' -f1)") {
                    @Override
                    public void commandOutput(int id, String line) {
                        super.commandOutput(id, line);
                    }

                    @Override
                    public void commandTerminated(int id, String reason) {
                        super.commandTerminated(id, reason);
                        synchronized (WifiReceiver.this) {
                            WifiReceiver.this.notify();
                        }
                    }

                    @Override
                    public void commandCompleted(int id, int exitcode) {
                        super.commandCompleted(id, exitcode);
                        synchronized (WifiReceiver.this) {
                            WifiReceiver.this.notify();
                        }
                    }
                };
                RootTools.getShell(true).add(command);
                synchronized (this) {
                    wait(2000);
                }
                System.out.println("Finished wait");

                Intent pushIntent = new Intent(context, BackgroundService.class);
                context.startService(pushIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
