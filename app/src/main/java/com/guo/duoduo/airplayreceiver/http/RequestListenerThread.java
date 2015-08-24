package com.guo.duoduo.airplayreceiver.http;


import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.os.Message;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


/**
 * Created by guo.duoduo on 2015/8/24.
 */
public class RequestListenerThread extends Thread
{
    private static final String tag = RequestListenerThread.class.getSimpleName();

    private android.os.Handler handler;
    private static final int port = 5000;
    private ServerSocket serversocket;
    private HttpParams params;
    private Inet4Address localAddressInet4Address;
    private MyHttpService httpService;

    private JmDNS jmdns_airplay = null;
    private JmDNS jmdns_raop;
    private ServiceInfo airplay_service = null;
    private ServiceInfo raop_service;

    public RequestListenerThread(android.os.Handler handler) throws IOException
    {
        this.handler = handler;

        initHttpServer();
    }

    public void run()
    {
        try
        {
            registerAirplay();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Log.d(tag,
            "airplay http server listen the port :  " + serversocket.getLocalPort());

        ExecutorService exec = Executors.newCachedThreadPool();
        while (!Thread.interrupted())
        {
            try
            {
                Socket socket = this.serversocket.accept();
                Log.d(tag, "airplay incoming connection from " + socket.getInetAddress()
                    + "; socket id= [" + socket + "]");

                MyHttpServerConnection conn = new MyHttpServerConnection();
                Thread thread = new WorkerThread(this.httpService, conn, socket);
                thread.setDaemon(true);
                exec.execute(thread);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                break;
            }
        }
        exec.shutdown();
    }

    private void registerAirplay() throws IOException
    {
        HashMap<String, String> values = new HashMap<String, String>();
        String strMac = null;

        String[] str_Array = new String[2];
        try
        {
            str_Array = NetworkUtils.getMACAddress(localAddressInet4Address);
            strMac = str_Array[0];
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("GPF: 注册Airplay Mac地址：" + strMac);

        values.put("deviceid", strMac);//修改为mac地址
        values.put("features", "0x39f7"); //new Version iOS
        values.put("model", "AppleTV2,1");//
        values.put("srcvers", "130.14");
        values.put("pw", "1");

        String preMac = "GuoDuoITV";
        int tmpi = (int) (Math.random() * 100);

        raop_service = ServiceInfo.create(preMac + "@airplay" + tmpi
            + "._raop._tcp.local", preMac + "@airplay" + tmpi, this.port - 1,
            "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 "
                + "sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");

        airplay_service = ServiceInfo.create(preMac + "@airplay" + tmpi
            + "._airplay._tcp.local.", preMac + "@airplay" + tmpi, this.port, 0, 0,
            values);

        jmdns_airplay = JmDNS.create(localAddressInet4Address);//create的必须绑定ip地址 android 4.0以上
        jmdns_airplay.registerService(airplay_service);

        jmdns_raop = JmDNS.create(localAddressInet4Address);
        jmdns_raop.registerService(raop_service);

        Log.d(tag, "airplay register airplay success");
        Message msg = handler.obtainMessage();
        msg.what = Constant.Register.OK;
        handler.sendMessage(msg);
    }

    private void unregisterAirplay()
    {
        if (jmdns_airplay != null && jmdns_raop != null)
        {
            jmdns_raop.unregisterService(airplay_service);
            jmdns_airplay.unregisterService(raop_service);
            try
            {
                jmdns_airplay.close();
                jmdns_raop.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void initHttpServer() throws IOException
    {
        Log.d(tag, "airplay init http server");

        localAddressInet4Address = NetworkUtils.getLocalIpAddress();
        serversocket = new ServerSocket(port, 20, localAddressInet4Address);

        params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

        BasicHttpProcessor httpProcessor = new BasicHttpProcessor();//http协议处理器
        httpProcessor.addInterceptor(new ResponseDate());//http协议拦截器，响应日期
        httpProcessor.addInterceptor(new ResponseServer());//响应服务器
        httpProcessor.addInterceptor(new ResponseContent());//响应内容
        httpProcessor.addInterceptor(new ResponseConnControl());//响应连接控制

        //http请求处理程序解析器
        HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();

        //http请求处理程序，HttpFileHandler继承于HttpRequestHandler（http请求处理程序
        registry.register("*", new WebServiceHandler());

        httpService = new MyHttpService(httpProcessor,
            new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory());
        httpService.setParams(this.params);
        httpService.setHandlerResolver(registry);//为http服务设置注册好的请求处理器。
    }

    public void destroy()
    {
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

        try
        {
            this.serversocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static class WorkerThread extends Thread
    {
        private final MyHttpService httpService;
        private final HttpServerConnection conn;
        private final Socket socket;

        public WorkerThread(final MyHttpService httpService,
                final HttpServerConnection conn, final Socket socket)
        {
            super();
            this.httpService = httpService;
            this.conn = conn;
            this.socket = socket;
        }

        public void run()
        {
            Log.d(tag, "airplay create new connection thread id = " + this.getId()
                + " handler http client request, socket id = " + "[" + socket +  "]");

            HttpContext context = new BasicHttpContext(null);

            try
            {
                while(!Thread.interrupted() && this.conn.isOpen())
                {
                    this.httpService.handleRequest(this.conn, context);
                }
            }catch (IOException e) {
                e.printStackTrace();
            } catch (HttpException e){
                e.printStackTrace();
            } finally {
                try
                {
                    this.conn.shutdown();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    private static class WebServiceHandler implements HttpRequestHandler
    {

        public WebServiceHandler()
        {
            super();
        }

        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse,
                HttpContext httpContext) throws HttpException, IOException
        {

        }
    }
}
