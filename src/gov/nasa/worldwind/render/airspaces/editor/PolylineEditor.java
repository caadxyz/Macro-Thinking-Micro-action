/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.city.CityPolyline;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author dcollins
 * @version $Id: PolygonEditor.java 8780 2009-02-07 00:42:00Z dcollins $
 */
public class PolylineEditor extends AbstractSurfaceEditor
{
    private CityPolyline polyline; // Can be null
    
    public PolylineEditor(SurfaceControlPointRenderer renderer)
    {
        super(renderer);
    }

    public PolylineEditor()
    {
    }

    public SurfaceShape getSurfaceShape()
    {
        return this.polyline;
    }

    public CityPolyline getPolyline()
    {
        return this.polyline;
    }

    public void setPolyline(CityPolyline polyline)
    {
        this.polyline = polyline;
    }

    //**************************************************************//
    //********************  Control Point Assembly  ****************//
    //**************************************************************//

    protected void assembleControlPoints(DrawContext dc)
    {
        if (this.getPolyline() == null)
            return;

        int numLocations = this.getPolyline().getLocations().size();
        for (int locationIndex = 0; locationIndex < numLocations; locationIndex++)
        { this.addPolylineControlPoint(dc, locationIndex);
        }
    }

    protected void addPolylineControlPoint(DrawContext dc, int locationIndex)
    {   
        LatLon location = this.getPolyline().getLocations().get(locationIndex);       
        //Vec4 point =dc.getGlobe().computePointFromLocation(location); 
        Vec4 point =
        	dc.getGlobe().computePointFromPosition(location, dc.getGlobe().getElevation(location.latitude,location.longitude));
        SurfaceControlPoint controlPoint =
        new BasicSurfaceControlPoint(this, this.getPolyline(), locationIndex, point);
        this.addControlPoint(dc, controlPoint);
    }

    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//

    protected SurfaceControlPoint doAddControlPoint(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint)
    {
        if (((this.getPolyline().getLocations())).isEmpty())
        {
            return this.doAddFirstLocation(wwd, mousePoint);
        }
        else
        {
            return this.doAddNextLocation(wwd, mousePoint);
        }
    }

    protected SurfaceControlPoint doAddFirstLocation(WorldWindow wwd, Point mousePoint)
    {
        // Adding the first location is unique in two ways:
        //
        // First, the airspace has no existing locations, so the only reference we have to interpret the user's intent
        // is the terrain. We will not modify the terrain conformance property. However we will modify the altitude
        // property to ensure the shape appears correctly on the terrain.
        //
        // Second, the app may want rubber band creation of the first two points. Therefore we add two points and 
        // return a handle to the second point. If rubber banding is enabled, then we return a control point
        // referencing to the second location. Otherwise we return a control point referencing the first location.

        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        double surfaceElevation = SurfaceEditorUtil.surfaceElevationAt(wwd, ray);

        Vec4 newPoint = SurfaceEditorUtil.intersectGlobeAt(wwd, surfaceElevation, ray);
        if (newPoint == null)
        {
            return null;
        }

        Position newPosition = wwd.getModel().getGlobe().computePositionFromPoint(newPoint);
        ArrayList<LatLon> locationList = new ArrayList<LatLon>();
        locationList.add(new LatLon(newPosition));

        // If rubber banding is enabled, add a second entry at the same location.
        if (this.isUseRubberBand())
        {
            locationList.add(new LatLon(newPosition));
        }
        
        this.getPolyline().setLocations(locationList);

        SurfaceControlPoint controlPoint =
            new BasicSurfaceControlPoint(this, this.getPolyline(), 0,  newPoint);
        this.fireControlPointAdded(new SurfaceEditEvent(wwd, this.getSurfaceShape(), this, controlPoint));

        // If rubber banding is enabled, fire a second add event, and return a reference to the second location.
        if (this.isUseRubberBand())
        {
            controlPoint = new BasicSurfaceControlPoint(this, this.getPolyline(), 1,  newPoint);
            this.fireControlPointAdded(new SurfaceEditEvent(wwd, this.getSurfaceShape(), this, controlPoint));
        }

        return controlPoint;
    }

    protected SurfaceControlPoint doAddNextLocation(WorldWindow wwd, Point mousePoint)
    {
        // Try to find the edge that is closest to a ray passing through the screen point. We're trying to determine
        // the user's intent as to which edge a new two control points should be added to. We create a list of all
        // potentiall control point edges, then find the best match. We compute the new location by intersecting the
        // geoid with the screen ray, then create a new control point by inserting that point into the location list
        // based on the points orientaton relative to the edge.

        List<SurfaceEditorUtil.EdgeInfo> edgeInfoList = SurfaceEditorUtil.computeEdgeInfoFor(
        		this.getPolyline().getLocations().size(), this.getCurrentControlPoints());
       
        if (edgeInfoList.isEmpty())
        {
            return null;
        }

        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        
        SurfaceEditorUtil.EdgeInfo bestMatch = SurfaceEditorUtil.selectBestEdgeMatch(
            wwd, ray, this.getSurfaceShape(), edgeInfoList);
      
        if (bestMatch == null)
        {
            return null;
        }

        SurfaceControlPoint controlPoint = SurfaceEditorUtil.createControlPointFor(
            wwd, ray, this, this.getSurfaceShape(), bestMatch);
      
        
        Vec4 newPoint = controlPoint.getPoint();
        LatLon newLocation = new LatLon(wwd.getModel().getGlobe().computePositionFromPoint(newPoint));
        
        ArrayList<LatLon> locationList = new ArrayList<LatLon> (this.getPolyline().getLocations());
        
        locationList.add(controlPoint.getLocationIndex(), newLocation);
        this.getPolyline().setLocations(locationList);

        this.fireControlPointAdded(new SurfaceEditEvent(wwd, this.getSurfaceShape(), this, controlPoint));

        return controlPoint;
    }

    protected void doRemoveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint)
    {
        int index = controlPoint.getLocationIndex();
        List<LatLon> newLocationList = new ArrayList<LatLon> ((ArrayList<? extends LatLon>)(this.getPolyline().getLocations()));
        newLocationList.remove(index);
        this.getPolyline().setLocations(newLocationList);

        this.fireControlPointRemoved(new SurfaceEditEvent(wwd, controlPoint.getSurfaceShape(), this, controlPoint));
    }

    protected void doMoveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Intersect a ray throuh each mouse point, with a geoid passing through the selected control point. Since
        // most airspace control points follow a fixed altitude, this will track close to the intended mouse position.
        // If either ray fails to intersect the geoid, then ignore this event. Use the difference between the two
        // intersected positions to move the control point's location.

        Position controlPointPos = wwd.getModel().getGlobe().computePositionFromPoint(controlPoint.getPoint());

        Line ray = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        Line previousRay = wwd.getView().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());

        Vec4 vec = SurfaceEditorUtil.intersectGlobeAt(wwd, controlPointPos.getElevation(), ray);
        Vec4 previousVec = SurfaceEditorUtil.intersectGlobeAt(wwd, controlPointPos.getElevation(), previousRay);
        
        if (vec == null || previousVec == null)
        {
            return;
        }

        Position pos = wwd.getModel().getGlobe().computePositionFromPoint(vec);
        Position previousPos = wwd.getModel().getGlobe().computePositionFromPoint(previousVec);
        LatLon change = pos.subtract(previousPos);

        int index = controlPoint.getLocationIndex();
        List<LatLon> newLocationList = new ArrayList<LatLon> ((ArrayList<? extends LatLon>)(this.getPolyline().getLocations()));
        LatLon newLatLon = newLocationList.get(index).add(change);
        newLocationList.set(index, newLatLon);
        this.getPolyline().setLocations(newLocationList);

        this.fireControlPointChanged(new SurfaceEditEvent(wwd, controlPoint.getSurfaceShape(), this, controlPoint));
    }

    protected void doResizeAtControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Find the closest points between the rays through each screen point, and the ray from the control point
        // and in the direction of the globe's surface normal. Compute the elevation difference between these two
        // points, and use that as the change in airspace altitude.
        //
        // When the airspace is collapsed, override the
        // selected control point altitude. This will typically be the case when the airspace is new. If the user drags
        // up, then adjust the upper altiutde. If the user drags down, then adjust the lower altitude.
        //
        // If the state keepControlPointsAboveTerrain is set, we prevent the control point from passing any lower than
        // the terrain elevation beneath it.

        Vec4 surfaceNormal = wwd.getModel().getGlobe().computeSurfaceNormalAtPoint(controlPoint.getPoint());
        Line verticalRay = new Line(controlPoint.getPoint(), surfaceNormal);
        Line screenRay = wwd.getView().computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());
        Line previousScreenRay = wwd.getView().computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());

        Vec4 pointOnLine = SurfaceEditorUtil.nearestPointOnLine(verticalRay, screenRay);
        Vec4 previousPointOnLine = SurfaceEditorUtil.nearestPointOnLine(verticalRay, previousScreenRay);

        Position pos = wwd.getModel().getGlobe().computePositionFromPoint(pointOnLine);
        Position previousPos = wwd.getModel().getGlobe().computePositionFromPoint(previousPointOnLine);
        double elevationChange = previousPos.getElevation() - pos.getElevation();
        double height = SurfaceEditorUtil.computeLowestHeightAboveSurface(
                    wwd, this.getCurrentControlPoints());
                if (elevationChange <= -height)
                    elevationChange = -height;       

      SurfaceEditEvent editEvent = new SurfaceEditEvent(wwd, controlPoint.getSurfaceShape(), this, controlPoint);
        this.fireControlPointChanged(editEvent);
        this.fireSurfaceResized(editEvent);
    }
}
