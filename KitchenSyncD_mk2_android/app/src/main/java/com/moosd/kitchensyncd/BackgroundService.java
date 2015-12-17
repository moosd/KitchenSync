package com.moosd.kitchensyncd;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.*;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.PowerManager;

import android.provider.Contacts;
import android.provider.ContactsContract;
import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.PacketHandler;
import com.moosd.kitchensyncd.sync.OneWaySync;
import com.moosd.kitchensyncd.sync.SyncScheduler;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.File;
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
    public static Context ctx;

    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            System.out.println("contacts changed, syncing...");
            for (Account ac : AccountManager.get(ctx).getAccountsByType("org.dmfs.carddav.account")) {
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                ContentResolver.requestSync(ac, "com.android.contacts", bundle);
            }
        }
    }

    public class MyFileObserver extends FileObserver {
        public String absolutePath;

        public MyFileObserver(String path) {
            super(path, FileObserver.ALL_EVENTS);
            absolutePath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null) {
                return;
            }
            if ((FileObserver.CREATE & event)!=0||(FileObserver.MODIFY & event)!=0||(FileObserver.MOVED_TO & event)!=0) {
                sync();
            }
        }
    }

    public class MyFileObserver2 extends FileObserver {
        public String absolutePath;
        long last = 0;

        public MyFileObserver2(String path) {
            super(path, FileObserver.ALL_EVENTS);
            absolutePath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null) {
                return;
            }


            if ((FileObserver.MODIFY & event)!=0 && path.equals("db.sqlite")) {
                System.out.println("calendar contacts db event! path="+path+", event ="+event  );
                if(last < (System.currentTimeMillis() - 5000)/* && !(new File("/dev/syncinprogress").exists())*/) {
                    System.out.println("Should sync back to laptop now.");
                    // in 5 secs, sync back
                    /*new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try{
                                Thread.sleep(5000);
                            } catch(Exception e){e.printStackTrace();}
                      */      sync();
                        /*}
                    }.start()*/;
                }
                last = System.currentTimeMillis();
            }
        }
    }

    MyContentObserver contentObserver = new MyContentObserver();
    MyFileObserver cameraWatcher = new MyFileObserver("/sdcard/DCIM/Camera/");
    MyFileObserver2 pimWatcher = new MyFileObserver2("/sdcard/www/baikal/Specific/db/");

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
        /*wakeLock.release();
        wifiLock.release();
        multilock.release();*/
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        subnetupdate();

        // do a sync
        sync();

        return Service.START_STICKY;
    }

    public static void sync() {
        new Thread(){
            @Override
            public void run() {
                BackgroundService.net.directSend.dump(10000, 5, (""+System.currentTimeMillis()/(1000*60)).getBytes());
            }
        }.start();
    }

    public void subnetupdate() {
        if(net != null) {
            String ip = getIPAddress(true);
            if(!ip.equals("")) {
                String[] bits = ip.split("\\.");
                if(bits.length == 4) {
                    String subnet = bits[0] + "." + bits[1] + "." + bits[2] +  ".";
                    System.out.println("Subnet: " + subnet);
                    net.directSend.subnet = subnet;
                }
            }
        }
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
            ctx = this;
            this.getApplicationContext().getContentResolver().registerContentObserver (ContactsContract.Contacts.CONTENT_URI, true, contentObserver);
            cameraWatcher.startWatching();
            // disabled instant syncing because may want to add more events later. Will sync on next connection.
            pimWatcher.startWatching();

            System.out.println("Created.");
            try {
                // Init and test networking layer
                net = new Networking("TESTEST123", 10000);
                subnetupdate();
                System.out.println("IsConnected: "+net.isConnected());

                net.hooks.addDirectHook(1, new PacketHandler() {
                    @Override
                    public void handle(String senderUid, String senderIp,
                                       int senderPort, byte[] data) {
                        String dat = new String(data).trim();

                        // dial main
                        String uri = "tel:" + dat;
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse(uri));
                        startActivity(intent);
                    }
                });
                net.hooks.addDirectHook(2, new PacketHandler() {
                    @Override
                    public void handle(String senderUid, String senderIp,
                                       int senderPort, byte[] data) {
                        String dat = new String(data).trim();
                        System.out.println("[" + senderUid + "] "
                                + dat);
                        System.out.println(dat);

                        // notify main
                        try {
                            Intent intent = new Intent(BackgroundService.this, CopyIntent.class);
                            intent.setAction("com.moosd.CopyStuff");
                            intent.putExtra("data", dat);
                            PendingIntent pIntent = PendingIntent.getService(BackgroundService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                            Notification.Builder mBuilder =
                                    new Notification.Builder(BackgroundService.this)
                                            .setSmallIcon(R.drawable.ic_menu_send)
                                            .setContentTitle("From your computer")
                                            .setContentText(dat).setAutoCancel(true)
                                            .setContentIntent(pIntent);
                            NotificationManager mNotificationManager =
                                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            Notification n = mBuilder.build();
                            n.defaults |= Notification.DEFAULT_SOUND;
                            n.defaults |= Notification.DEFAULT_VIBRATE;
                            mNotificationManager.notify(0, n);
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                });


                net.hooks.addDirectHook(3, new PacketHandler() {
                    @Override
                    public void handle(String senderUid, String senderIp,
                                       int senderPort, byte[] data) {
                        String dat = new String(data).trim();
                        System.out.println("[" + senderUid + "] 3 - time to sync - "
                                + dat);
                        System.out.println(dat);

                        sync();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}