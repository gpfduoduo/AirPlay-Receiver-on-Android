package com.guo.duoduo.airplayreceiver.httpcore;


import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.guo.duoduo.airplayreceiver.service.RegisterService;
import com.guo.duoduo.airplayreceiver.ui.VideoPlayerActivity;
import com.guo.duoduo.airplayreceiver.utils.BplistParser;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


/**
 * Created by Guo.Duo duo on 2015/8/24.
 */
public class RequestListenerThread extends Thread
{
    private static final String tag = RequestListenerThread.class.getSimpleName();

    public static Map<String, byte[]> photoCacheMaps = Collections
            .synchronizedMap(new HashMap<String, byte[]>());
    private static Map<String, Socket> socketMaps = Collections
            .synchronizedMap(new HashMap<String, Socket>());
    private static String localMac = null;
    private ServerSocket serversocket;
    private HttpParams params;
    private InetAddress localAddress;
    private MyHttpService httpService;

    public RequestListenerThread()
    {
    }

    public void run()
    {
        try
        {
            Thread.sleep(2 * 1000);
            initHttpServer();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ExecutorService exec = Executors.newCachedThreadPool();

        while (!Thread.interrupted())
        {
            try
            {
                if (serversocket == null)
                    break;

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
        Log.d(tag, "exec shut down");
    }

    @Override
    public void destroy()
    {
        try
        {
            Log.d(tag, "serverSocket destroy");
            this.interrupt();
            this.serversocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initHttpServer() throws IOException
    {
        Log.d(tag, "airplay init http server");

        localAddress = NetworkUtils.getLocalIpAddress();

        if (localAddress == null)
        {
            Thread.interrupted();
            return;
        }

        String[] str_Array = new String[2];
        try
        {
            str_Array = NetworkUtils.getMACAddress(localAddress);
            String strMac = str_Array[0];
            localMac = strMac.toUpperCase(Locale.ENGLISH);
            Log.d(tag, "airplay local mac = " + localMac);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        serversocket = new ServerSocket(RegisterService.AIRPLAY_PORT, 2, localAddress);
        serversocket.setReuseAddress(true);

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

    private static class WorkerThread extends Thread
    {
        private static final String tag = WorkerThread.class.getSimpleName();

        private final MyHttpService httpService;
        private final MyHttpServerConnection conn;
        private final Socket socket;

        public WorkerThread(final MyHttpService httpService,
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

                    Log.d(tag, "socket maps size = " + socketMaps.size());

                    String needSendReverse = (String) context
                            .getAttribute(Constant.Need_sendReverse);
                    if (socketMaps.size() != 1)
                        continue;

                    String sessionId = (String) socketMaps.entrySet().iterator().next()
                            .getKey();
                    Log.d(tag, "airplay need send reverse = " + needSendReverse
                        + "; sessionId = " + sessionId);

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
                                    Log.d(tag, "airplay close socket");
                                    socket.close();
                                    socketMaps.clear();
                                }
                                this.conn.shutdown();
                                this.interrupt();
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
                    Log.d(tag, "connection shutdown");
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

    /**
     * all the protocol interactions between apple client (iphone ipad) and
     * Android device are processed here
     */
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
            String sessionId = "";

            //IOS 8.4.1 播放视频的时候 只有target为play的时候带有sessionId， 图片每一个命令都有sessionId

            if (sessionHead != null)
            {
                sessionId = sessionHead.getValue();
                Log.d(tag, "incoming HTTP airplay session id =" + sessionId);

                if (target.equals(Constant.Target.REVERSE)
                    || target.equals(Constant.Target.PLAY))
                {
                    socketMaps.clear();
                    Socket reverseSocket = currentConn.getCurrentSocket();
                    if (!socketMaps.containsKey(sessionId))
                    {
                        socketMaps.put(sessionId, reverseSocket);
                        Log.d(tag, "airplay add sock to maps");
                    }
                }
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
                    if (!socketMaps.containsKey(sessionId))
                    {
                        socketMaps.put(sessionId, reverseSocket);
                        Log.d(tag,
                            "airplay receive Reverse, keep Socket in HashMap, key="
                                + sessionId + "; value=" + reverseSocket + ";total Map="
                                + socketMaps);
                    }
                }
            }
            else if (target.equals(Constant.Target.SERVER_INFO))
            {
                String responseStr = Constant.getServerInfoResponse(localMac);
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());
                httpResponse.setEntity(new StringEntity(responseStr));
            }
            else if (target.equals(Constant.Target.STOP)) //停止消息
            {
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());

                Message msg = Message.obtain();
                msg.what = Constant.Msg.Msg_Stop;
                MyApplication.broadcastMessage(msg);

                httpContext.setAttribute(Constant.Need_sendReverse,
                    Constant.Status.Status_stop);
                httpContext.setAttribute(Constant.ReverseMsg,
                    Constant.getStopEventMsg(0, sessionId, Constant.Status.Status_stop));

                photoCacheMaps.clear();
            }
            else if (target.equals(Constant.Target.PHOTO)) //推送的是图片
            {
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.setHeader("Date", new Date().toString());

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
                {  //<BINARY PLIST DATA>
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

                Log.d(tag, "airplay playUrl = " + playUrl + "; start Pos =" + startPos);

                Message msg = Message.obtain();
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(Constant.PlayURL, playUrl);
                map.put(Constant.Start_Pos, Double.toString(startPos));
                msg.what = Constant.Msg.Msg_Video_Play;
                msg.obj = map;
                MyApplication.getInstance().broadcastMessage(msg);

                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.setHeader("Date", new Date().toString());
            }
            else if (target.startsWith(Constant.Target.SCRUB)) //post 就是 seek操作，如果是get则是或者播放的position和duration
            {
                StringEntity returnBody = new StringEntity("");

                if (target.indexOf("?position=") > 0)
                {//post方法
                    int index = target.indexOf("?position=") + 10;
                    float pos = new Float(target.substring(index));
                    Log.d(tag, "airplay seek position =" + pos); //此时的单位是秒
                    Message msg = Message.obtain();
                    msg.what = Constant.Msg.Msg_Video_Seek;
                    msg.obj = pos;
                    MyApplication.getInstance().broadcastMessage(msg);
                }
                else
                //get方法 获取播放的duration and position
                {
                    long duration = 0;
                    long curPos = 0;

                    if (!VideoPlayerActivity.isVideoActivityFinished())
                    {
                        duration = VideoPlayerActivity.getDuration();
                        curPos = VideoPlayerActivity.getCurrentPosition();
                        duration = duration < 0 ? 0 : duration;
                        curPos = curPos < 0 ? 0 : curPos;
                        Log.d(tag, "airplay get method scrub: duration=" + duration
                            + "; position=" + curPos);

                        //毫秒需要转为秒
                        DecimalFormat decimalFormat = new DecimalFormat(".000000");//
                        String strDuration = decimalFormat.format(duration / 1000f);
                        String strCurPos = decimalFormat.format(curPos / 1000f);

                        //must have space, duration: **.******, or else, apple client can not syn with android
                        String returnStr = "duration: " + strDuration + "\nposition: "
                            + strCurPos;
                        Log.d(tag, "airplay return scrub message = " + returnStr);
                        returnBody = new StringEntity(returnStr, "UTF-8");
                    }
                    else
                    //播放视频的界面退出后，手机端也要退出
                    {
                        httpContext.setAttribute(Constant.Need_sendReverse,
                            Constant.Status.Status_stop);
                        httpContext.setAttribute(Constant.ReverseMsg, Constant
                                .getStopEventMsg(1, sessionId,
                                    Constant.Status.Status_stop));
                    }
                }

                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.setHeader("Date", new Date().toString());
                httpResponse.setEntity(returnBody);
            }
            else if (target.startsWith(Constant.Target.RATE)) //设置播放的速率(其实就是播放和暂停)
            {
                int playState = Constant.Msg.Msg_Video_Resume;
                String status = Constant.Status.Status_play;
                if (target.indexOf("value=1") > 0) //正常速率播放
                {
                    playState = Constant.Msg.Msg_Video_Resume;
                    status = Constant.Status.Status_play;
                }
                else if (target.indexOf("value=0") > 0) //暂停播放了
                {
                    playState = Constant.Msg.Msg_Video_Pause;
                    status = Constant.Status.Status_pause;
                }

                Message msg = Message.obtain();
                msg.what = playState;
                MyApplication.getInstance().broadcastMessage(msg);

                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.setHeader("Date", new Date().toString());
            }
            //IOS 8.4.1 从来不发 这个命令（优酷不发送，腾讯 video是发送的）
            else if (target.equalsIgnoreCase(Constant.Target.PLAYBACK_INFO))
            {
                Log.d(tag, "airplay received playback_info request");
                String playback_info = "";
                long duration = 0;
                long cacheDuration = 0;
                long curPos = 0;

                String status = Constant.Status.Status_stop;

                if (VideoPlayerActivity.isVideoActivityFinished())
                {
                    Log.d(tag, " airplay video activity is finished");
                    status = Constant.Status.Status_stop;

                    httpContext.setAttribute(Constant.Need_sendReverse, status);
                    httpContext.setAttribute(Constant.ReverseMsg,
                        Constant.getStopEventMsg(1, sessionId, status));
                }
                else
                {
                    curPos = VideoPlayerActivity.getCurrentPosition();
                    duration = VideoPlayerActivity.getDuration();
                    cacheDuration = curPos;
                    if (curPos == -1 || duration == -1 || cacheDuration == -1)
                    {
                        status = Constant.Status.Status_load;
                        playback_info = Constant.getPlaybackInfo(0, 0, 0, 0); //
                    }
                    else
                    {
                        status = Constant.Status.Status_play;
                        playback_info = Constant.getPlaybackInfo(duration / 1000f,
                            cacheDuration / 1000f, curPos / 1000f, 1);
                    }

                    httpContext.setAttribute(Constant.Need_sendReverse, status);
                    httpContext.setAttribute(Constant.ReverseMsg,
                        Constant.getVideoEventMsg(sessionId, status));
                }

                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());
                httpResponse.addHeader("Content-Type", "text/x-apple-plist+xml");
                httpResponse.setEntity(new StringEntity(playback_info));
            }
            else if (target.equals("/fp-setup"))
            {
                Log.d(tag, "airplay setup content  ="
                    + new String(entityContent, "UTF-8"));
                httpResponse.setStatusCode(HttpStatus.SC_OK);
                httpResponse.addHeader("Date", new Date().toString());
            }
            else
            {
                Log.d(tag, "airplay default not process");
            }
        }
    }
}
