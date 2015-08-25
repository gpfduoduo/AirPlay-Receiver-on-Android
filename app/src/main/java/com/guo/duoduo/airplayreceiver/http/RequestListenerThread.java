package com.guo.duoduo.airplayreceiver.http;


import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.utils.BplistParser;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


/**
 * Created by guo.duoduo on 2015/8/24.
 */
public class RequestListenerThread extends Thread
{
    private static final String tag = RequestListenerThread.class.getSimpleName();

    private static final int port = 5000;
    private ServerSocket serversocket;
    private HttpParams params;
    private InetAddress localAddress;
    private MyHTTPService httpService;

    private JmDNS jmdns_airplay = null;
    private JmDNS jmdns_raop;
    private ServiceInfo airplay_service = null;
    private ServiceInfo raop_service;

    private static String localMac = null;

    protected static Map<String, Socket> socketMaps = Collections
            .synchronizedMap(new HashMap<String, Socket>());

    public static Map<String, byte[]> photoCacheMaps = Collections
            .synchronizedMap(new HashMap<String, byte[]>());

    public RequestListenerThread() throws IOException
    {
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
            Message msg = Message.obtain();
            msg.what = Constant.Register.FAIL;
            MyApplication.broadcastMessage(msg);
            return;
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
                conn.bind(socket, this.params);

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

        InetAddress ia = NetworkUtils.getLocalIpAddress();//获取本地IP对象
        String[] str_Array = new String[2];
        try
        {
            str_Array = NetworkUtils.getMACAddress(ia);
            strMac = str_Array[0];
            localMac = strMac.toUpperCase(Locale.ENGLISH);
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
        values.put("pw", "1");

        String preMac = str_Array[1];
        int tmpi = (int) (Math.random() * 100);

        String name = preMac + "@airplay" + tmpi;

        airplay_service = ServiceInfo.create(preMac + "@airplay" + tmpi
            + "._airplay._tcp.local", name, this.port, 0, 0, values);

        raop_service = ServiceInfo.create(preMac + "@airplay" + tmpi
            + "._raop._tcp.local", name, this.port - 1,
            "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 "
                + "sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");

        jmdns_airplay = JmDNS.create(localAddress);//create的必须绑定ip地址 android 4.0以上
        jmdns_airplay.registerService(airplay_service);

        jmdns_raop = JmDNS.create(localAddress);
        jmdns_raop.registerService(raop_service);

        Log.d(tag, "airplay register airplay success");
        Message msg = Message.obtain();
        msg.what = Constant.Register.OK;
        MyApplication.broadcastMessage(msg);
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

        localAddress = NetworkUtils.getLocalIpAddress();
        serversocket = new ServerSocket(port, 20, localAddress);

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

        httpService = new MyHTTPService(httpProcessor,
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
        private static final String tag = WorkerThread.class.getSimpleName();

        private final MyHTTPService httpService;
        private final MyHttpServerConnection conn;
        private final Socket socket;

        public WorkerThread(final MyHTTPService httpService,
                final MyHttpServerConnection conn, final Socket socket)
        {
            super();
            this.httpService = httpService;
            this.conn = conn;
            this.socket = socket;
        }

        public void run()
        {
            Log.d(tag, "airplay create new connection thread id = " + this.getId()
                + " handler http client request, socket id = " + "[" + socket + "]");

            HttpContext context = new BasicHttpContext(null);

            try
            {
                while (!Thread.interrupted() && this.conn.isOpen())
                {
                    this.httpService.handleRequest(this.conn, context);

                    String needSendReverse = (String) context
                            .getAttribute(Constant.Need_sendReverse);
                    String sessionId = (String) context.getAttribute(Constant.SessionId);
                    if (needSendReverse != null && sessionId != null)
                    {
                        if (socketMaps.containsKey(sessionId))
                        {
                            Socket socket = (Socket) socketMaps.get(sessionId);
                            String httpMsg = (String) context
                                    .getAttribute(Constant.ReverseMsg);
                            Log.d(tag, "airplay sendReverseMsg: " + httpMsg
                                + " on socket " + "[" + socket + "]" + "; sessionId = "
                                + sessionId);

                            sendReverseMsg(socket, httpMsg);
                            context.removeAttribute(Constant.Need_sendReverse);
                            context.removeAttribute(Constant.ReverseMsg);

                            if (Constant.Status.Status_stop.equals(needSendReverse))
                            {
                                if (socket != null && !socket.isClosed())
                                {
                                    socket.close();
                                    socketMaps.remove(sessionId);

                                }
                                this.conn.shutdown();
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (HttpException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    this.conn.shutdown();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        private void sendReverseMsg(Socket socket, String httpMsg)
        {
            if (socket == null || TextUtils.isEmpty(httpMsg))
                return;
            if (socket.isConnected())
            {
                OutputStreamWriter osw;
                try
                {
                    osw = new OutputStreamWriter(socket.getOutputStream());
                    osw.write(httpMsg);
                    osw.flush();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class WebServiceHandler implements HttpRequestHandler
    {
        private static final String tag = WebServiceHandler.class.getSimpleName();

        public WebServiceHandler()
        {
            super();
        }

        //在这个方法中我们就可以处理请求的业务逻辑
        @Override
        public void handle(HttpRequest httpRequest, HttpResponse httpResponse,
                HttpContext httpContext) throws HttpException, IOException
        {
            Log.d(tag, "airplay in WebServiceHandler");

            String method = httpRequest.getRequestLine().getMethod()
                    .toUpperCase(Locale.ENGLISH);

            MyHttpServerConnection currentConn = (MyHttpServerConnection) httpContext
                    .getAttribute(ExecutionContext.HTTP_CONNECTION);

            String target = httpRequest.getRequestLine().getUri();
            Header typeHead = httpRequest.getFirstHeader("content-type");
            String contentType = "";
            if (null != typeHead)
                contentType = typeHead.getValue();
            Log.d(tag, "airplay  incoming HTTP  method = " + method + "; target = "
                + target + "; contentType = " + contentType);

            Header sessionHead = httpRequest.getFirstHeader("X-Apple-Session-ID");
            String sessionId = null;
            if (sessionHead != null)
            {
                sessionId = sessionHead.getValue();
                httpContext.setAttribute(Constant.SessionId, sessionId);
            }

            String requestBody = "";
            byte[] entityContent = null;
            if (httpRequest instanceof HttpEntityEnclosingRequest)
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest)
                        .getEntity();
                entityContent = EntityUtils.toByteArray(entity);
            }

            if (target.equals(Constant.Target.REVERSE))
            {
                httpResponse.setStatusCode(HttpStatus.SC_SWITCHING_PROTOCOLS);

                /*
                 * HTTP/1.1 101 Switching Protocols Date: Fri Jul 06 07:17:13
                 * 2012 Upgrade: PTTH/1.0 Connection: Upgrade
                 */
                httpResponse.addHeader("Upgrade", "PTTH/1.0");
                httpResponse.addHeader("Connection", "Upgrade");

                // 增加一个HashMap保留这个Socket， <Apple-SessionID> ---- <Socket>
                currentConn.setSocketTimeout(0);
                // 获取当前的socket
                Socket reverseSocket = currentConn.getCurrentSocket();

                if (null != sessionId)
                {
                    socketMaps.put(sessionId, reverseSocket);
                    Log.d(tag, "airplay receive Reverse, keep Socket in HashMap, key="
                        + sessionId + "; value=" + reverseSocket + ";total Map="
                        + socketMaps);
                }
            }
            else if (target.equals(Constant.Target.SERVER_INFO))
            {
                String responseStr = Constant.getServerInfoResponse(localMac
                        .toUpperCase(Locale.ENGLISH));
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());
                httpResponse.setEntity(new StringEntity(responseStr));
            }
            else if (target.equals(Constant.Target.STOP)) //停止消息
            {
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());
                StringEntity body = new StringEntity("");
                body.setContentType("text/html");
                httpResponse.setEntity(body);

                httpContext.setAttribute(Constant.Need_sendReverse,
                    Constant.Status.Status_stop);
                httpContext.setAttribute(Constant.ReverseMsg,
                    Constant.getEventMsg(0, sessionId, Constant.Status.Status_stop));

                Message msg = Message.obtain();
                msg.what = Constant.Msg.Msg_Stop;
                MyApplication.broadcastMessage(msg);

                photoCacheMaps.clear();
            }
            else if (target.equals(Constant.Target.PHOTO)) //推送的是图片
            {
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                StringEntity returnBody = new StringEntity("HTTP return 200 OK!", "UTF-8");
                returnBody.setContentType("text/html");
                httpResponse.setEntity(returnBody);

                Message msg = Message.obtain();
                msg.what = Constant.Msg.Msg_Photo;
                if (!httpRequest.containsHeader("X-Apple-AssetAction"))
                {
                    Log.d(
                        tag,
                        "airplay display image" + "; assetKey = "
                            + httpRequest.getFirstHeader("X-Apple-AssetKey"));
                    msg.obj = entityContent;
                    MyApplication.broadcastMessage(msg);
                }
                else
                {
                    String assetAction = httpRequest
                            .getFirstHeader("X-Apple-AssetAction").getValue();
                    String assetKey = httpRequest.getFirstHeader("X-Apple-AssetKey")
                            .getValue();
                    if ("cacheOnly".equals(assetAction))
                    {
                        Log.d(tag, "airplay cached image, assetKey = " + assetKey);

                        if (assetKey != null & entityContent != null)
                        {
                            if (!photoCacheMaps.containsKey(assetKey))
                            {
                                photoCacheMaps.put(assetKey, entityContent);
                            }
                        }
                    }
                    else if ("displayCached".equals(assetAction))
                    {
                        Log.d(tag, "airplay display cached image, assetKey = " + assetKey);
                        if (photoCacheMaps.containsKey(assetKey))
                        {
                            byte[] pic = photoCacheMaps.get(assetKey);
                            if (pic != null)
                            {
                                msg.obj = pic;
                                MyApplication.broadcastMessage(msg);
                            }
                        }
                        else
                        {
                            httpResponse.setStatusCode(HttpStatus.SC_PRECONDITION_FAILED);
                        }

                    }
                }
            }
            else if (target.equals(Constant.Target.PLAY)) //推送的视频
            {
                String playUrl = "";
                Double startPos = 0.0;

                requestBody = new String(entityContent);
                Log.d(tag, " airplay play action request content = " + requestBody);
                //如果是来自 iphone 推送的视频
                if (contentType.equalsIgnoreCase("application/x-apple-binary-plist"))
                {
                    HashMap map = BplistParser.parse(entityContent);
                    playUrl = (String) map.get("Content-Location");
                    startPos = (Double) map.get("Start-Position");
                }
                else
                { //iTunes 推送的视频 或者是优酷之类的
                    playUrl = requestBody.substring(
                        requestBody.indexOf("Content-Location:")
                            + "Content-Location:".length(),
                        requestBody.indexOf("\n",
                            requestBody.indexOf("Content-Location:")));
                    startPos = Double
                            .valueOf(requestBody.substring(
                                requestBody.indexOf("Start-Position:")
                                    + "Start-Position:".length(),
                                requestBody.indexOf("\n",
                                    requestBody.indexOf("Start-Position:"))));
                    playUrl = playUrl.trim();
                }

                Log.d(tag, "airplay playUrl = " + playUrl + "; start Pos ="
                        + startPos);

                Message msg = Message.obtain();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(Constant.PlayURL, playUrl);
                map.put(Constant.Start_Pos, Double.toString(startPos));
                msg.what = Constant.Msg.Msg_Video_Play;
                msg.obj = map;
                MyApplication.getInstance().broadcastMessage(msg);

                httpResponse.setStatusCode(HttpStatus.SC_OK);
                StringEntity returnBody = new StringEntity("HTTP return 200 OK!", "UTF-8");
                returnBody.setContentType("text/html");
                httpResponse.setEntity(returnBody);

                // /send /event
                httpContext.setAttribute(Constant.Need_sendReverse, Constant.Status.Status_play);
                httpContext.setAttribute(Constant.ReverseMsg,
                        Constant.getEventMsg(1, sessionId, Constant.Status.Status_play));
            }
            else if(target.equals(Constant.Target.SCRUB)) //获取当前播放的duration 和 position
            {
                StringEntity returnBody = new StringEntity("");


            }
            else if(target.equals(Constant.Target.RATE)) //设置播放的速率
            {

            }
        }
    }
}
