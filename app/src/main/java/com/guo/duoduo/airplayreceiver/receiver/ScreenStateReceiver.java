package com.guo.duoduo.airplayreceiver.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.service.RegisterService;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


public class ScreenStateReceiver extends BroadcastReceiver
{

    private static final String tag = ScreenStateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {

        String action = intent.getAction();

        if (Intent.ACTION_SCREEN_OFF.equals(action))
        {
            Log.d(tag, "action screen off");
            if(NetworkUtils.isWifiConnected(MyApplication.getInstance()))
            {
                MyApplication.getInstance().stopService(
                        new Intent(MyApplication.getInstance(), RegisterService.class));
            }
        }
        else if (Intent.ACTION_SCREEN_ON.equals(action))
        {
            Log.d(tag, "action screen on");
            MyApplication.getInstance().startService(
                new Intent(MyApplication.getInstance(), RegisterService.class));
        }

    }
}
