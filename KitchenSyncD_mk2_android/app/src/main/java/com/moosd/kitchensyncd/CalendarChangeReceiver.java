package com.moosd.kitchensyncd;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by souradip on 15/12/15.
 */
public class CalendarChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Calendar changed, syncing...");
        for (Account ac : AccountManager.get(context).getAccountsByType("org.dmfs.caldav.account")) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(ac, "com.android.calendar", bundle);
        }
    }
}
