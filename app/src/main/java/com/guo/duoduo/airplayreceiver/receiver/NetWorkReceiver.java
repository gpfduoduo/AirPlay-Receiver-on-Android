package com.guo.duoduo.airplayreceiver.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.service.RegisterService;


public class NetWorkReceiver extends BroadcastReceiver
{

    private static final String tag = NetWorkReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {

        if (intent == null)
            return;

        String action = intent.getAction();
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        Log.d(tag, "net work receiver action = " + action);

        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
        {
            //WIFI开关
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_DISABLED);
            if (wifiState == WifiManager.WIFI_STATE_DISABLED)
            {
                Log.d(tag, "network is not connected");
                MyApplication.getInstance().stopService(
                    new Intent(MyApplication.getInstance(), RegisterService.class));
            }
            else
            {
                Log.d(tag, "network is connected");
                MyApplication.getInstance().startService(
                    new Intent(MyApplication.getInstance(), RegisterService.class));
            }
        }
    }
}
