package com.guo.duoduo.airplayreceiver.service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Locale;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.guo.duoduo.airplayreceiver.MyApplication;
import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.httpProcess.HttpProcess;
import com.guo.duoduo.airplayreceiver.httpcore.RequestListenerThread;
import com.guo.duoduo.airplayreceiver.rtsp.LaunchThread;
import com.guo.duoduo.airplayreceiver.ui.ImageActivity;
import com.guo.duoduo.airplayreceiver.ui.VideoPlayerActivity;
import com.guo.duoduo.airplayreceiver.utils.NetworkUtils;


/**
 * Created by Guo.DUO duo on 2015/8/27.
 */
public class RegisterService extends Service
{
    public static final int AIRPLAY_PORT = 8192;
    public static final int RAOP_PORT = 5000;
    private static final String tag = RegisterService.class.getSimpleName();
    private static final String airplayType = "._airplay._tcp.local";
    private static final String raopType = "._raop._tcp.local";
    private String airplayName = "郭攀峰";
    private MyController myController;
    private ServiceHandler handler;
    private InetAddress localAddress;
    private JmDNS jmdnsAirplay = null;
    private JmDNS jmdnsRaop;
    private ServiceInfo airplayService = null;
    private ServiceInfo raopService;

    private HashMap<String, String> values = new HashMap<String, String>();
    private String preMac;

    private RequestListenerThread thread;

    private HttpProcess httpProcess;
    private LaunchThread raopThread;

    public static PrivateKey pk;
    private WifiManager.MulticastLock lock;

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.d(tag, "register service onCreate");

        WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("mylockthereturn");
        lock.setReferenceCounted(true);
        lock.acquire();

        try
        {
            Resources resources = this.getResources();
            InputStream is = resources.openRawResource(R.raw.key);
            pk = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(getByteArrayFromStream(is)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }

        airplayName = android.os.Build.MODEL + "@" + airplayName;

        handler = new ServiceHandler(RegisterService.this);
        myController = new MyController(RegisterService.class.getName(), handler);

        Toast toast = android.widget.Toast.makeText(getApplicationContext(),
            "正在注册Airplay服务...", android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        new Thread()
        {
            public void run()
            {
                try
                {
                    thread = new RequestListenerThread();
                    thread.setDaemon(false);
                    thread.start();

                    registerAirplay();

                    //                            httpProcess = new HttpProcess();
                    //                            httpProcess.setHTTPPort(RegisterService.AIRPLAY_PORT);
                    //                            httpProcess.start();
                    //
                    //                            raopThread = new LaunchThread(RAOP_PORT);
                    //                            raopThread.start();
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        Log.d(tag, "RegisterService onDestroy");
        super.onDestroy();
        myController.destroy();

        lock.release();

        new Thread()
        {
            public void run()
            {
                if (thread != null) //airplay关闭服务很慢，将thread关闭放在前面
                    thread.destroy();

                //                    if (httpProcess != null)
                //                        httpProcess.stop();
                //
                //                    if (raopThread != null)
                //                    {
                //                        raopThread.destroy();
                //                    }

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

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void registerAirplay() throws IOException
    {
        Message msg = Message.obtain();
        if (!getParams())
        {
            msg.what = Constant.Register.FAIL;
        }
        else
        {
            register();
            Log.d(tag, "airplay register airplay success");
        }
        msg.what = Constant.Register.OK;
        MyApplication.broadcastMessage(msg);
    }

    private void register() throws IOException
    {
        Log.d(tag, "airplay register");
        registerTcpLocal();
        registerRaopLocal();
    }

    private void registerTcpLocal() throws IOException
    {
        airplayService = ServiceInfo.create(airplayName + airplayType, airplayName,
            AIRPLAY_PORT, 0, 0, values);
        jmdnsAirplay = JmDNS.create(localAddress);//create的必须绑定ip地址 android 4.0以上
        jmdnsAirplay.registerService(airplayService);
    }

    private void registerRaopLocal() throws IOException
    {
        String raopName = preMac + "@" + airplayName;
        raopService = ServiceInfo.create(raopName + raopType, raopName, RAOP_PORT,
            "tp=UDP sm=false sv=false ek=1 et=0,1 cn=0,1 ch=2 ss=16 "
                + "sr=44100 pw=false vn=3 da=true md=0,1,2 vs=103.14 txtvers=1");
        jmdnsRaop = JmDNS.create(localAddress);
        jmdnsRaop.registerService(raopService);
    }

    private boolean getParams()
    {
        String strMac = null;

        try
        {
            Thread.sleep(2 * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        localAddress = NetworkUtils.getLocalIpAddress();//获取本地IP对象
        if (localAddress == null)
        {
            Log.d(tag, "local address = null");
            return false;
        }
        String[] str_Array = new String[2];
        try
        {
            str_Array = NetworkUtils.getMACAddress(localAddress);
            if (str_Array == null)
                return false;
            strMac = str_Array[0].toUpperCase(Locale.ENGLISH);
            preMac = str_Array[1].toUpperCase(Locale.ENGLISH);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        Log.d(tag, "airplay 注册Airplay Mac地址：" + strMac + "; preMac = " + preMac);

        values.put("deviceid", strMac);//修改为mac地址
        values.put("features", "0x297f"); //
        values.put("model", "AppleTV2,1");//
        values.put("srcvers", "130.14");

        return true;
    }

    private void unregisterAirplay()
    {
        Log.d(tag, "un register airplay service");

        if (jmdnsAirplay != null)
        {
            jmdnsAirplay.unregisterService(airplayService);
            try
            {
                jmdnsAirplay.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (jmdnsRaop != null)
        {
            jmdnsRaop.unregisterService(raopService);
            try
            {
                jmdnsRaop.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private byte[] getByteArrayFromStream(InputStream is) throws IOException
    {
        byte[] b = new byte[10000];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = is.read(b, 0, b.length)) > 0)
        {
            out.write(b, 0, read);
        }
        return out.toByteArray();
    }

    private static class ServiceHandler extends Handler
    {
        private WeakReference<RegisterService> weakReference;

        public ServiceHandler(RegisterService service)
        {
            weakReference = new WeakReference<RegisterService>(service);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            RegisterService service = weakReference.get();
            if (service == null)
                return;
            switch (msg.what)
            {
                case Constant.Register.OK :
                {
                    Toast toast = Toast.makeText(service.getApplicationContext(),
                        "Airplay注册成功", android.widget.Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                    break;
                case Constant.Register.FAIL :
                {
                    Toast toast = Toast.makeText(service.getApplicationContext(),
                        "Airplay注册失败", android.widget.Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    service.stopSelf();
                    android.os.Process.killProcess(android.os.Process.myPid()); //完全退出程序
                    break;
                }
                case Constant.Msg.Msg_Photo :
                {
                    byte[] pic = (byte[]) msg.obj;
                    Intent intent = new Intent(service, ImageActivity.class);
                    intent.putExtra("picture", pic);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    service.startActivity(intent);
                    break;
                }

                case Constant.Msg.Msg_Video_Play :
                {
                    HashMap<String, String> map = (HashMap) msg.obj;
                    String playUrl = map.get(Constant.PlayURL);
                    String startPos = map.get(Constant.Start_Pos);

                    Intent intent = new Intent(service, VideoPlayerActivity.class);
                    intent.putExtra("path", playUrl);
                    intent.putExtra("position", Double.valueOf(startPos));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    service.startActivity(intent);
                    break;
                }

            }
        }
    }
}
