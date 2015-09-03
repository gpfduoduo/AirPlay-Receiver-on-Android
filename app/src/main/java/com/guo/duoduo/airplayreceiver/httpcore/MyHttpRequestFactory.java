package com.guo.duoduo.airplayreceiver.httpcore;


import org.apache.http.HttpRequest;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;


public class MyHttpRequestFactory extends DefaultHttpRequestFactory
{
    private static final String[] RFC2616_COMMON_METHODS = {"GET"};

    private static final String[] RFC2616_ENTITY_ENC_METHODS = {"POST", "PUT"};

    private static final String[] RFC2616_SPECIAL_METHODS = {"HEAD", "OPTIONS", "DELETE",
        "TRACE", "CONNECT"};

    public MyHttpRequestFactory()
    {
        super();
    }

    private static boolean isOneOf(final String[] methods, final String method)
    {
        for (int i = 0; i < methods.length; i++)
        {
            if (methods[i].equalsIgnoreCase(method))
            {
                return true;
            }
        }
        return false;
    }

    public HttpRequest newHttpRequest(final RequestLine requestline)
            throws MethodNotSupportedException
    {
        if (requestline == null)
        {
            throw new IllegalArgumentException("Request line may not be null");
        }
        String method = requestline.getMethod();
        System.out.println("in MyHTTPRequestFactory requestMethod=" + method);
        if (isOneOf(RFC2616_COMMON_METHODS, method))
        {
            //System.out.println("RFC2616_COMMON_METHODS,in MyHTTPRequestFactory create new BasicHttpRequest(requestline)");
            return new BasicHttpRequest(requestline);
        }
        else if (isOneOf(RFC2616_ENTITY_ENC_METHODS, method))
        {
            //System.out.println("RFC2616_ENTITY_ENC_METHODS,in MyHTTPRequestFactory create new BasicHttpEntityEnclosingRequest(requestline)");
            return new BasicHttpEntityEnclosingRequest(requestline);
        }
        else if (isOneOf(RFC2616_SPECIAL_METHODS, method))
        {
            //System.out.println("RFC2616_SPECIAL_METHODS,in MyHTTPRequestFactory create new BasicHttpRequest(requestline)");
            return new BasicHttpRequest(requestline);
        }
        else if ("200".equalsIgnoreCase(method))
        {
            //System.out.println("200 Revese HTTP, MyHTTPRequestFactory create new BasicHttpEntityEnclosingRequest(requestline)");
            return new BasicHttpRequest(requestline);
        }
        else
        {
            throw new MethodNotSupportedException(method + " method not supported");
        }
    }

    public HttpRequest newHttpRequest(final String method, final String uri)
            throws MethodNotSupportedException
    {
        if (isOneOf(RFC2616_COMMON_METHODS, method))
        {
            return new BasicHttpRequest(method, uri);
        }
        else if (isOneOf(RFC2616_ENTITY_ENC_METHODS, method))
        {
            return new BasicHttpEntityEnclosingRequest(method, uri);
        }
        else if (isOneOf(RFC2616_SPECIAL_METHODS, method))
        {
            return new BasicHttpRequest(method, uri);
        }
        else
        {
            throw new MethodNotSupportedException(method + " method not supported");
        }
    }

}
