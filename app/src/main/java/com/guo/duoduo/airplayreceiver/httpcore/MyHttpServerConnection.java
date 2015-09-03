package com.guo.duoduo.airplayreceiver.httpcore;


import java.net.Socket;

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.io.HttpRequestParser;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.params.HttpParams;

import android.util.Log;


public class MyHttpServerConnection extends DefaultHttpServerConnection
{

    private static final String tag = MyHttpServerConnection.class.getSimpleName();

    public MyHttpServerConnection()
    {
        super();

    }

    public Socket getCurrentSocket()
    {
        return super.getSocket();
    }

    protected HttpMessageParser createRequestParser(final SessionInputBuffer buffer,
            final HttpRequestFactory requestFactory, final HttpParams params)
    {

        Log.d(tag, "airplay in MyHttpServerConnection ");
        //需增加一个自定义的requestFactory， 处理 HTTP1.1 200 OK的Reverse请求
        return new HttpRequestParser(buffer, new MyLineParser(),
            new MyHttpRequestFactory(), params);

    }

}