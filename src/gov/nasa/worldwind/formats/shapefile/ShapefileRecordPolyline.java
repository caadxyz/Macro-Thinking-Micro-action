/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.awt.geom.*;
import java.nio.*;
import java.util.List;

/**
 * @author Patrick Murris
 * @version $Id: ShapefileRecordPolyline.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class ShapefileRecordPolyline extends ShapefileRecord
{
    protected Rectangle2D boundingRectangle;

    public Rectangle2D getBoundingRectangle()
    {
        return this.boundingRectangle;
    }

    /**
     * Get all the points X and Y coordinates for the given part of this record. Part numbers start at zero.
     *
     * @param partNumber the number of the part of this record - zero based.
     * @return an {@link Iterable} over the points X and Y coordinates.
     */
    public Iterable<double[]> getPoints(int partNumber)
    {
        return this.getBuffer(partNumber).getCoords();
    }

    protected void normalizeLocations()
    {
        if (this.boundingRectangle.getX() >= -180 && this.boundingRectangle.getMaxX() <= 180)
            return;
        
        super.normalizeLocations();
        ShapefileUtils.normalizeRectangle(this.boundingRectangle);
    }

    /**
     * Creates a new {@link ShapefileRecordPolyline} instance from the given {@link java.nio.ByteBuffer}.
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
     * @return a new {@link ShapefileRecordPolyline} instance.
     */
    protected static ShapefileRecordPolyline fromBuffer(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        ShapefileRecordPolyline record = new ShapefileRecordPolyline();
        record.readFromBuffer(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
        return record;
    }

    protected void readFromBuffer(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        // Read record number and skip record length, big endian
        buffer.order(ByteOrder.BIG_ENDIAN);
        this.recordNumber = buffer.getInt();
        this.lengthInBytes = buffer.getInt() * 2;

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
            return;

        if (!shapeType.equals(Shapefile.SHAPE_POLYLINE) && !shapeType.equals(Shapefile.SHAPE_POLYGON))
        {
            String message = Logging.getMessage("SHP.UnexpectedRecordShapeType", shapeType);
            Logging.logger().log(java.util.logging.Level.SEVERE, message);
            throw new WWRuntimeException(message);
        }

        this.shapeType = shapeType;
        this.shapeFile = shapeFile;

        // Bounding rectangle
        double[] bounds = ShapefileUtils.readDoubleArray(buffer, 4);
        this.boundingRectangle = new Rectangle2D.Double(bounds[0], bounds[1],
            bounds[2] - bounds[0], bounds[3] - bounds[1]);

        // Parts and points
        this.firstPartNumber = partsOffset.size();

        this.numberOfParts = buffer.getInt();
        int numPoints = buffer.getInt();
        int[] parts = ShapefileUtils.readIntArray(buffer, this.numberOfParts);

        // Update parts offset and length lists
        int pointBufferPosition = partsOffset.size() > 0 ?
            partsOffset.get(partsOffset.size() - 1) + partsLength.get(partsLength.size() - 1) : 0;
        for (int i = 0; i < this.numberOfParts; i++)
        {
            int partOffset = parts[i];
            int partLength = (i == this.numberOfParts - 1) ? numPoints - partOffset : parts[i + 1] - partOffset;
            partsOffset.add(pointBufferPosition + partOffset);
            partsLength.add(partLength);
        }

        // Put points in point buffer
        ShapefileUtils.transferPoints(buffer, pointBuffer, pointBufferPosition, numPoints);
    }

}