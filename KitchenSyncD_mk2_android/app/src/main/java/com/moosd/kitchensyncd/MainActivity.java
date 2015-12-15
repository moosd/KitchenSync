package com.moosd.kitchensyncd;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.moosd.kitchensyncd.networking.Networking;


public class MainActivity extends Activity {

    Button button = null, buttonSync = null;
    EditText editText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i= new Intent(this, BackgroundService.class);
        startService(i);
        button = (Button) findViewById(R.id.button);
        buttonSync = (Button) findViewById(R.id.button2);
        editText = (EditText) findViewById(R.id.editText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    new Thread(){
                        @Override
                        public void run() {
                            BackgroundService.net.directSend.dump(10000, 3, (editText.getText().toString()).getBytes());
                        }
                    }.start();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        buttonSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    BackgroundService.sync();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
