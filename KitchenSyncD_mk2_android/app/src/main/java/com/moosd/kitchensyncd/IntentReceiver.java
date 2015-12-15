package com.moosd.kitchensyncd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by souradip on 14/12/15.
 */
public class IntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Test intent");
        try {
            //BackgroundService.sync();
            Intent pushIntent = new Intent(context, BackgroundService.class);
            context.startService(pushIntent);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
