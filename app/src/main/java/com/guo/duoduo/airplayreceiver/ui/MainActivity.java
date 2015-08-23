package com.guo.duoduo.airplayreceiver.ui;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.service.WebService;


public class MainActivity extends AppCompatActivity
{

    private WifiManager.MulticastLock multicastLock;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        allowMulticastLock();//启动接收组播消息
        startWebService();
    }

    public void onDestroy()
    {
        super.onDestroy();
        stopService(new Intent(MainActivity.this, WebService.class));
        releaseMulticasLock();
    }

    private void startWebService()
    {
        startService(new Intent(MainActivity.this, WebService.class));
    }

    private void allowMulticastLock()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast.test");
        multicastLock.acquire();
    }

    private void releaseMulticasLock()
    {
        if (multicastLock != null)
        {
            multicastLock.release();
        }

    }
}
