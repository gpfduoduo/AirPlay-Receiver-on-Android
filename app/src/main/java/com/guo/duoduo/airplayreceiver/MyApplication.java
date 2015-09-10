package com.guo.duoduo.airplayreceiver;


import java.util.concurrent.ConcurrentHashMap;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;

import com.guo.duoduo.airplayreceiver.receiver.NetWorkReceiver;
import com.guo.duoduo.airplayreceiver.receiver.ScreenStateReceiver;


/**
 * Created by Guo.Duo duo on 2015/8/25.
 */
public class MyApplication extends Application
{

    private static MyApplication instance;

    private ConcurrentHashMap<String, Handler> mHandlerMap = new ConcurrentHashMap<String, Handler>();

    public static MyApplication getInstance()
    {
        return instance;
    }

    public static void broadcastMessage(Message msg)
    {
        for (Handler handler : getInstance().getHandlerMap().values())
        {
            handler.sendMessage(Message.obtain(msg));
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;

        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();
        registerReceiver(screenStateReceiver, screenStateFilter);

        NetWorkReceiver netWorkReceiver = new NetWorkReceiver();
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(netWorkReceiver, wifiFilter);
    }

    public ConcurrentHashMap<String, Handler> getHandlerMap()
    {
        return mHandlerMap;
    }
}
