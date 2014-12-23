/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.vpf;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * @author dcollins
 * @version $Id: VPFSurfaceArea.java 12798 2009-11-17 20:14:14Z dcollins $
 */
public class VPFSurfaceArea extends SurfacePolygon implements Disposable
{
    protected VPFFeature feature;
    protected VPFPrimitiveData primitiveData;
    protected CompoundVecBuffer buffer;
    protected int interiorDisplayList;

    public VPFSurfaceArea(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        this.feature = feature;
        this.primitiveData = primitiveData;
        this.buffer = computeAreaFeatureCoords(feature, primitiveData);
    }

    protected static CompoundVecBuffer computeAreaFeatureCoords(VPFFeature feature, VPFPrimitiveData primitiveData)
    {
        final int numEdges = traverseAreaEdges(feature, primitiveData, null);
        final IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numEdges);
        final IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numEdges);
        final CompoundVecBuffer buffer = primitiveData.getPrimitiveCoords(VPFConstants.EDGE_PRIMITIVE_TABLE);

        traverseAreaEdges(feature, primitiveData, new EdgeListener()
        {
            public void nextEdge(int edgeId, VPFPrimitiveData.EdgeInfo edgeInfo)
            {
                offsetBuffer.put(buffer.getSubPositionBuffer().get(edgeId));
                lengthBuffer.put(buffer.getSubLengthBuffer().get(edgeId));
            }
        });

        offsetBuffer.rewind();
        lengthBuffer.rewind();

        return new CompoundVecBuffer(buffer.getBackingBuffer(), offsetBuffer, lengthBuffer, numEdges, null);
    }

    protected interface EdgeListener
    {
        void nextEdge(int edgeId, VPFPrimitiveData.EdgeInfo edgeInfo);
    }

    protected static int traverseAreaEdges(VPFFeature feature, VPFPrimitiveData primitiveData, EdgeListener listener)
    {
        int count = 0;

        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();

        for (int id : feature.getPrimitiveIds())
        {
            VPFPrimitiveData.FaceInfo faceInfo = (VPFPrimitiveData.FaceInfo) primitiveData.getPrimitiveInfo(
                primitiveName, id);

            VPFPrimitiveData.Ring outerRing = faceInfo.getOuterRing();
            count += traverseRingEdges(outerRing, primitiveData, listener);

            for (VPFPrimitiveData.Ring ring : faceInfo.getInnerRings())
            {
                count += traverseRingEdges(ring, primitiveData, listener);
            }
        }

        return count;
    }

    protected static int traverseRingEdges(VPFPrimitiveData.Ring ring, VPFPrimitiveData primitiveData,
        EdgeListener listener)
    {
        int count = 0;

        for (int edgeId : ring.edgeId)
        {
            VPFPrimitiveData.EdgeInfo edgeInfo = (VPFPrimitiveData.EdgeInfo)
                primitiveData.getPrimitiveInfo(VPFConstants.EDGE_PRIMITIVE_TABLE, edgeId);

            if (!edgeInfo.isOnTileBoundary())
            {
                if (listener != null)
                    listener.nextEdge(edgeId, edgeInfo);
                count++;
            }
        }

        return count;
    }

    protected Iterable<? extends Sector> doGetSectors(DrawContext dc, double texelSizeRadians)
    {
        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            Arrays.asList(this.feature.getBounds().toSector()), this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    public Iterable<? extends LatLon> getLocations()
    {
        return this.buffer.getLocations();
    }

    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        throw new UnsupportedOperationException();
    }

    public void dispose()
    {
        GLContext glContext = GLContext.getCurrent();
        if (glContext == null)
            return;

        if (this.interiorDisplayList > 0)
        {
            glContext.getGL().glDeleteLists(this.interiorDisplayList, 1);
            this.interiorDisplayList = 0;
        }
    }

    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void drawInterior(DrawContext dc)
    {
        GL gl = dc.getGL();

        if (this.interiorDisplayList <= 0)
        {
            this.interiorDisplayList = gl.glGenLists(1);
            gl.glNewList(this.interiorDisplayList, GL.GL_COMPILE);
            tessellateInterior(dc, new SurfaceConcaveShape.ImmediateModeCallback(dc));
            gl.glEndList();
        }

        gl.glCallList(this.interiorDisplayList);
    }

    protected void drawOutline(DrawContext dc, int drawMode)
    {
        // Intentionally override the superclass specified drawMode. Edges features are not necessarily closed loops, 
        // therefore each edge must be rendered as separate line strip.
        this.buffer.bindAsVertexBuffer(dc);
        this.buffer.multiDrawArrays(dc, GL.GL_LINE_STRIP);
    }

    protected WWTexture getTexture()
    {
        if (this.texture == null && this.attributes.getInteriorImageSource() != null)
        {
            this.texture = new BasicWWTexture(this.attributes.getInteriorImageSource(),
                ((VPFSymbolAttributes) this.attributes).isMipMapIconImage());
        }

        return this.texture;
    }

    protected void doTessellate(DrawContext dc, GLU glu, GLUtessellator tess, GLUtessellatorCallback callback)
    {
        String primitiveName = feature.getFeatureClass().getPrimitiveTableName();

        // Setup the winding order to correctly tessellate the outer and inner rings. The outer ring is specified
        // with a clockwise winding order, while inner rings are specified with a counter-clockwise order. Inner
        // rings are subtracted from the outer ring, producing an area with holes.
        glu.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NEGATIVE);
        glu.gluTessBeginPolygon(tess, null);

        for (int id : this.feature.getPrimitiveIds())
        {
            VPFPrimitiveData.FaceInfo faceInfo = (VPFPrimitiveData.FaceInfo) primitiveData.getPrimitiveInfo(
                primitiveName, id);

            tessellateRing(glu, tess, faceInfo.getOuterRing());

            for (VPFPrimitiveData.Ring ring : faceInfo.getInnerRings())
            {
                tessellateRing(glu, tess, ring);
            }
        }

        glu.gluTessEndPolygon(tess);
    }

    protected void tessellateRing(GLU glu, GLUtessellator tess, VPFPrimitiveData.Ring ring)
    {
        CompoundVecBuffer buffer = this.primitiveData.getPrimitiveCoords(VPFConstants.EDGE_PRIMITIVE_TABLE);
        glu.gluTessBeginContour(tess);

        int numEdges = ring.getNumEdges();
        for (int i = 0; i < numEdges; i++)
        {
            VecBuffer vecBuffer = buffer.getSubBuffer(ring.getEdgeId(i));
            Iterable<double[]> iterable = (ring.getEdgeOrientation(i) < 0) ?
                vecBuffer.getReverseCoords(3) : vecBuffer.getCoords(3);

            for (double[] coords : iterable)
            {
                glu.gluTessVertex(tess, coords, 0, coords);
            }
        }

        glu.gluTessEndContour(tess);
    }
}
