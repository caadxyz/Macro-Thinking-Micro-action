/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: BufferWrapperRaster.java 13002 2010-01-12 20:17:23Z dcollins $
 */
public class BufferWrapperRaster extends BufferedDataRaster implements Cacheable, Disposable
{
    private BufferWrapper buffer;

    public BufferWrapperRaster(int width, int height, Sector sector, BufferWrapper buffer)
    {
        super(width, height, sector);

        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int expectedValues = width * height;
        if (buffer.length() < expectedValues)
        {
            String message = Logging.getMessage("generic.BufferSize", "buffer.length() < " + expectedValues);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer = buffer;
    }
    
    public BufferWrapper getBuffer()
    {
        return this.buffer;
    }

    public long getSizeInBytes()
    {
        return this.buffer.getSizeInBytes();
    }

    public void dispose()
    {
    }

    protected void get(int x, int y, int length, double[] buffer, int pos)
    {
        int index = this.indexFor(x, y);
        this.getBuffer().getDouble(index, buffer, pos, length);
    }

    protected void put(int x, int y, double[] buffer, int pos, int length)
    {
        int index = this.indexFor(x, y);
        this.getBuffer().putDouble(index, buffer, pos, length);
    }

    protected final int indexFor(int x, int y)
    {
        // Map raster coordinates to buffer coordinates.
        return x + y * this.getWidth();
    }
}
