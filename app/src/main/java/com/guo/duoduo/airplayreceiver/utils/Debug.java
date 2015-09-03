/******************************************************************
 *
 * CyberUtil for Java
 *
 * Copyright (C) Satoshi Konno 2002
 *
 * File: Debug.java
 *
 * Revision;
 *
 * 11/18/02 - first revision.
 *
 ******************************************************************/

package com.guo.duoduo.airplayreceiver.utils;


import android.util.Log;

import java.io.PrintStream;


public final class Debug
{

    public static Debug debug = new Debug();
    public static boolean enabled = true;
    private PrintStream out = System.out;

    public Debug()
    {

    }

    public static Debug getDebug()
    {
        return Debug.debug;
    }

    public static final void on()
    {
        enabled = true;
    }

    public static final void off()
    {
        enabled = false;
    }

    public static boolean isOn()
    {
        return enabled;
    }

    public static final void message(String s)
    {
        if (enabled == true)
            Debug.debug.getOut().println("CyberGarage message : " + s);
    }

    public static final void message(String m1, String m2)
    {
        if (enabled == true)
            Debug.debug.getOut().println("CyberGarage message : ");
        Debug.debug.getOut().println(m1);
        Debug.debug.getOut().println(m2);
    }

    public static final void warning(String s)
    {
        Debug.debug.getOut().println("CyberGarage warning : " + s);
    }

    public static final void warning(String m, Exception e)
    {
        if (e.getMessage() == null)
        {
            Debug.debug.getOut().println("CyberGarage warning : " + m + " START");
            e.printStackTrace(Debug.debug.getOut());
            Debug.debug.getOut().println("CyberGarage warning : " + m + " END");
        }
        else
        {
            Debug.debug.getOut().println(
                "CyberGarage warning : " + m + " (" + e.getMessage() + ")");
            e.printStackTrace(Debug.debug.getOut());
        }
    }

    public static final void warning(Exception e)
    {
        warning(e.getMessage());
        e.printStackTrace(Debug.debug.getOut());
    }

    public synchronized PrintStream getOut()
    {
        return out;
    }

    public synchronized void setOut(PrintStream out)
    {
        this.out = out;
    }

    /**
     * 使用LogCat打印日志
     * @param tag
     * @param message
     */
    public static final void d(String tag, String message)
    {
        if(enabled == true)
        {
            Log.d(tag, message);
        }
    }
}
