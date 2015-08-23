package com.guo.duoduo.airplayreceiver.service;


import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import java.lang.ref.WeakReference;


public class WebService extends Service
{

    private WebServiceHandler webServiceHandler;

    public WebService()
    {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        webServiceHandler = new WebServiceHandler(WebService.this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webServiceHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static class WebServiceHandler extends Handler
    {
        private WeakReference<WebService> service;

        public WebServiceHandler(WebService service)
        {
            this.service = new WeakReference<WebService>(service);
        }

        @Override
        public void handleMessage(Message msg)
        {
            final WebService service = this.service.get();
            if(service == null)
            {
                return;
            }

        }
    }
}
