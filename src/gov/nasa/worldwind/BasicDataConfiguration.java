/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

/**
 * An implementation of the {@link DataConfiguration} interface, which uses an {@link org.w3c.dom.Element} as its
 * backing store. Parameter names in query methods are interpreted as {@link XPath} queries against the backing
 * Element.
 *
 * @author dcollins
 * @version $Id: BasicDataConfiguration.java 11666 2009-06-16 15:49:14Z dcollins $
 */
public class BasicDataConfiguration implements DataConfiguration
{
    protected Element dom;
    protected XPath xpath;

    /**
     * Creates a BasicDataConfiguration backed by a specified Element.
     *
     * @param domElement backing DOM element.
     *
     * @throws IllegalArgumentException if the element is null.
     */
    public BasicDataConfiguration(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.dom = domElement;
    }

    /**
     * A no-op implemenation of the {@link DataConfiguration#getName()} method provided so that this class can be
     * instantiated. Not all configuration info supports a <code>getName</code> method; this class is therefore
     * useful as is in these cases.
     *
     * @return this no-op method always returns null.
     */
    public String getName()
    {
        return null;
    }

    /**
     * A no-op implemenation of the {@link DataConfiguration#getType()} method provided so that this class can be
     * instantiated. Not all configuration info supports a <code>getType</code> method; this class is therefore
     * useful as is in these cases.
     *
     * @return this no-op method always returns null.
     */
    public String getType()
    {
        return null;
    }

    /**
     * A no-op implemenation of the {@link DataConfiguration#getVersion()} method provided so that this class can be
     * instantiated. Not all configuration info supports a <code>getVersion</code> method; this class is therefore
     * useful as is in these cases.
     *
     * @return this no-op method always returns null.
     */
    public String getVersion()
    {
        return null;
    }

    /**
     * Returns this data configuration's backing DOM Element.
     *
     * @return the backing DOM element.
     */
    public Object getSource()
    {
        return this.dom;
    }

    public DataConfiguration getChild(String paramName)
    {
        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Element el = WWXML.getElement(this.dom, paramName, this.getXPath());
        if (el == null)
            return null;

        return this.createChildConfigInfo(el);
    }

    public DataConfiguration[] getChildren(String paramName)
    {
        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Element[] els = WWXML.getElements(this.dom, paramName, this.getXPath());
        if (els == null)
            return null;

        DataConfiguration[] cis = new DataConfiguration[els.length];

        for (int i = 0; i < els.length; i++)
        {
            cis[i] = this.createChildConfigInfo(els[i]);
        }

        return cis;
    }

    public String getString(String paramName)
    {
        return WWXML.getText(this.dom, paramName, this.getXPath());
    }

    public String[] getStringArray(String paramName)
    {
        return WWXML.getTextArray(this.dom, paramName, this.getXPath());
    }

    public String[] getUniqueStrings(String paramName)
    {
        return WWXML.getUniqueText(this.dom, paramName, this.getXPath());
    }

    public Integer getInteger(String paramName)
    {
        return WWXML.getInteger(this.dom, paramName, this.getXPath());
    }

    public Long getLong(String paramName)
    {
        return WWXML.getLong(this.dom, paramName, this.getXPath());
    }

    public Double getDouble(String paramName)
    {
        return WWXML.getDouble(this.dom, paramName, this.getXPath());
    }

    public Boolean getBoolean(String paramName)
    {
        return WWXML.getBoolean(this.dom, paramName, this.getXPath());
    }

    public LatLon getLatLon(String paramName)
    {
        return WWXML.getLatLon(this.dom, paramName, this.getXPath());
    }

    public Sector getSector(String paramName)
    {
        return WWXML.getSector(this.dom, paramName, this.getXPath());
    }

    public LevelSet.SectorResolution getSectorResolutionLimit(String paramName)
    {
        return WWXML.getSectorResolutionLimit(this.dom, paramName, this.getXPath());
    }

    public Long getTimeInMillis(String paramName)
    {
        return WWXML.getTimeInMillis(this.dom, paramName, this.getXPath());
    }

    protected DataConfiguration createChildConfigInfo(Element domElement)
    {
        return new BasicDataConfiguration(domElement);
    }

    protected XPath getXPath()
    {
        if (this.xpath == null)
        {
            this.xpath = WWXML.makeXPath();
        }

        return this.xpath;
    }
}
