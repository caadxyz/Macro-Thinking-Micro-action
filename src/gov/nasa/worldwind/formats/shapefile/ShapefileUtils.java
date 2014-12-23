/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

/**
 * @author Patrick Murris
 * @version $Id: ShapefileUtils.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class ShapefileUtils
{
    public static double normalizeLongitude(double longitudeDegrees)
    {
        while (longitudeDegrees < -180)
            longitudeDegrees += 360;
        while (longitudeDegrees > 180)
            longitudeDegrees -= 360;

        return longitudeDegrees;
    }

    public static Rectangle2D normalizeRectangle(Rectangle2D rect)
    {
        if (rect.getMinX() >= -180 && rect.getMaxX() <= 180)
            return rect;

        rect.setRect(-180, rect.getY(), 360, rect.getHeight());
        return rect;
    }

    // *** I/O ***

    public static ByteBuffer readFileToBuffer(File file) throws IOException
    {
        ByteBuffer buffer = WWIO.readFileToBuffer(file, false); // Read file to a non direct ByteBuffer.
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Default to least significant byte first order.
        buffer.rewind();
        return buffer;
    }

    public static void skipBytes(InputStream is, int numBytes) throws IOException
    {
        int byteSkipped = 0;
        while (byteSkipped < numBytes)
            byteSkipped += is.skip(numBytes - byteSkipped);
    }

    public static ByteBuffer readByteChannelToBuffer(ReadableByteChannel channel, int numBytes) throws IOException
    {
        return readByteChannelToBuffer(channel, numBytes, null);
    }

    public static ByteBuffer readByteChannelToBuffer(ReadableByteChannel channel, int numBytes,
        ByteBuffer buffer) throws IOException
    {
        if (buffer == null)
            buffer = ByteBuffer.allocate(numBytes);

        int bytesRead = 0;
        int count = 0;
        while (count >= 0 && (numBytes - bytesRead) > 0)
        {
            count = channel.read(buffer);
            if (count > 0)
            {
                bytesRead += count;
            }
        }

        buffer.flip();
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Default to least significant byte first order.

        return buffer;
    }


    public static int[] readIntArray(ByteBuffer buffer, int numEntries)
    {
        int[] array = new int[numEntries];
        for (int i = 0; i < numEntries; i++)
            array[i] = buffer.getInt();

        return array;
    }

    public static double[] readDoubleArray(ByteBuffer buffer, int numEntries)
    {
        double[] array = new double[numEntries];
        for (int i = 0; i < numEntries; i++)
            array[i] = buffer.getDouble();

        return array;
    }

    public static void transferPoints(ByteBuffer from, VecBuffer to, int position, int numPoints)
    {
        DoubleBuffer doubleBuffer = from.slice().order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        doubleBuffer.limit(numPoints * 2);
        VecBuffer vecBuffer = new VecBuffer(2, new BufferWrapper.DoubleBufferWrapper(doubleBuffer));
        to.putSubBuffer(position, vecBuffer);
    }

    // *** Record selection ***

    /**
     * Selects shapefile records for which a given attribute match a given value.
     *
     * @param records the shapefile record list.
     * @param attributeName  the name of the attribute that should match the given value.
     * @param value the value the attribute should match.
     * @param acceptNullValue if true the filtering process will accept records which do not have the given attribute.
     * @return the filtered record list or <code>null</code> if the provided record list is empty.
     */
    public static List<ShapefileRecord> selectRecords(List<ShapefileRecord> records, String attributeName,
        Object value, boolean acceptNullValue)
    {
        Iterator<ShapefileRecord> iter = records.iterator();
        if (!iter.hasNext())
            return null;

        ShapefileRecord record;
        while (iter.hasNext())
        {
            record = iter.next();
            if (record == null)
                iter.remove();
            else
            {
                Object o = record.getAttributes().getValue(attributeName);
                if ( (o == null && !acceptNullValue)
                    || (o != null && o instanceof String && !((String)o).equalsIgnoreCase((String)value))
                    || (o != null && !o.equals(value)))

                    iter.remove();
            }
        }

        return records;
    }

    /**
     * Selects shapefile records that are contained in or intersect with a given {@link Sector}.
     *
     * @param records the shapefile record list.
     * @param sector  the geographic sector that records must be contained in or intersect with.
     * @return the filtered record list or <code>null</code> if the provided record list is empty.
     */
    public static List<ShapefileRecord> selectRecords(List<ShapefileRecord> records, Sector sector)
    {
        Iterator<ShapefileRecord> iter = records.iterator();
        if (!iter.hasNext())
            return null;

        Rectangle2D rectangle = sector.toRectangleDegrees();
        ShapefileRecord record;
        while (iter.hasNext())
        {
            record = iter.next();
            if (record == null)
                iter.remove();
            else
            {
                if (record instanceof ShapefileRecordPoint)
                {
                    double[] point = ((ShapefileRecordPoint)record).getPoint();
                    if (!rectangle.contains(point[0], point[1]))
                        iter.remove();
                }
                else if(record instanceof ShapefileRecordPolyline)  // catches polygons too
                {
                    if (!rectangle.intersects(((ShapefileRecordPolyline)record).getBoundingRectangle()))
                        iter.remove();
                }
            }
        }

        return records;
    }

    /**
     * Returns a new {@link CompoundVecBuffer} that only contains the selected records sub-buffers or parts.
     * Records are selected when the specified attribute has the given value. Note that the original backing
     * {@link VecBuffer} is not duplicated in the process.
     * <p>
     * String values are not case sensitive while attributes names are.
     *
     * @param shapeFile the shapefile to select records from.
     * @param attributeName the name of the attribute which value is to be compared to the given value.
     * @param value the value to compare the attribute with.
     * @return a new {@link CompoundVecBuffer} that only contains the selected records sub-buffers or parts.
     */
    public static CompoundVecBuffer createBufferFromAttributeValue(Shapefile shapeFile, String attributeName,
        Object value)
    {
        List<ShapefileRecord> records = new ArrayList<ShapefileRecord>(shapeFile.getRecords());
        // Filter record list
        ShapefileUtils.selectRecords(records, attributeName, value, false);

        return ShapefileUtils.createBufferFromRecords(records);
    }

    /**
     * Returns a new {@link CompoundVecBuffer} that only contains the given records sub-buffers or parts.
     * Note that the original backing {@link VecBuffer} is not duplicated in the process.
     *
     * @param records a list of records to include in the buffer.
     * @return a new {@link CompoundVecBuffer} that only contains the given records sub-buffers or parts.
     */
    public static CompoundVecBuffer createBufferFromRecords(List<ShapefileRecord> records)
    {
        if (records.size() == 0)
            return null;

        // Count parts
        int numParts = 0;
        for (ShapefileRecord record : records)
            numParts += record.getNumberOfParts();

        // Get source geometry buffer
        CompoundVecBuffer sourceBuffer = records.get(0).getShapeFile().getBuffer();
        
        // Create new offset and length buffers
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
        for (ShapefileRecord record : records)
        {
            for (int part = 0; part < record.getNumberOfParts(); part++)
            {
                offsetBuffer.put(sourceBuffer.getSubPositionBuffer().get(record.getFirstPartNumber() + part));
                lengthBuffer.put(sourceBuffer.getSubLengthBuffer().get(record.getFirstPartNumber() + part));
            }
        }
        offsetBuffer.rewind();
        lengthBuffer.rewind();

        return new CompoundVecBuffer(sourceBuffer.getBackingBuffer(), offsetBuffer, lengthBuffer, numParts,
            sourceBuffer.getBufferFactory());
    }


}
