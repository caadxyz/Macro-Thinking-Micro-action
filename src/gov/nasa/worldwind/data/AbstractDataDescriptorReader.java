/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractDataDescriptorReader.java 12567 2009-09-07 22:36:12Z tgaskins $
 */
public abstract class AbstractDataDescriptorReader implements DataDescriptorReader
{
    private Object source;

    public AbstractDataDescriptorReader()
    {
    }

    public Object getSource()
    {
        return this.source;
    }

    public void setSource(Object source)
    {
        this.source = source;
    }

    public boolean canRead() throws java.io.IOException
    {
        Object src = this.getSource();
        //noinspection SimplifiableIfStatement
        if (src == null)
            return false;

        return this.doCanRead(src);
    }

    public DataDescriptor read() throws java.io.IOException
    {
        Object src = this.getSource();
        if (src == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        java.io.Reader r = WWIO.openReader(src);
        if (r == null)
        {
            String message = Logging.getMessage("generic.CannotOpenInputStream", src);
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        DataDescriptor descriptor = null;
        try
        {
            descriptor = this.doRead(r);
        }
        finally
        {
            r.close();
        }

        return descriptor;
    }

    protected boolean doCanRead(Object src) throws java.io.IOException
    {
        java.io.Reader r = WWIO.openReader(src);
        if (r == null)
            return false;

        boolean canRead = false;
        try
        {
            canRead = this.doCanReadStream(r);
        }
        finally
        {
            r.close();
        }

        return canRead;
    }

    protected abstract boolean doCanReadStream(java.io.Reader reader) throws java.io.IOException;

    protected abstract DataDescriptor doRead(java.io.Reader reader) throws java.io.IOException;
}
