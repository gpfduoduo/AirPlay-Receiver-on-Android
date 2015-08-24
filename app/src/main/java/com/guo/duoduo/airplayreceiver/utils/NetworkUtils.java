package com.guo.duoduo.airplayreceiver.utils;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * Created by guo.duoduo on 2015/8/24.
 */
public class NetworkUtils
{

    public static Inet4Address getLocalIpAddress()
    {
        try
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();)
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        if (inetAddress instanceof Inet4Address)
                        {
                            return ((Inet4Address) inetAddress);
                        }
                    }
                }
            }
        }
        catch (SocketException ex)
        {
        }
        return null;
    }

    public static String[] getMACAddress(InetAddress ia) throws Exception
    {
        //获得网络接口对象（即网卡），并得到mac地址，mac地址存在于一个byte数组中。
        byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();

        //下面代码是把mac地址拼装成String
        String[] str_array = new String[2];
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        for (int i = 0; i < mac.length; i++)
        {
            if (i != 0)
            {
                sb1.append(":");
            }
            //mac[i] & 0xFF 是为了把byte转化为正整数
            String s = Integer.toHexString(mac[i] & 0xFF);
            sb1.append(s.length() == 1 ? 0 + s : s);
            sb2.append(s.length() == 1 ? 0 + s : s);
        }
        //把字符串所有小写字母改为大写成为正规的mac地址并返回
        str_array[0] = sb1.toString();
        str_array[1] = sb2.toString();
        return str_array;
        //return sb1.toString().toUpperCase();
    }

}
