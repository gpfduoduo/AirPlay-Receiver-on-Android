package com.guo.duoduo.airplayreceiver.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.service.RegisterService;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


public class MainActivity extends Activity
{
    public final static String TAG = "AirPlay";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (!NetworkUtils.isWifiConnected(MyApplication.getInstance()))
        {
            Log.i(TAG,"MainActivity : is Wifi not Connect");
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        startListenService();
        onBackPressed();
    }

    private void startListenService()
    {
        Intent intent = new Intent(getApplicationContext(), RegisterService.class);
        startService(intent);
        finish();
    }

}