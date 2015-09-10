package com.guo.duoduo.airplayreceiver.rtsp;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.regex.Pattern;

import com.guo.duoduo.airplayreceiver.utils.Debug;


/**
 * Created by Guo.Duo duo on 2015/9/9.
 */
public class RTSPResponder extends Thread
{

    private static final String tag = RTSPResponder.class.getSimpleName();

    private Socket socket;

    private int[] fmtp;
    private byte[] aesiv, aeskey;
    private BufferedReader in;
    private static final Pattern completedPacket = Pattern.compile("(.*)\r\n\r\n");

    public RTSPResponder(Socket socket) throws IOException
    {
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public RTSPResponse handlePacket(RTSPPacket packet)
    {
        RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
        response.append("Audio-Jack-Status", "connected; type=analog");
        response.append("CSeq", packet.valueOfHeader("CSeq"));

        String req = packet.getReq();
        String content  = packet.getContent();
        Debug.d(tag, "raop rtsp request = " + req);
        Debug.d(tag, "raop rtsp content = " + content);

        if (req.contains("POST"))
        {

        }
        else if (req.contentEquals("ANNOUNCE"))
        {

        }
        else if (req.contentEquals("TEARDOWN"))
        {

        }

        return response;
    }

    @Override
    public void run()
    {
        try
        {
            do
            {
                Debug.d(tag, "raop listening packets");
                StringBuffer packet = new StringBuffer();
                int ret = 0;
                do
                {
                    char[] buffer = new char[4096];
                    ret = in.read(buffer);
                    packet.append(new String(buffer));
                } while (ret != -1 && !completedPacket.matcher(packet.toString()).find());

                if (ret != -1)
                {
                    RTSPPacket request = new RTSPPacket(packet.toString());
                    Debug.d(tag, "raop rtsp request = " + request.toString());

                    RTSPResponse response = this.handlePacket(request);
                    Debug.d(tag, "raop rtsp response = " + response.toString());

                    try
                    {
                        BufferedWriter oStream = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream()));
                        oStream.write(response.getRawPacket());
                        oStream.flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    if ("TEARDOWN".equals(request.getReq()))
                    {
                        socket.close();
                        socket = null;
                    }
                }
                else
                {
                    socket.close();
                    socket = null;
                }
            } while (socket != null);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (in != null)
                    in.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (socket != null)
                        socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

}
