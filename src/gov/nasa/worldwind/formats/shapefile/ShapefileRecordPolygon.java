/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.shapefile;

import gov.nasa.worldwind.util.VecBuffer;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author Patrick Murris
 * @version $Id: ShapefileRecordPolygon.java 12796 2009-11-14 23:37:46Z patrickmurris $
 */
public class ShapefileRecordPolygon extends ShapefileRecordPolyline
{
    /**
     * Creates a new {@link ShapefileRecordPolygon} instance from the given {@link java.nio.ByteBuffer}.
     * Returns <code>null</code> if the record shape type is {@link Shapefile#SHAPE_NULL}.
     * <p>
     * The buffer current position is assumed to be set at the start of the record and will be set to
     * the start of the next record after this method has completed.
     *
     * @param shapeFile the parent {@link Shapefile}.
     * @param buffer the shapefile record {@link java.nio.ByteBuffer} to read from.
     * @param pointBuffer the {@link gov.nasa.worldwind.util.VecBuffer} into which points are to be added.
     * @param partsOffset the current list of parts offset for preceding records.
     * @param partsLength the current list of parts length for preceding records.
     * @return a new {@link ShapefileRecordPolygon} instance.
     */
    protected static ShapefileRecordPolyline fromBuffer(Shapefile shapeFile, ByteBuffer buffer, VecBuffer pointBuffer,
        List<Integer> partsOffset, List<Integer> partsLength)
    {
        ShapefileRecordPolygon record = new ShapefileRecordPolygon();
        record.readFromBuffer(shapeFile, buffer, pointBuffer, partsOffset, partsLength);
        return record;
    }

}
