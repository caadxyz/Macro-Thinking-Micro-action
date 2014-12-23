/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: AbstractDataDescriptorWriter.java 6836 2008-09-26 09:38:56Z dcollins $
 */
public abstract class AbstractDataDescriptorWriter implements DataDescriptorWriter
{
    private Object destination;

    public AbstractDataDescriptorWriter()
    {
    }

    public Object getDestination()
    {
        return this.destination;
    }

    public void setDestination(Object destination)
    {
        this.destination = destination;
    }

    public void write(DataDescriptor descriptor) throws java.io.IOException
    {
        if (descriptor == null)
        {
            String message = Logging.getMessage("nullValue.DataDescriptorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object dest = this.getDestination();
        if (dest == null)
        {
            String message = Logging.getMessage("nullValue.DestinationIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        java.io.Writer w = this.openWriter(dest);
        if (w == null)
        {
            String message = Logging.getMessage("generic.CannotOpenOutputStream", dest);
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        try
        {
            this.doWrite(w, descriptor);
        }
        finally
        {
            w.close();
        }
    }

    protected java.io.Writer openWriter(Object dest) throws java.io.IOException
    {
        java.io.Writer w = null;

        if (dest instanceof java.io.Writer)
            w = (java.io.Writer) dest;
        else if (dest instanceof java.io.OutputStream)
            w = new java.io.OutputStreamWriter((java.io.OutputStream) dest);
        else if (dest instanceof java.io.File)
            w = new java.io.FileWriter((java.io.File) dest);

        return w;
    }

    protected abstract void doWrite(java.io.Writer writer, DataDescriptor descriptor)
                                    throws java.io.IOException;
}
