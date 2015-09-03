package com.guo.duoduo.airplayreceiver.httpcore;


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
import org.apache.http.ProtocolVersion;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerResolver;
import org.apache.http.util.EncodingUtils;

import android.util.Log;


public class MyHttpService
{
    private static final String tag = MyHttpService.class.getSimpleName();

    private HttpParams params = null;
    private HttpProcessor processor = null;
    private HttpRequestHandlerResolver handlerResolver = null;
    private ConnectionReuseStrategy connStrategy = null;
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
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        HttpResponse response = null;

        try
        {
            HttpRequest request = conn.receiveRequestHeader();
            request.setParams(new DefaultedHttpParams(request.getParams(), this.params));

            String method = request.getRequestLine().getMethod();
            Log.d(tag, "airplay in HTTpService, method = " + method);
            if ("200".equals(method))
            {
                Log.d(
                    tag,
                    "airplay in HTTPService, Receive iOS HTTP reverse response 200 OK, do nothing just return");
                return;
            }
            if (context.getAttribute("NO-RESP") != null)
            {
                Log.d(tag,
                    "airplay in HTTPService, get /playback-info response this time!!");
                context.removeAttribute("NO-RESP");
                return;
            }

            ProtocolVersion ver = request.getRequestLine().getProtocolVersion();
            if (!ver.lessEquals(HttpVersion.HTTP_1_1))
            {
                ver = HttpVersion.HTTP_1_1;
            }

            if (request instanceof HttpEntityEnclosingRequest)
            {
                if (((HttpEntityEnclosingRequest) request).expectContinue())
                {
                    response = this.responseFactory.newHttpResponse(ver,
                        HttpStatus.SC_CONTINUE, context);
                    response.setParams(new DefaultedHttpParams(response.getParams(),
                        this.params));

                    if (this.expectationVerifier != null)
                    {
                        try
                        {
                            this.expectationVerifier.verify(request, response, context);
                        }
                        catch (HttpException ex)
                        {
                            response = this.responseFactory.newHttpResponse(
                                HttpVersion.HTTP_1_0,
                                HttpStatus.SC_INTERNAL_SERVER_ERROR, context);
                            response.setParams(new DefaultedHttpParams(response
                                    .getParams(), this.params));
                            handleException(ex, response);
                        }
                    }
                    if (response.getStatusLine().getStatusCode() < 200)
                    {
                        conn.sendResponseHeader(response);
                        conn.flush();
                        response = null;
                        conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                    }
                }
                else
                {
                    conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                }
            }

            if (response == null)
            {
                response = this.responseFactory.newHttpResponse(ver, HttpStatus.SC_OK,
                    context);
                response.setParams(new DefaultedHttpParams(response.getParams(),
                    this.params));

                context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
                context.setAttribute(ExecutionContext.HTTP_RESPONSE, response);

                this.processor.process(request, context);
                doService(request, response, context);
            }

            // Make sure the request content is fully consumed
            if (request instanceof HttpEntityEnclosingRequest)
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                if (entity != null)
                {
                    entity.consumeContent();
                }
            }

        }
        catch (HttpException ex)
        {
            response = this.responseFactory.newHttpResponse(HttpVersion.HTTP_1_0,
                HttpStatus.SC_INTERNAL_SERVER_ERROR, context);
            response.setParams(new DefaultedHttpParams(response.getParams(), this.params));
            handleException(ex, response);
        }

        this.processor.process(response, context);
        conn.sendResponseHeader(response);
        conn.sendResponseEntity(response);
        conn.flush();

        if (!this.connStrategy.keepAlive(response, context))
        {
            conn.close();
        }
    }

    protected void handleException(final HttpException ex, final HttpResponse response)
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
        byte[] msg = EncodingUtils.getAsciiBytes(ex.getMessage());
        ByteArrayEntity entity = new ByteArrayEntity(msg);
        entity.setContentType("text/plain; charset=US-ASCII");
        response.setEntity(entity);
    }

    protected void doService(final HttpRequest request, final HttpResponse response,
            final HttpContext context) throws HttpException, IOException
    {
        Log.d(tag, "airplay in HttpService doService");
        HttpRequestHandler handler = null;
        if (this.handlerResolver != null)
        {
            String requestURI = request.getRequestLine().getUri();

            Log.d(tag, "airplay in http service, requestUri=" + requestURI);

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