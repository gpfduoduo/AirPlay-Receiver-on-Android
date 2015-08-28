package com.guo.duoduo.airplayreceiver;


import android.app.Application;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by Guo.duoduo on 2015/8/25.
 */
public class MyApplication extends Application
{

    private static MyApplication instance;

    private ConcurrentHashMap<String, Handler> mHandlerMap = new ConcurrentHashMap<String, Handler>();

    private boolean isVideoFinished = false;

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
    }

    public static MyApplication getInstance()
    {
        return instance;
    }

    public ConcurrentHashMap<String, Handler> getHandlerMap()
    {
        return mHandlerMap;
    }

    public synchronized static void broadcastMessage(Message msg)
    {
        for (Handler handler : getInstance().getHandlerMap().values())
        {
            handler.sendMessage(Message.obtain(msg));
        }
    }

    public boolean isVideoActivityFinished()
    {
        return isVideoFinished;
    }

    public void setVideoActivityFinish(boolean finished)
    {
        this.isVideoFinished = finished;
    }
}
