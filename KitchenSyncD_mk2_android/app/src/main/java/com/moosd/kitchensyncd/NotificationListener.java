package com.moosd.kitchensyncd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();

    public static String[] ignoredNotifications = {"com.moosd.kitchensyncd", "com.fsck.k9", "android", "com.woodslink.android.wiredheadphoneroutingfix", "net.dinglisch.android.tasker", "com.cyanogenmod.eleven", "com.arachnoid.sshelper"};

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public String[] lastWhatsapp = {"", ""};

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        boolean present = false;
        final String test = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;
        final String title = extras.getString("android.title");
        final String text = extras.getCharSequence("android.text").toString();
        final boolean whatsapp = (text.contains(" messages from ") && title.equals("WhatsApp"));

        for (String a : ignoredNotifications) {
            if(a.equals(test))
                present = true;
        }

        if(test.equals("com.whatsapp") && !whatsapp){
                lastWhatsapp = new String[]{title, text};
                present = true;
        }

        if (!present) {
            new Thread(){
                @Override
                public void run() {
                    String[] say = {title, text};
                    if(whatsapp)
                        say = lastWhatsapp;
                    BackgroundService.net.directSend.dump(10000, 4, (say[0] + "\n" + say[1]).getBytes());
                }
            }.start();
        }
        //Log.i(TAG, "**********  onNotificationPosted");
        //Log.i(TAG,"ID :" + sbn.getId() + ", " + sbn.getNotification().tickerText + ", " + sbn.getPackageName());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

}