/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.BufferWrapper;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: ByteBufferRaster.java 12553 2009-09-03 22:19:57Z garakl $
 */
public class ByteBufferRaster extends BufferWrapperRaster
{
    private java.nio.ByteBuffer byteBuffer;

    public ByteBufferRaster(int width, int height, Sector sector, java.nio.ByteBuffer byteBuffer, AVList params)
    {
        super(width, height, sector, BufferWrapper.wrap(byteBuffer, params));

        this.byteBuffer = byteBuffer;
        this.parseParameters( params );
    }

    private void parseParameters( AVList params )
    {
        if( null != params && params.hasKey( AVKey.MISSING_DATA_REPLACEMENT ))
        {
            Object o = params.getValue( AVKey.MISSING_DATA_REPLACEMENT );
            if( o instanceof Double )
                this.setTransparentValue( (Double)o );
        }
    }

    public ByteBufferRaster(int width, int height, Sector sector, AVList params)
    {
        this(width, height, sector, createCompatibleBuffer(width, height, params), params);
        this.parseParameters( params );
    }

    public static java.nio.ByteBuffer createCompatibleBuffer(int width, int height, AVList params)
    {
        if (width < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "width < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (height < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "height < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParamsIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object pixelType = params.getValue(AVKey.DATA_TYPE);

        int sizeOfDataType = 0;
        if (AVKey.INT8.equals(pixelType))
            sizeOfDataType = (Byte.SIZE / 8);
        else if (AVKey.INT16.equals(pixelType))
            sizeOfDataType = (Short.SIZE / 8);
        else if (AVKey.INT32.equals(pixelType))
            sizeOfDataType = (Integer.SIZE / 8);
        else if (AVKey.FLOAT32.equals(pixelType))
            sizeOfDataType = (Float.SIZE / 8);

        int sizeInBytes = sizeOfDataType * width * height;
        return java.nio.ByteBuffer.allocate(sizeInBytes);
    }

    public java.nio.ByteBuffer getByteBuffer()
    {
        return this.byteBuffer;
    }
}
