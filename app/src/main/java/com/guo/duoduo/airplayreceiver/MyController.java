package com.guo.duoduo.airplayreceiver;


import android.os.Handler;


public class MyController
{
    private String mName;
    private Handler mHandler;

    public MyController(String name, Handler handler)
    {
        MyApplication.getInstance().getHandlerMap().put(name, handler);
        this.mName = name;
        this.mHandler = handler;
    }

    public void destroy()
    {
        MyApplication.getInstance().getHandlerMap().remove(mName);
    }
}
