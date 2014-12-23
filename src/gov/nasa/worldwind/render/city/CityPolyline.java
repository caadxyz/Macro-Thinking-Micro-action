/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AbstractSurfaceShape;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.WWTexture;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.measure.LengthMeasurer;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallback;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import org.postgis.Point;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: SurfacePolyline.java 12728 2009-10-19 21:36:25Z dcollins $
 */
public class CityPolyline extends AbstractSurfaceShape
{   protected static GLU glu;
    protected static GLUtessellator tess;
    protected WWTexture texture;
    protected boolean closed = false;
    protected boolean  setDrawInterior=true;
    public List<LatLon> locations = new ArrayList<LatLon>();
   
    ////////////////////////////////////////////////

    //////////////////////////////////////////////////
    
    public Polyline polyline;
    public CityPolyline(ShapeAttributes attributes, Iterable<? extends LatLon> iterable)
    {
        super(attributes);

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        
       this.addLocations(iterable);
       polyline=new Polyline(locations,0);
       
    }
    

    
    public CityPolyline(ShapeAttributes attributes,List<? extends LatLon> locations)
    {   super(attributes);
        this.addLocations(locations);
        polyline=new Polyline(locations,0);
        
    }
    
    protected void addLocations(Iterable<? extends LatLon> newLocations)
    {
        if (newLocations != null)
            for (LatLon ll : newLocations)
            {
                if (ll != null)
                    this.locations.add(ll);                
            }
    }

    public Polyline getPolyline(Iterable<? extends LatLon> locations){    	
    	
    
    	this.polyline.setPositions(locations,0);
    	
    	
    	
    	return polyline;
    }
    
    
    public CityPolyline(ShapeAttributes attributes)
    {
        this(attributes, new java.util.ArrayList<LatLon>());
    }

    public CityPolyline(Iterable<? extends LatLon> iterable)
    {
        this(new BasicShapeAttributes(), iterable);
    }

    public CityPolyline()
    {
        this(new BasicShapeAttributes());
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    public void setClosed(boolean closed)
    {
        this.closed = closed;
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getLocations();
    }
    
    public List<LatLon> getLocations()
    {
        return this.locations;
    }


    
    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.locations.clear();
        this.addLocations(iterable);
        this.onShapeChanged();
    }

 
    
    
    public Position getReferencePosition()
    {
        Iterator<? extends LatLon> iterator = this.locations.iterator();
        if (!iterator.hasNext())
            return null;

        return new Position(iterator.next(), 0);
    }

    protected Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntervalsPerDegree)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.ArrayList<LatLon> newLocations = new java.util.ArrayList<LatLon>();
        getSurfaceShapeSupport().generateIntermediateLocations(this.locations, this.pathType,
            edgeIntervalsPerDegree, this.minEdgeIntervals, this.maxEdgeIntervals, false, newLocations);

        return newLocations;
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        java.util.ArrayList<LatLon> newLocations = new java.util.ArrayList<LatLon>();

        for (LatLon ll : this.locations)
        {
            Angle heading = LatLon.greatCircleAzimuth(oldReferencePosition, ll);
            Angle pathLength = LatLon.greatCircleDistance(oldReferencePosition, ll);
            newLocations.add(LatLon.greatCircleEndPosition(newReferencePosition, heading, pathLength));
        }

        this.setLocations(newLocations);
    }

    protected void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Polyline does not render an interior.
    	
   
	// Concave shape makes no assumptions about the nature or structure of the shape's vertices. The interior is
        // treated as a potentially complex polygon, and this code will do its best to rasterize that polygon. The
        // outline is treated as a simple line loop, regardless of whether the shape's vertices actually define a
        // closed path.
       if(setDrawInterior){
        LatLon shapeCenter = Sector.boundingSector(this.getLocations(dc.getGlobe())).getCentroid();
        getSurfaceShapeSupport().applyInteriorState(dc, this.attributes, this.getTexture(), sector,
            new Rectangle(x, y, width, height), shapeCenter);
        this.drawInterior(dc);
       }
        
    }
    
    protected void drawInterior(DrawContext dc)
    {
        this.tessellateInterior(dc, new ImmediateModeCallback(dc));
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

    protected void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        getSurfaceShapeSupport().applyOutlineState(dc, this.attributes);
        this.drawOutline(dc, (this.isClosed() ? GL.GL_LINE_LOOP : GL.GL_LINE_STRIP));
    }

    protected void drawOutline(DrawContext dc, int drawMode)
    {
        getSurfaceShapeSupport().drawLocations(dc, drawMode, this.drawLocations, this.drawLocations.size());
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    

    
    
    
    
    
    
    
    
    
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    

    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        Iterable<? extends LatLon> iterable = this.getLocations();
        if (iterable != null)
            rs.addStateValueAsLatLonList(context, "locationList", iterable);

        rs.addStateValueAsBoolean(context, "closed", this.isClosed());
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Iterable<LatLon> iterable = rs.getStateValueAsLatLonList(context, "locationList");
        if (iterable != null)
            this.setLocations(iterable);

        Boolean b = rs.getStateValueAsBoolean(context, "closed");
        if (b != null)
            this.setClosed(b);
    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        java.util.ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setLocations(locations);
    }
}
