/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.AbstractSurfaceShape;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceConcaveShape;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: SurfacePolyline.java 12728 2009-10-19 21:36:25Z dcollins $
 */
public class CityPolylgon2D extends SurfaceConcaveShape
{
    protected boolean closed = false;
    public List<LatLon> locations = new ArrayList<LatLon>();
    public CityPolylgon2D(ShapeAttributes attributes, Iterable<? extends LatLon> iterable)
    {
        super(attributes);

        if (iterable == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        
       this.addLocations(iterable);
       
    }
    
    
    public CityPolylgon2D(ShapeAttributes attributes,List<? extends LatLon> locations)
    {   super(attributes);
        this.addLocations(locations);
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

    
    
    
    public CityPolylgon2D(ShapeAttributes attributes)
    {
        this(attributes, new java.util.ArrayList<LatLon>());
    }

    public CityPolylgon2D(Iterable<? extends LatLon> iterable)
    {
        this(new BasicShapeAttributes(), iterable);
    }

    public CityPolylgon2D()
    {
        this(new BasicShapeAttributes());
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



    //**************************************************************//
    //******************** Restorable State  ***********************//
    //**************************************************************//

    protected void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doGetRestorableState(rs, context);

        Iterable<? extends LatLon> iterable = this.getLocations();
        if (iterable != null)
            rs.addStateValueAsLatLonList(context, "locationList", iterable);
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.doRestoreState(rs, context);

        Iterable<LatLon> iterable = rs.getStateValueAsLatLonList(context, "locationList");
        if (iterable != null)
            this.setLocations(iterable);

    }

    protected void legacyRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        super.legacyRestoreState(rs, context);

        java.util.ArrayList<LatLon> locations = rs.getStateValueAsLatLonList(context, "locations");
        if (locations != null)
            this.setLocations(locations);
    }
}
