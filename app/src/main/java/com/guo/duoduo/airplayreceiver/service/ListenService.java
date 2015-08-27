package com.guo.duoduo.airplayreceiver.service;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.http.RequestListenerThread;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


/**
 * Created by Guo.DUO duo on 2015/8/27.
 */
public class ListenService extends Service
{
    private static final String tag = ListenService.class.getSimpleName();

    private static final String airplayName = "郭攀峰的Android实现";
    private MyController myController;
    private ServiceHandler handler;

    private int tmpi = (int) (Math.random() * 100);
    private InetAddress localAddress;
    private JmDNS jmdnsAirplay = null;
    private JmDNS jmdnsRaop;
    private ServiceInfo airplayService = null;
    private ServiceInfo raopService;

    private Timer registerTimer;
    private TimerTask registerTask;

    private HashMap<String, String> values = new HashMap<String, String>();
    private String preMac;

    private RequestListenerThread thread;

    @Override
    public void onCreate()
    {
        super.onCreate();

        handler = new ServiceHandler(ListenService.this);
        myController = new MyController(ListenService.class.getName(), handler);

        new Thread()
        {
            public void run()
            {
                try
                {
                    registerAirplay();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = Constant.Register.FAIL;
                    MyApplication.broadcastMessage(msg);
                    return;
                }
            }
        }.start();

        try
        {
            thread = new RequestListenerThread();
            thread.setDaemon(false);
            thread.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        Log.d(tag, "ListenService onDestroy");
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        myController.destroy();

        new Thread()
        {
            public void run()
            {
                try
                {
                    unregisterAirplay();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();

        if (thread != null)
            thread.destroy();

        stopTimer();

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void registerAirplay() throws IOException
    {
        unregisterAirplay(); //每次注册之前先unregister
        getParams();
        register();

        Log.d(tag, "airplay register airplay success");
        Message msg = Message.obtain();
        msg.what = Constant.Register.OK;
        MyApplication.broadcastMessage(msg);

        //定时器 每隔30s注册一下，实现手机断网后，再重新连接后，还可以发现。
        startTimer();
    }

    private void startTimer()
    {
        registerTimer = new Timer();
        registerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                Log.d(tag, "airplay timer");
            }
        };
        registerTimer.scheduleAtFixedRate(registerTask, 15 * 1000, 15 * 1000);
    }

    private void stopTimer()
    {
        if (registerTimer != null && registerTask != null)
        {
            registerTask.cancel();
            registerTimer.cancel();
            registerTask = null;
            registerTimer = null;
        }
    }

    private void register() throws IOException
    {
        Log.d(tag, "airplay register");
        registerTcpLocal();
        registerRaopLocal();
    }

    private void registerTcpLocal() throws IOException
    {
        airplayService = ServiceInfo.create(airplayName + "._airplay._tcp.local",
            airplayName, RequestListenerThread.port, 0, 0, values);
        jmdnsAirplay = JmDNS.create(localAddress);//create的必须绑定ip地址 android 4.0以上
        jmdnsAirplay.registerService(airplayService);
    }

    private void registerRaopLocal() throws IOException
    {
        String raopName = preMac + "@" + airplayName;
        raopService = ServiceInfo.create(raopName + "._raop._tcp.local", raopName,
            RequestListenerThread.port - 1,
            "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 "
                + "sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");
        jmdnsRaop = JmDNS.create(localAddress);
        jmdnsRaop.registerService(raopService);
    }

    private void getParams()
    {
        String strMac = null;

        localAddress = NetworkUtils.getLocalIpAddress();//获取本地IP对象
        String[] str_Array = new String[2];
        try
        {
            str_Array = NetworkUtils.getMACAddress(localAddress);
            strMac = str_Array[0];
            preMac = str_Array[1];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Log.d(tag, "airplay 注册Airplay Mac地址：" + strMac);

        values.put("deviceid", strMac);//修改为mac地址
        values.put("features", "0x39f7"); //new Version iOS
        values.put("model", "AppleTV2,1");//
        values.put("srcvers", "130.14");
    }

    private void unregisterAirplay()
    {
        Log.d(tag, "un regitser airplay service");

        if (jmdnsAirplay != null && jmdnsRaop != null)
        {
            jmdnsRaop.unregisterService(airplayService);
            jmdnsAirplay.unregisterService(raopService);
            try
            {
                jmdnsAirplay.close();
                jmdnsRaop.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static class ServiceHandler extends Handler
    {
        private WeakReference<ListenService> weakReference;

        public ServiceHandler(ListenService service)
        {
            weakReference = new WeakReference<ListenService>(service);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            ListenService service = weakReference.get();
            if (service == null)
                return;
            switch (msg.what)
            {

            }
        }
    }
}
