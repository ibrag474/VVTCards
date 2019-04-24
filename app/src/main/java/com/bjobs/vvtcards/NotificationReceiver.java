package com.bjobs.vvtcards;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class NotificationReceiver extends AppCompatActivity {

    private static final int PREFERENCE_MODE_PRIVATE = 0;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_receiver);

        Intent intent = getIntent();
        int remindMoney = intent.getIntExtra("remindMoney", 0);
        sharedPref = getSharedPreferences("notif",PREFERENCE_MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.putInt("remindMoney", remindMoney);
        editor.commit();
        finish();
    }
}
