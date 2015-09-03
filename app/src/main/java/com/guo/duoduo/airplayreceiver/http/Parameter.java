/******************************************************************
 *
 * CyberHTTP for Java
 *
 * Copyright (C) Satoshi Konno 2002-2004
 *
 * File: Parameter.java
 *
 * Revision;
 *
 * 02/01/04 - first revision.
 *
 ******************************************************************/

package com.guo.duoduo.airplayreceiver.http;


public class Parameter
{
    private String name = new String();
    private String value = new String();

    public Parameter()
    {
    }

    public Parameter(String name, String value)
    {
        setName(name);
        setValue(value);
    }

    ////////////////////////////////////////////////
    //	name
    ////////////////////////////////////////////////

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    ////////////////////////////////////////////////
    //	value
    ////////////////////////////////////////////////

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}
