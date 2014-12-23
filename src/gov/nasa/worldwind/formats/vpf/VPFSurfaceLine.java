/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.CompoundVecBuffer;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * @author dcollins
 * @version $Id: VPFSurfaceLine.java 12798 2009-11-17 20:14:14Z dcollins $
 */
public class VPFSurfaceLine extends SurfacePolyline
{
    protected Sector sector;
    protected CompoundVecBuffer buffer;

    public VPFSurfaceLine(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        this.sector = feature.getBounds().toSector();
        this.buffer = computeLineFeatureCoords(feature, primitiveData);
    }

    protected static CompoundVecBuffer computeLineFeatureCoords(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();
        int[] primitiveIds = feature.getPrimitiveIds();

        int numLines = primitiveIds.length;
        IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numLines);
        IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numLines);

        CompoundVecBuffer buffer = primitiveData.getPrimitiveCoords(primitiveName);
        for (int id : primitiveIds)
        {
            offsetBuffer.put(buffer.getSubPositionBuffer().get(id));
            lengthBuffer.put(buffer.getSubLengthBuffer().get(id));
        }

        offsetBuffer.rewind();
        lengthBuffer.rewind();

        return new CompoundVecBuffer(buffer.getBackingBuffer(), offsetBuffer, lengthBuffer, numLines, null);
    }

    protected Iterable<? extends Sector> doGetSectors(DrawContext dc, double texelSizeRadians)
    {
        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            Arrays.asList(this.sector), this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.buffer.getLocations();
        
    }

    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        throw new UnsupportedOperationException();
    }

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void drawOutline(DrawContext dc, int drawMode)
    {
        this.buffer.bindAsVertexBuffer(dc);
        this.buffer.multiDrawArrays(dc, drawMode);
    }
}
