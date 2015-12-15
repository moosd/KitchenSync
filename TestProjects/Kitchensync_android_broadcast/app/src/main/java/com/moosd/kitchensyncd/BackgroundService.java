package com.moosd.kitchensyncd;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;

import com.moosd.networking.Networking;
import com.moosd.networking.PacketHandler;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;


public class BackgroundService extends Service {
    boolean active = false;
    WifiManager.WifiLock wifiLock = null;
    WifiManager.MulticastLock multilock = null;
    PowerManager.WakeLock wakeLock = null;
    public static Networking net = null;

    public BackgroundService() {
        active = false; wifiLock =null; wakeLock = null; multilock = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        subnetupdate();
        new Thread() {
            @Override
            public void run() {

        try {
            System.out.println("Isreachable " + InetAddress.getByName("192.168.0.2").isReachable(100));
        } catch (IOException e) {
            e.printStackTrace();
        }
            }
        }.start();

        WifiManager wifi = (WifiManager)getSystemService( Context.WIFI_SERVICE );
        if(wifi != null){
            WifiManager.MulticastLock lock = wifi.createMulticastLock("Log_Tag");
            lock.acquire();
        }

        // do a sync
        sync();

        return Service.START_STICKY;
    }

    public static void sync() {
        new Thread(){
            @Override
            public void run() {
                BackgroundService.net.broadcast(5, ("" + System.currentTimeMillis() / (1000 * 60)));
            }
        }.start();
    }

    public void subnetupdate() {
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(!active) {
            active = true;

            System.out.println("Created. Broadcast edition.");
            try {
                net = new Networking("TESTEST123");

                net.handle(new PacketHandler(1) {
                    @Override
                    public void run(InetAddress senderIp,
                                    String msg) {
                        // dial main
                        String uri = "tel:" + msg;
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse(uri));
                        startActivity(intent);
                    }
                });
                net.handle(new PacketHandler(2) {
                    @Override
                    public void run(InetAddress ip, String data) {

                        // notify main
                        try {
                            Intent intent = new Intent(BackgroundService.this, CopyIntent.class);
                            intent.setAction("com.moosd.CopyStuff");
                            intent.putExtra("data", data);
                            PendingIntent pIntent = PendingIntent.getService(BackgroundService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            Notification.Builder mBuilder =
                                    new Notification.Builder(BackgroundService.this)
                                            .setSmallIcon(R.drawable.ic_menu_send)
                                            .setContentTitle("From your computer")
                                            .setContentText(data).setAutoCancel(true)
                                            .setContentIntent(pIntent);
                            NotificationManager mNotificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            Notification n = mBuilder.build();
                            n.defaults |= Notification.DEFAULT_SOUND;
                            n.defaults |= Notification.DEFAULT_VIBRATE;
                            mNotificationManager.notify(0, n);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                net.startReceive();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
