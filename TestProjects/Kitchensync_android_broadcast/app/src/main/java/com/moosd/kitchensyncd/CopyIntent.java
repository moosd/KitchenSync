package com.moosd.kitchensyncd;

import android.app.IntentService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.Context;

public class CopyIntent extends IntentService {

    public CopyIntent() {
        super("CopyIntent");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //System.out.println("Copy text");
        if (intent != null) {
            final String action = intent.getStringExtra("data");
            //System.out.println("Copy text - "+action);
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text from computer", action);
            clipboard.setPrimaryClip(clip);
        }
    }
}
