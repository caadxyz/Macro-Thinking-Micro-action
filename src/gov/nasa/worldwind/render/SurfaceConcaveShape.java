/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.*;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: SurfaceConcaveShape.java 12662 2009-09-28 18:19:37Z dcollins $
 */
public abstract class SurfaceConcaveShape extends AbstractSurfaceShape
{
    protected static GLU glu;
    protected static GLUtessellator tess;
    protected WWTexture texture;

    protected SurfaceConcaveShape(ShapeAttributes attributes)
    {
        super(attributes);
    }

    protected SurfaceConcaveShape()
    {
    }

    protected void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Concave shape makes no assumptions about the nature or structure of the shape's vertices. The interior is
        // treated as a potentially complex polygon, and this code will do its best to rasterize that polygon. The
        // outline is treated as a simple line loop, regardless of whether the shape's vertices actually define a
        // closed path.

        LatLon shapeCenter = Sector.boundingSector(this.getLocations(dc.getGlobe())).getCentroid();
        getSurfaceShapeSupport().applyInteriorState(dc, this.attributes, this.getTexture(), sector,
            new Rectangle(x, y, width, height), shapeCenter);
        this.drawInterior(dc);
    }

    protected void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        getSurfaceShapeSupport().applyOutlineState(dc, this.attributes);
        this.drawOutline(dc, GL.GL_LINE_LOOP);
    }

    protected void drawInterior(DrawContext dc)
    {
        this.tessellateInterior(dc, new ImmediateModeCallback(dc));
    }

    protected void drawOutline(DrawContext dc, int drawMode)
    {
        getSurfaceShapeSupport().drawLocations(dc, drawMode, this.drawLocations, this.drawLocations.size());
    }

    protected WWTexture getTexture()
    {
        if (this.attributes.getInteriorImageSource() == null)
            return null;

        if (this.texture == null && this.attributes.getInteriorImageSource() != null)
            this.texture = new BasicWWTexture(this.attributes.getInteriorImageSource());

        return this.texture;
    }

    protected static GLU getGLU()
    {
        if (glu == null)
        {
            glu = new GLU();
        }

        return glu;
    }

    protected static GLUtessellator getGLUTessellator()
    {
        if (tess == null)
        {
            tess = glu.gluNewTess();
        }

        return tess;
    }

    //**************************************************************//
    //********************  Interior Tessellation  *****************//
    //**************************************************************//

    protected void tessellateInterior(DrawContext dc, GLUtessellatorCallback callback)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        GLU glu = new GLU();
        GLUtessellator tess = glu.gluNewTess();
        this.beginTessellation(dc, glu, tess, callback);

        try
        {
            this.doTessellate(dc, glu, tess, callback);
        }
        finally
        {
            this.endTessellation(dc, glu, tess);
            glu.gluDeleteTess(tess);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void beginTessellation(DrawContext dc, GLU glu, GLUtessellator tess, GLUtessellatorCallback callback)
    {
        glu.gluTessNormal(tess, 0.0, 0.0, 1.0);
        glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_END, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback);        
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void endTessellation(DrawContext dc, GLU glu, GLUtessellator tess)
    {
        glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, null);
        glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, null);
        glu.gluTessCallback(tess, GLU.GLU_TESS_END, null);
        glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, null);
    }

    protected void doTessellate(DrawContext dc, GLU glu, GLUtessellator tess, GLUtessellatorCallback callback)
    {
        // Determine the winding order of the shape vertices, and setup the GLU winding rule which corresponds to
        // the shapes winding order.
        //noinspection StringEquality
        int windingRule = (WWMath.computePolygonWindingOrder(this.drawLocations) == AVKey.CLOCKWISE)
            ? GLU.GLU_TESS_WINDING_NEGATIVE : GLU.GLU_TESS_WINDING_POSITIVE;

        glu.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, windingRule);
        glu.gluTessBeginPolygon(tess, null);
        glu.gluTessBeginContour(tess);

        for (LatLon ll : this.drawLocations)
        {
            double[] compArray = new double[3];
            compArray[1] = ll.getLatitude().degrees;
            compArray[0] = ll.getLongitude().degrees;
            glu.gluTessVertex(tess, compArray, 0, compArray);
        }

        glu.gluTessEndContour(tess);
        glu.gluTessEndPolygon(tess);
    }

    protected static class ImmediateModeCallback extends GLUtessellatorCallbackAdapter
    {
        protected final GL gl;

        public ImmediateModeCallback(DrawContext dc)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.gl = dc.getGL();
        }

        public void begin(int type)
        {
            this.gl.glBegin(type);
        }

        public void vertex(Object vertexData)
        {
            this.gl.glVertex3dv((double[]) vertexData, 0);
        }

        public void end()
        {
            this.gl.glEnd();
        }
        
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData)
        {
            outData[0] = coords;
        }
    }
}
