package com.guo.duoduo.airplayreceiver.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.service.RegisterService;


public class NetWorkReceiver extends BroadcastReceiver
{

    private static final String tag = NetWorkReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent)
    {

        String action = intent.getAction();
        // 这个监听wifi的打开与关闭，与wifi的连接无关

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
        {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra)
            {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;//当然，这边可以更精确的确定状态
                if (isConnected)
                {
                    Log.d(tag, "network is connected");
                    MyApplication.getInstance().startService(
                        new Intent(MyApplication.getInstance(), RegisterService.class));
                }
                else
                {
                    Log.d(tag, "network is not connected");
                    MyApplication.getInstance().stopService(
                        new Intent(MyApplication.getInstance(), RegisterService.class));
                }
            }
        }
    }
}
