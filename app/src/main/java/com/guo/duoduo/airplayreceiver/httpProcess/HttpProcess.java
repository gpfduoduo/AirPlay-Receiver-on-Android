package com.guo.duoduo.airplayreceiver.httpProcess;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpStatus;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.http.HTTP;
import com.guo.duoduo.airplayreceiver.http.HTTPHeader;
import com.guo.duoduo.airplayreceiver.http.HTTPRequest;
import com.guo.duoduo.airplayreceiver.http.HTTPRequestListener;
import com.guo.duoduo.airplayreceiver.http.HTTPResponse;
import com.guo.duoduo.airplayreceiver.http.HTTPServerList;
import com.guo.duoduo.airplayreceiver.http.HTTPSocket;
import com.guo.duoduo.airplayreceiver.service.RegisterService;
import com.guo.duoduo.airplayreceiver.ui.VideoPlayerActivity;
import com.guo.duoduo.airplayreceiver.utils.BplistParser;
import com.guo.duoduo.airplayreceiver.utils.Debug;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;

import io.vov.vitamio.MediaPlayer;


/**
 * Created by Guo.Duo duo on 2015/8/31.
 */
public class HttpProcess implements HTTPRequestListener
{

    private static final String tag = HttpProcess.class.getSimpleName();

    private InetAddress localAddress;
    private String localMac;

    private HTTPServerList httpServerList;
    private int httpPort = 0;

    private HTTPSocket reverseSocket;
    private String reverseSessionId;

    public static Map<String, byte[]> photoCache = Collections
            .synchronizedMap(new HashMap<String, byte[]>());

    private boolean isPlayingPhoto = false;
    private boolean isSlideShow = false;

    public HttpProcess() throws IOException
    {
        initHttpServer();
    }

    @Override
    public void httpRequestReceived(HTTPRequest httpReq)
    {
        Debug.d(tag, "http request received = \r\n" + httpReq.toString());

        String target = httpReq.getURI();

        Debug.d(tag, "http uri = " + httpReq.getURI());

        // 播放视频的时候 只有target为play的时候带有sessionId， 图片每一个命令都有sessionId
        if (target.equals(Constant.Target.SERVER_INFO))
        {
            HTTPResponse response = new HTTPResponse();
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Content-Type", "text/x-apple-plist+xml");
            response.setHeader("Date", new Date().toString());
            String content = Constant.getServerInfoResponse(localMac
                    .toUpperCase(Locale.ENGLISH));
            response.setHeader("Content-Length", content.length());
            response.setContent("\n" + content);

            Debug.d(tag, "server info response = \r\n" + response.toString());
            httpReq.post(response);
        }
        else if (target.equals(Constant.Target.REVERSE))
        {
            HTTPHeader httpHeader = httpReq.getHeader("X-Apple-Session-ID");
            if (httpHeader != null)
            {
                reverseSessionId = httpHeader.getValue();
                Debug.d(tag, "reverse session id = " + reverseSessionId);
            }

            HTTPHeader purposeHeader = httpReq.getHeader("X-Apple-Purpose");
            if(purposeHeader != null)
            {
                String purpose = purposeHeader.getValue();
                Debug.d(tag, "http reverse session purpose = " + purpose);
                if(purpose != null && purpose.equals("slideshow"))
                {
                    isSlideShow = true;
                }
            }
            reverseSocket = httpReq.getSocket();

            HTTPResponse response = new HTTPResponse();
            response.setStatusCode(HttpStatus.SC_SWITCHING_PROTOCOLS);
            response.setHeader("Connection", "Upgrade");
            response.setHeader("Content-Length", 0);
            response.setHeader("Upgrade", "PTTH/1.0");
            httpReq.post(response);

            Debug.d(tag, "reverse response = \r\n" + response.toString());
        }
        else if (target.equals(Constant.Target.PHOTO))
        {
            isPlayingPhoto = true;
            HTTPResponse response = new HTTPResponse();
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Date", new Date().toString());

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Photo;

            byte[] entityContent = null;
            entityContent = httpReq.getContent();

            HTTPHeader assetActionHeader = httpReq.getHeader("X-Apple-AssetAction");
            HTTPHeader assetKeyHeader = httpReq.getHeader("X-Apple-AssetKey");
            if (assetActionHeader != null && assetKeyHeader != null)
            {
                String assetAction = assetActionHeader.getValue();
                String assetKey = assetKeyHeader.getValue();

                if (assetAction != null)
                {
                    if (assetAction.equals("cacheOnly"))
                    {
                        if (assetKey != null & entityContent != null)
                        {
                            if (!photoCache.containsKey(assetKey))
                            {
                                photoCache.put(assetKey, entityContent);
                            }
                        }
                    }
                    else if (assetAction.equals("displayCached"))
                    {
                        if (photoCache.containsKey(assetKey))
                        {
                            byte[] pic = photoCache.get(assetKey);
                            if (pic != null)
                            {
                                msg.obj = pic;
                                MyApplication.broadcastMessage(msg);
                            }
                        }
                        else
                        {
                            response.setStatusCode(HttpStatus.SC_PRECONDITION_FAILED);
                        }
                    }
                }
            }
            else
            {
                if (entityContent != null)
                {
                    msg.obj = entityContent;
                    MyApplication.broadcastMessage(msg);
                }
            }

            httpReq.post(response);
        }
        else if(target.equals(Constant.Target.SLIDER_SHOW))
        {
            HTTPResponse response = new HTTPResponse();

            String returnContent = "";


            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Date", new Date().toString());
            response.setHeader("Content-Type", "text/x-apple-plist+xml");
            response.setHeader("Content-Length", returnContent.length());

            httpReq.post(response);
        }
        else if (target.equals(Constant.Target.STOP))
        {
            HTTPResponse response = new HTTPResponse();

            response.setStatusCode(HttpStatus.SC_OK);
            response.addHeader("Date", new Date().toString());
            response.setHeader("Content-Length", 0);

            httpReq.post(response);

            Message msg = Message.obtain();
            msg.what = Constant.Msg.Msg_Stop;
            MyApplication.broadcastMessage(msg);

            photoCache.clear(); //清除缓存

            //发送一个post stop 消息给apple client
            HTTPRequest httpRequest = new HTTPRequest();
            httpRequest.setMethod(HTTP.POST);
            httpRequest.setSocket(reverseSocket);

            HTTPResponse httpResponse = new HTTPResponse();
            httpResponse.setHeader("Content-Type", "text/x-apple-plist+xml");
            httpResponse.setHeader("X-Apple-Session-ID", reverseSessionId);
            String content;
            if (isPlayingPhoto)
            {
                content = Constant.getImageStopEvent();
                Debug.d(tag, "stop photo airplay");
            }
            else
            {
                content = Constant.getVideoStopEvent();
                Debug.d(tag, "stop video airplay");
            }
            httpResponse.setHeader("Content-Length", content.length());
            httpResponse.setContent(content);

            httpRequest.post(httpResponse);
        }
        else if (target.equals(Constant.Target.PLAY)) //播放视频
        {
            isPlayingPhoto = false;

            HTTPHeader httpHeader = httpReq.getHeader("X-Apple-Session-ID");
            if (httpHeader != null)
            {
                reverseSessionId = httpHeader.getValue();
                Debug.d(tag, "reverse session id = " + reverseSessionId);
            }

            HTTPHeader contentTypeHeader = httpReq.getHeader("Content-Type");
            if (contentTypeHeader != null)
            {
                String playUrl = "";
                Double startPos = 0.0;

                String contentType = contentTypeHeader.getValue();
                //如果是来自 iphone 推送的视频
                if (contentType.equalsIgnoreCase("application/x-apple-binary-plist")) //<BINARY PLIST DATA>
                {
                    //爱奇艺的视频推送就是这样的，而且有点乱码的感觉
                    byte[] content = httpReq.getContent();
                    if(content != null)
                    {
                        try
                        {
                            String strContent = new String(content, "utf-8");
                            HashMap map = BplistParser.parse(content);
                            if(map != null && map.size() > 0)
                            {
                                playUrl = (String) map.get("Content-Location");
                                startPos = (Double) map.get("Start-Position");
                            }
                            else
                            {
                                int startIndex = strContent.indexOf("http://");
                                int endIndex = strContent.indexOf("\"");
                                playUrl = strContent.substring(startIndex, endIndex);
                                Debug.d(tag, "爱奇艺 视频地址：" + playUrl);
                            }

                            Message msg = Message.obtain();
                            HashMap<String, String> mapUrl = new HashMap<String, String>();
                            mapUrl.put(Constant.PlayURL, playUrl);
                            mapUrl.put(Constant.Start_Pos, Double.toString(startPos));
                            msg.what = Constant.Msg.Msg_Video_Play;
                            msg.obj = mapUrl;
                            MyApplication.getInstance().broadcastMessage(msg);
                        } catch (UnsupportedEncodingException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                else if (contentType.equalsIgnoreCase("text/parameters")) //iTunes 推送的视频 或者是优酷之类的
                {
                    String requestBody = new String(httpReq.getContent());
                    if (!TextUtils.isEmpty(requestBody))
                    {
                        playUrl = requestBody.substring(
                            requestBody.indexOf("Content-Location:")
                                + "Content-Location:".length(),
                            requestBody.indexOf("\n",
                                requestBody.indexOf("Content-Location:")));
                        startPos = Double.valueOf(requestBody.substring(
                            requestBody.indexOf("Start-Position:")
                                + "Start-Position:".length(),
                            requestBody.indexOf("\n",
                                requestBody.indexOf("Start-Position:"))));
                        playUrl = playUrl.trim();

                        Debug.d(tag, "airplay playUrl = " + playUrl + "; start Pos ="
                            + startPos);

                        Message msg = Message.obtain();
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put(Constant.PlayURL, playUrl);
                        map.put(Constant.Start_Pos, Double.toString(startPos));
                        msg.what = Constant.Msg.Msg_Video_Play;
                        msg.obj = map;
                        MyApplication.getInstance().broadcastMessage(msg);
                    }
                    else
                    {
                        Debug.d(tag, "http content = null");
                    }
                }
            }

            HTTPResponse response = new HTTPResponse();
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Date", new Date().toString());
            response.setHeader("Content-Length", 0);
            httpReq.post(response);
        }
        //优酷视频通过这个进行client和server的时间同步
        else if (target.startsWith(Constant.Target.SCRUB))
        {//post 就是 seek操作，如果是get则是或者播放的position和duration
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
                    String returnBody = "duration: " + strDuration + "\nposition: "
                        + strCurPos;
                    Log.d(tag, "airplay return scrub message = " + returnBody);

                    HTTPResponse response = new HTTPResponse();
                    response.setStatusCode(HttpStatus.SC_OK);
                    response.setHeader("Date", new Date().toString());
                    response.setHeader("Content-Length", returnBody.length());
                    response.setContent(returnBody);

                    httpReq.post(response);
                }
                else
                //播放视频的界面退出后，手机端也要退出
                {
                    Debug.d(tag, "get scrub Video Activity is Finished");
                    //发送一个post消息给apple client
                    if (reverseSocket != null && reverseSocket.getSocket() != null
                        && reverseSocket.getSocket().isConnected())
                    {
                        HTTPRequest httpRequest = new HTTPRequest();
                        httpRequest.setMethod(HTTP.POST);
                        httpRequest.setSocket(reverseSocket);

                        HTTPResponse httpResponse = new HTTPResponse();
                        httpResponse.setHeader("Content-Type", "text/x-apple-plist+xml");
                        httpResponse.setHeader("X-Apple-Session-ID", reverseSessionId);
                        String content = Constant.getVideoStopEvent();
                        httpResponse.setHeader("Content-Length", content.length());
                        httpResponse.setContent(content);

                        httpRequest.post(httpResponse);
                    }
                    else
                    {
                        Debug.d(tag, "get scrub reverse socket is null");
                    }
                }
            }
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

            HTTPResponse response = new HTTPResponse();
            response.setStatusCode(HttpStatus.SC_OK);
            response.setHeader("Date", new Date().toString());
            response.setHeader("Content-Length", 0);

            httpReq.post(response);
        }
        //腾讯视频是发送这个命令的。
        else if (target.equalsIgnoreCase(Constant.Target.PLAYBACK_INFO))
        {

        }
    }

    private void initHttpServer() throws IOException
    {
        Log.d(tag, "airplay init http server");

        localAddress = NetworkUtils.getLocalIpAddress();

        if(localAddress == null)
        {
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

        InetAddress[] binds = new InetAddress[1];
        binds[0] = localAddress;

        httpServerList = new HTTPServerList(binds, RegisterService.AIRPLAY_PORT);
    }

    public int getHTTPPort()
    {
        return httpPort;
    }

    public void setHTTPPort(int port)
    {
        httpPort = port;
    }

    private HTTPServerList getHTTPServerList()
    {
        return httpServerList;
    }

    public void start()
    {
        stop();

        if(localAddress != null)
        {
            int bindPort = getHTTPPort();
            HTTPServerList httpServerList = getHTTPServerList();
            httpServerList.open(bindPort);
            httpServerList.addRequestListener(this);
            httpServerList.start();
        }
    }

    public void stop()
    {
        HTTPServerList httpServerList = getHTTPServerList();
        if(httpServerList != null)
        {
            httpServerList.stop();
            httpServerList.close();
            httpServerList.clear();
        }
    }
}
