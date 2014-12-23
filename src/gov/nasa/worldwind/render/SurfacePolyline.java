/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: SurfacePolyline.java 12728 2009-10-19 21:36:25Z dcollins $
 */
public class SurfacePolyline extends AbstractSurfaceShape
{
    protected boolean closed = false;
    protected Iterable<? extends LatLon> locations; 
    //public List<LatLon> locations = new ArrayList<LatLon>();
   

    public SurfacePolyline(ShapeAttributes attributes, Iterable<? extends LatLon> iterable)
    {
        super(attributes);

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        
        this.locations = iterable;
        //this.addLocations(iterable);
       
    }
    
    
    public SurfacePolyline(ShapeAttributes attributes)
    {
        this(attributes, new java.util.ArrayList<LatLon>());
    }

    public SurfacePolyline(Iterable<? extends LatLon> iterable)
    {
        this(new BasicShapeAttributes(), iterable);
    }

    public SurfacePolyline()
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
    
    //public List<LatLon> getLocations()
    
    public Iterable<? extends LatLon> getLocations()
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

        this.locations = iterable;
        //this.locations.clear();
        //this.addLocations(iterable);        
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
