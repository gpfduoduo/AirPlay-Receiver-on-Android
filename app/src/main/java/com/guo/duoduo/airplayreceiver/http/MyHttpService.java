package com.guo.duoduo.airplayreceiver.http;


import java.io.IOException;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolException;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.util.EncodingUtils;
import org.apache.http.util.EntityUtils;


/**
 * Created by guo.duoduo on 2015/8/24.
 */
public class MyHttpService
{

    private HttpParams params = null;
    private HttpProcessor processor = null;
    private HttpRequestHandlerResolver handlerResolver = null;
    private ConnectionReuseStrategy connStrategy;
    private HttpResponseFactory responseFactory = null;
    private HttpExpectationVerifier expectationVerifier = null;

    public MyHttpService(final HttpProcessor proc,
            final ConnectionReuseStrategy connStrategy,
            final HttpResponseFactory responseFactory)
    {
        super();
        setHttpProcessor(proc);
        setConnReuseStrategy(connStrategy);
        setResponseFactory(responseFactory);
    }

    public void setHttpProcessor(final HttpProcessor processor)
    {
        if (processor == null)
        {
            throw new IllegalArgumentException("HTTP processor may not be null.");
        }
        this.processor = processor;
    }

    public void setConnReuseStrategy(final ConnectionReuseStrategy connStrategy)
    {
        if (connStrategy == null)
        {
            throw new IllegalArgumentException(
                "Connection reuse strategy may not be null");
        }
        this.connStrategy = connStrategy;
    }

    public void setResponseFactory(final HttpResponseFactory responseFactory)
    {
        if (responseFactory == null)
        {
            throw new IllegalArgumentException("Response factory may not be null");
        }
        this.responseFactory = responseFactory;
    }

    public void setHandlerResolver(final HttpRequestHandlerResolver handlerResolver)
    {
        this.handlerResolver = handlerResolver;
    }

    public void setExpectationVerifier(final HttpExpectationVerifier expectationVerifier)
    {
        this.expectationVerifier = expectationVerifier;
    }

    public HttpParams getParams()
    {
        return this.params;
    }

    public void setParams(final HttpParams params)
    {
        this.params = params;
    }

    public void handleRequest(final HttpServerConnection conn, final HttpContext context)
            throws IOException, HttpException
    {
        context.setAttribute("http.connection", conn);
        HttpResponse response = null;

        try
        {
            HttpRequest ex = conn.receiveRequestHeader();



            if (ex instanceof HttpEntityEnclosingRequest)
            {
                if (((HttpEntityEnclosingRequest) ex).expectContinue())
                {
                    response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_1,
                        100, context);
                    if (this.expectationVerifier != null)
                    {
                        try
                        {
                            this.expectationVerifier.verify(ex, response, context);
                        }
                        catch (HttpException var6)
                        {
                            response = this.responseFactory.newHttpResponse(
                                HttpVersion.HTTP_1_0, 500, context);
                            this.handleException(var6, response);
                        }
                    }

                    if (response.getStatusLine().getStatusCode() < 200)
                    {
                        conn.sendResponseHeader(response);
                        conn.flush();
                        response = null;
                        conn.receiveRequestEntity((HttpEntityEnclosingRequest) ex);
                    }
                }
                else
                {
                    conn.receiveRequestEntity((HttpEntityEnclosingRequest) ex);
                }
            }

            context.setAttribute("http.request", ex);
            if (response == null)
            {
                response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_1,
                    200, context);
                this.processor.process(ex, context);
                this.doService(ex, response, context);
            }

            if (ex instanceof HttpEntityEnclosingRequest)
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) ex).getEntity();
                EntityUtils.consume(entity);
            }
        }
        catch (HttpException var7)
        {
            response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0, 500,
                context);
            this.handleException(var7, response);
        }

        context.setAttribute("http.response", response);
        this.processor.process(response, context);
        conn.sendResponseHeader(response);
        conn.sendResponseEntity(response);
        conn.flush();
        if (!this.connStrategy.keepAlive(response, context))
        {
            conn.close();
        }
    }

    protected void handleException(HttpException ex, HttpResponse response)
    {
        if (ex instanceof MethodNotSupportedException)
        {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
        else if (ex instanceof UnsupportedHttpVersionException)
        {
            response.setStatusCode(HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED);
        }
        else if (ex instanceof ProtocolException)
        {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }
        else
        {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        String message = ex.getMessage();
        if (message == null)
        {
            message = ex.toString();
        }

        byte[] msg = EncodingUtils.getAsciiBytes(message);
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
    }

    protected void doService(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException
    {
        HttpRequestHandler handler = null;
        if (this.handlerResolver != null)
        {
            String requestURI = request.getRequestLine().getUri();
            handler = this.handlerResolver.lookup(requestURI);
        }

        if (handler != null)
        {
            handler.handle(request, response, context);
        }
        else
        {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }

    }
}
