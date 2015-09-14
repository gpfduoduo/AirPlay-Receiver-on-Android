package com.guo.duoduo.airplayreceiver.receiver;


import java.util.Timer;
import java.util.TimerTask;

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

            MyApplication.getInstance().stopService(
                new Intent(MyApplication.getInstance(), RegisterService.class));
        }
        else if (Intent.ACTION_SCREEN_ON.equals(action))
        {
            Log.d(tag, "action screen on");
            if (NetworkUtils.isWifiConnected(MyApplication.getInstance()))
            {
                //延迟注册
                Timer timer = new Timer();
                TimerTask task = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        MyApplication.getInstance()
                                .startService(
                                    new Intent(MyApplication.getInstance(),
                                        RegisterService.class));
                    }
                };
                timer.schedule(task, 3 * 1000);
            }
        }

    }
}
