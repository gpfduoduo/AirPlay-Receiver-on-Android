package com.guo.duoduo.airplayreceiver.rtsp;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.guo.duoduo.airplayreceiver.utils.Debug;


/**
 * Created by Guo.Duo duo on 2015/9/9.
 */
public class LaunchThread extends Thread
{

    private static final String tag = LaunchThread.class.getSimpleName();

    ServerSocket serverSocket = null;
    private boolean stopThread = false;
    private int port;

    public LaunchThread(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            while (!stopThread)
            {
                try
                {
                    Socket socket = serverSocket.accept();
                    Debug.d(tag, "raop get connection from " + socket.toString());

                    new RTSPResponder(socket).start();
                }
                catch (SocketTimeoutException e)
                {
                    e.printStackTrace();
                    break;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (serverSocket != null)
                {
                    serverSocket.close();
                }
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }

    public void destroy()
    {
        Debug.d(tag, "launchThread destroy = ");
        stopThread = true;
        try
        {
            if (serverSocket != null)
                serverSocket.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
