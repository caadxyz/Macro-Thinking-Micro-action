/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.nio.*;
import java.util.List;

/**
 * @author Patrick Murris
 * @version $Id: ShapefileRecordPoint.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class ShapefileRecordPoint extends ShapefileRecord
{
    /**
     * Get the point X and Y coordinates.
     *
     * @return the point X and Y coordinates.
     */
    public double[] getPoint()
    {
        VecBuffer pointBuffer = this.getShapeFile().getBuffer().getSubBuffer(this.getFirstPartNumber());
        Iterable<double[]> iterable = pointBuffer.getCoords();
        return iterable.iterator().next();
    }

    /**
     * Creates a new {@link ShapefileRecordPoint} instance from the given {@link java.nio.ByteBuffer}.
     * Returns <code>null</code> if the record shape type is {@link Shapefile#SHAPE_NULL}.
     * <p>
     * The buffer current position is assumed to be set at the start of the record and will be set to
     * the start of the next record after this method has completed.
     *
     * @param shapeFile the parent {@link Shapefile}.
     * @param buffer the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer the {@link VecBuffer} into which points are to be added.
     * @param partsOffset the current list of parts offset for preceding records.
     * @param partsLength the current list of parts length for preceding records.
     * @return a new {@link ShapefileRecordPoint} instance.
     */
    protected static ShapefileRecordPoint fromBuffer(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        ShapefileRecordPoint record = new ShapefileRecordPoint();
        int pointBufferPosition = partsOffset.size() > 0 ?
            partsOffset.get(partsOffset.size() - 1) + partsLength.get(partsLength.size() - 1) : 0;

        // Read record number and skip record length, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        record.recordNumber = buffer.getInt();
        record.lengthInBytes = buffer.getInt() * 2;

        // Read shape type - little endian
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = buffer.getInt();
        String shapeType = Shapefile.getShapeType(type);
        if (shapeType == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        if (shapeType.equals(Shapefile.SHAPE_NULL))
            return null;

        if (!shapeType.equals(Shapefile.SHAPE_POINT))
        {
            String message = Logging.getMessage("SHP.UnexpectedRecordShapeType", shapeType);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Set record state
        record.shapeType = shapeType;
        record.shapeFile = shapeFile;
        record.firstPartNumber = partsOffset.size();
        record.numberOfParts = 1;

        // Read point X and Y and add to point buffer
        ShapefileUtils.transferPoints(buffer, pointBuffer, pointBufferPosition, 1);

        // Update parts offset and length lists with one part of one point
        partsOffset.add(pointBufferPosition);
        partsLength.add(1);

        return record;
    }

}