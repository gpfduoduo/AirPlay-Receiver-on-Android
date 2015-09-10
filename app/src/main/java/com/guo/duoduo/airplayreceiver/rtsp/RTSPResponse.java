package com.guo.duoduo.airplayreceiver.rtsp;


public class RTSPResponse
{

    private StringBuilder response = new StringBuilder();

    public RTSPResponse(String header)
    {
        response.append(header + "\r\n");
    }

    public void append(String key, String value)
    {
        response.append(key + ": " + value + "\r\n");
    }

    /**
     * close the response
     */
    public void finalize()
    {
        response.append("\r\n");
    }

    public String getRawPacket()
    {
        return response.toString();
    }

    @Override
    public String toString()
    {
        return " > " + response.toString().replaceAll("\r\n", "\r\n > ");
    }

}
