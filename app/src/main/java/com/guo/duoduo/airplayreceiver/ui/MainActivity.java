package com.guo.duoduo.airplayreceiver.ui;


import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.service.ListenService;


public class MainActivity extends Activity
{
    private AirplayServiceHandler handler;
    private TextView airplayStatusTxView;
    private WifiManager.MulticastLock multicastLock;

    private MyController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new AirplayServiceHandler(MainActivity.this);

        mController = new MyController(MainActivity.class.getName(), handler);

        initView();
        allowMulticastLock();//启动接收组播消息

        startListenService();
    }

    private void startListenService()
    {
        Intent intent = new Intent(getApplicationContext(), ListenService.class);
        startService(intent);
    }

    private void initView()
    {
        airplayStatusTxView = (TextView) findViewById(R.id.airplay_status);
        airplayStatusTxView.setText("Airplay正在注册...");

    }

    public void onDestroy()
    {
        super.onDestroy();
        mController.destroy();
        releaseMulticasLock();
        stopService(new Intent(getApplicationContext(), ListenService.class));
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
                case Constant.Register.FAIL :
                    activity.airplayStatusTxView.setText("Airplay注册失败");
                    break;
                case Constant.Msg.Msg_Photo :
                {
                    byte[] pic = (byte[]) msg.obj;
                    Intent intent = new Intent(activity, ImageActivity.class);
                    intent.putExtra("picture", pic);
                    activity.startActivity(intent);
                    break;
                }

                case Constant.Msg.Msg_Video_Play :
                {
                    HashMap<String, String> map = (HashMap) msg.obj;
                    String playUrl = map.get(Constant.PlayURL);
                    String startPos = map.get(Constant.Start_Pos);

                    Intent intent = new Intent(activity, VideoPlayerActivity.class);
                    intent.putExtra("path", playUrl);
                    intent.putExtra("position", Double.valueOf(startPos));
                    activity.startActivity(intent);
                    break;
                }

            }
        }
    }

}
