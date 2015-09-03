package com.guo.duoduo.airplayreceiver.httpcore;


import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

import android.util.Log;


public class MyLineParser extends BasicLineParser
{
    private static final String tag = MyLineParser.class.getSimpleName();

    public MyLineParser()
    {
    }

    public ProtocolVersion parseProtocolVersion(final CharArrayBuffer buffer,
            final ParserCursor cursor) throws ParseException
    {

        Log.d(tag, "airplay in MyLineParse, protocol version parse");

        Log.d(tag, " airplay in MyLineParse buffer = " + buffer.toString());

        if (buffer == null)
        {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (cursor == null)
        {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }

        //增加Reverse HTTP，iOS发过来200 OK的特殊处理
        if (buffer.toString().startsWith("HTTP/1.1 200 OK"))
        {
            return createProtocolVersion(1, 0);

        }
        else
        {
            return super.parseProtocolVersion(buffer, cursor);
        }
    } // parseProtocolVersion

    /**
     * Parses a request line.
     * 
     * @param buffer a buffer holding the line to parse
     * 
     * @return the parsed request line
     * 
     * @throws ParseException in case of a parse error
     */
    public RequestLine parseRequestLine(final CharArrayBuffer buffer,
            final ParserCursor cursor) throws ParseException
    {

        Log.d(tag,
            "airplay in MyLineParse, parseRequestLine(x,x) buffer=" + buffer.toString());

        if (buffer.toString().startsWith("HTTP/1.1 200 OK"))
        {
            return super.createRequestLine("200", "200", createProtocolVersion(1, 0));
        }
        else
        {
            return super.parseRequestLine(buffer, cursor);
        }

    } // parseRequestLine

}
