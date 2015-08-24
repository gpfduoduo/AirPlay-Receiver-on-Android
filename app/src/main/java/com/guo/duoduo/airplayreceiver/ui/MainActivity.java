package com.guo.duoduo.airplayreceiver.ui;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.http.RequestListenerThread;

import java.io.IOException;
import java.lang.ref.WeakReference;


public class MainActivity extends AppCompatActivity
{
    private AirplayServiceHandler handler;

    private TextView airplayStatusTxView;

    private WifiManager.MulticastLock multicastLock;

    private RequestListenerThread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new AirplayServiceHandler(MainActivity.this);

        initView();
        allowMulticastLock();//启动接收组播消息
        startAirplay();
    }

    private void startAirplay()
    {
        try
        {
            thread = new RequestListenerThread(handler);
            thread.setDaemon(false);
            thread.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private void initView()
    {
        airplayStatusTxView = (TextView) findViewById(R.id.airplay_status);
        airplayStatusTxView.setText("Airplay正在注册...");

    }
    public void onDestroy()
    {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        releaseMulticasLock();

        if (thread != null)
            thread.destroy();
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


    private static class AirplayServiceHandler extends Handler
    {
        private WeakReference<MainActivity> activityWeakReference;

        public AirplayServiceHandler(MainActivity activity)
        {
            this.activityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            final MainActivity activity = this.activityWeakReference.get();
            if (activity == null)
            {
                return;
            }
            switch (msg.what)
            {
                case Constant.Register.OK :
                    activity.airplayStatusTxView.setText("Airplay注册成功");
                    break;
            }
        }
    }

}
