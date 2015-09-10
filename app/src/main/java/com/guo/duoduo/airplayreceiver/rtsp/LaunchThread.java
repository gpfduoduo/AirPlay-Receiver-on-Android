package com.guo.duoduo.airplayreceiver.rtsp;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.guo.duoduo.airplayreceiver.service.RegisterService;
import com.guo.duoduo.airplayreceiver.utils.Debug;


/**
 * Created by Guo.Duo duo on 2015/9/9.
 */
public class LaunchThread extends Thread
{

    private static final String tag = LaunchThread.class.getSimpleName();

    private boolean stopThread = false;
    private int port;

    public LaunchThread(int port)
    {
        this.port = port;
    }

    @Override
    public void run()
    {
        ServerSocket serverSocket = null;

        try
        {
            serverSocket = new ServerSocket(RegisterService.RAOP_PORT);
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
                    if (serverSocket != null)
                        serverSocket.close();
                }
            }
            if (serverSocket != null)
                serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            try
            {
                if (serverSocket != null)
                    serverSocket.close();
            }
            catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }

    public void destroy()
    {
        stopThread = true;
        this.interrupt();

    }
}
