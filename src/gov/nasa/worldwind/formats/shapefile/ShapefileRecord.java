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
 * @version $Id: ShapefileRecord.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class ShapefileRecord
{
    protected Shapefile shapeFile;
    protected int recordNumber;
    protected String shapeType;
    protected DBaseRecord attributes;

    protected int numberOfParts;
    protected int firstPartNumber;

    protected int lengthInBytes;

    public Shapefile getShapeFile()
    {
        return this.shapeFile;
    }

    public int getRecordNumber()
    {
        return this.recordNumber;
    }

    public String getShapeType()
    {
        return this.shapeType;
    }

    public DBaseRecord getAttributes()
    {
        return this.attributes;
    }

    public int getNumberOfParts()
    {
        return this.numberOfParts;
    }

    public int getFirstPartNumber()
    {
        return this.firstPartNumber;
    }

    public int getNumberOfPoints()
    {
        int numPoints = 0;
        for (int part = 0; part < this.getNumberOfParts(); part++)
            numPoints += this.getNumberOfPoints(part);
        return numPoints;
    }

    public int getNumberOfPoints(int partNumber)
    {
        return this.getBuffer(partNumber).getSize();
    }

    public VecBuffer getBuffer(int partNumber)
    {
        if (partNumber < 0 || partNumber >= this.getNumberOfParts())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", partNumber);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getShapeFile().getBuffer().getSubBuffer(this.getFirstPartNumber() + partNumber);
    }

    protected void normalizeLocations()
    {
        double[] point = new double[2];
        for (int part = 0; part < this.getNumberOfParts(); part++)
        {
            VecBuffer buffer = this.getBuffer(part);
            for (int i = 0; i < buffer.getSize(); i++)
            {
                buffer.get(i, point);
                point[0] = ShapefileUtils.normalizeLongitude(point[0]);
                buffer.put(i, point);
            }
        }
    }

    /**
     * Creates a new {@link ShapefileRecord} instance from the given {@link java.nio.ByteBuffer}.
     * Returns <code>null</code> if the record shape type is {@link Shapefile#SHAPE_NULL}.
     * <p>
     * The buffer current position is assumed to be set at the start of the record and will be set to
     * the start of the next record after this method has completed.
     *
     * @param shapeFile the parent {@link Shapefile}.
     * @param recordBuffer the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer the {@link VecBuffer} into which points are to be added.
     * @param partsOffset the current list of parts offset for preceding records.
     * @param partsLength the current list of parts length for preceding records.
     * @return a new {@link ShapefileRecord} instance.
     */
    protected static ShapefileRecord fromBuffer(Shapefile shapeFile, ByteBuffer recordBuffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        // Read shape type - little endian
        recordBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int type = recordBuffer.getInt(recordBuffer.position() + 2 * 4); // skip record number and length as ints
        String shapeType = Shapefile.getShapeType(type);
        if (shapeType == null)
        {
            String message = Logging.getMessage("SHP.UnsupportedShapeType", type);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        // Select proper record class
        if (shapeType.equals(Shapefile.SHAPE_POINT))
        {
            return ShapefileRecordPoint.fromBuffer(shapeFile, recordBuffer, pointBuffer, partsOffset,
                partsLength);
        }
        else if (shapeType.equals(Shapefile.SHAPE_POLYLINE))
        {
            return ShapefileRecordPolyline.fromBuffer(shapeFile, recordBuffer, pointBuffer, partsOffset,
                partsLength);
        }
        else if (shapeType.equals(Shapefile.SHAPE_POLYGON))
        {
            return ShapefileRecordPolygon.fromBuffer(shapeFile, recordBuffer, pointBuffer, partsOffset,
                partsLength);
        }

        return null;
    }
}