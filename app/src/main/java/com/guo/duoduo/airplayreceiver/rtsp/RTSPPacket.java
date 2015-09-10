package com.guo.duoduo.airplayreceiver.rtsp;


import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RTSPPacket
{
    private String req;
    private String directory;
    private String rtspVersion;
    private String content;
    private Vector<String> headers;
    private Vector<String> headerContent;
    private String rawPacket;

    public RTSPPacket(String packet)
    {
        // Init arrays
        headers = new Vector<String>();
        headerContent = new Vector<String>();
        rawPacket = packet;

        // If packet completed
        // First line
        Pattern p = Pattern.compile("^(\\w+)\\W(.+)\\WRTSP/(.+)\r\n");
        Matcher m = p.matcher(packet);
        if (m.find())
        {
            req = m.group(1);
            directory = m.group(2);
            rtspVersion = m.group(3);
        }

        // Header fields
        p = Pattern.compile("^([\\w-]+):\\W(.+)\r\n", Pattern.MULTILINE);
        m = p.matcher(packet);
        while (m.find())
        {
            headers.add(m.group(1));
            headerContent.add(m.group(2));
        }

        // Content if present or null if not
        p = Pattern.compile("\r\n\r\n(.+)", Pattern.DOTALL);
        m = p.matcher(packet);
        if (m.find())
        {
            content = m.group(1).trim();
            if (content.length() == 0)
            {
                content = null;
            }
        }
    }

    public String getRawPacket()
    {
        return rawPacket;
    }

    public String getContent()
    {
        return content;
    }

    public String getReq()
    {
        return req;
    }

    public String getVersion()
    {
        return rtspVersion;
    }

    public String getDirectory()
    {
        return directory;
    }

    public int getCode()
    {
        return 200;
    }

    public String valueOfHeader(String headerName)
    {
        int i = headers.indexOf(headerName);
        if (i == -1)
        {
            return null;
        }
        return headerContent.elementAt(i);
    }

    @Override
    public String toString()
    {
        String s = " < " + rawPacket.replaceAll("\r\n", "\r\n < ");
        return s;
    }
}
