/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city.editor;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.editor.AirspaceEditorUtil;
import gov.nasa.worldwind.render.city.ActivityMarker;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.util.Logging;

import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author dcollins
 * @version $Id: AbstractAirspaceEditor.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public abstract class AbstractMarkerEditor extends AbstractLayer implements MarkerEditor
{
    private boolean armed;
    private boolean keepControlPointsAboveTerrain;
    private MarkerControlPointRenderer controlPointRenderer;
    private EventListenerList eventListeners = new EventListenerList();
    // List of control points from the last call to draw().
    private ArrayList<MarkerControlPoint> currentControlPoints = new ArrayList<MarkerControlPoint>();

    public AbstractMarkerEditor(MarkerControlPointRenderer renderer)
    {
        if (renderer == null)
        {
            String message = Logging.getMessage("nullValue.RendererIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.armed = false;
        this.keepControlPointsAboveTerrain = true;
        this.controlPointRenderer = renderer;
    }

    public AbstractMarkerEditor()
    {
        this(new BasicMarkerControlPointRenderer());
    }

    public boolean isArmed()
    {
        return this.armed;
    }

    public void setArmed(boolean armed)
    {
        this.armed = armed;
    }



    public boolean isKeepControlPointsAboveTerrain()
    {
        return this.keepControlPointsAboveTerrain;
    }

    public void setKeepControlPointsAboveTerrain(boolean state)
    {
        this.keepControlPointsAboveTerrain = state;
    }
    
    public MarkerControlPointRenderer getControlPointRenderer()
    {
        return this.controlPointRenderer;
    }

    public void setControlPointRenderer(MarkerControlPointRenderer renderer)
    {
        if (renderer == null)
        {
            String message = Logging.getMessage("nullValue.RendererIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.controlPointRenderer = renderer;
    }

    public MarkerEditListener[] getEditListeners()
    {
        return this.eventListeners.getListeners(MarkerEditListener.class);
    }

    public void addEditListener(MarkerEditListener listener)
    {
        this.eventListeners.add(MarkerEditListener.class, listener);
    }

    public void removeEditListener(MarkerEditListener listener)
    {
        this.eventListeners.remove(MarkerEditListener.class, listener);
    }

    //**************************************************************//
    //********************  Control Point Rendering  ***************//
    //**************************************************************//

    protected void doRender(DrawContext dc)
    {
        if (!this.isArmed())
           return;

        this.draw(dc, null);
    }

    protected void doPick(DrawContext dc, Point point)
    {
        if (!this.isArmed())
            return;

        this.draw(dc, point);
    }

    protected void draw(DrawContext dc, Point pickPoint)
    {
        this.getCurrentControlPoints().clear();
        this.assembleControlPoints(dc);

        if (dc.isPickingMode())
        {
            this.getControlPointRenderer().pick(dc, this.getCurrentControlPoints(), pickPoint, this);
        }
        else
        {
            this.getControlPointRenderer().render(dc, this.getCurrentControlPoints());
        }
    }

    protected java.util.List<MarkerControlPoint> getCurrentControlPoints()
    {
        return this.currentControlPoints;
    }

    protected void setCurrentControlPoints(java.util.List<? extends MarkerControlPoint> controlPointList)
    {
        this.currentControlPoints.clear();
        this.currentControlPoints.addAll(controlPointList);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void addControlPoint(DrawContext dc, MarkerControlPoint controlPoint)
    {
        this.currentControlPoints.add(controlPoint);
    }

    protected abstract void assembleControlPoints(DrawContext dc);
    
    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//

    public void moveMarkerLaterally(WorldWindow wwd, Marker marker,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getMarker() == null || this.getMarker() != marker)
        {
            return;
        }

        this.doMoveMarkerLaterally(wwd, marker, mousePoint, previousMousePoint);
    }

    
    public MarkerControlPoint addControlPoint(WorldWindow wwd, ActivityMarker marker,
        Point mousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getMarker() == null || this.getMarker() != marker)
        {
            return null;
        }

        if (wwd == null || mousePoint == null)
        {
            return null;
        }

        return this.doAddControlPoint(wwd, marker, mousePoint);
    }

    public void removeControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getMarker() == null)
        {
            return;
        }

        if (wwd == null || controlPoint == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getMarker() != controlPoint.getMarker())
        {
            return;
        }

        this.doRemoveControlPoint(wwd, controlPoint);
    }

    public void moveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getMarker() == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getMarker() != controlPoint.getMarker())
        {
            return;
        }

        this.doMoveControlPoint(wwd, controlPoint, mousePoint, previousMousePoint);
    }

    public void resizeAtControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getMarker() == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getMarker() != controlPoint.getMarker())
        {
            return;
        }

        this.doResizeAtControlPoint(wwd, controlPoint, mousePoint, previousMousePoint);
    }

    protected void fireMarkerMoved(MarkerEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        MarkerEditListener[] listeners = this.eventListeners.getListeners(MarkerEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].MarkerMoved(e);
        }
    }

    protected void fireSurfaceResized(MarkerEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        MarkerEditListener[] listeners = this.eventListeners.getListeners(MarkerEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].MarkerResized(e);
        }
    }

    protected void fireControlPointAdded(MarkerEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        MarkerEditListener[] listeners = this.eventListeners.getListeners(MarkerEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointAdded(e);
        }
    }

    protected void fireControlPointRemoved(MarkerEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        MarkerEditListener[] listeners = this.eventListeners.getListeners(MarkerEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointRemoved(e);
        }
    }

    protected void fireControlPointChanged(MarkerEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        MarkerEditListener[] listeners = this.eventListeners.getListeners(MarkerEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointChanged(e);
        }
    }

    protected abstract MarkerControlPoint doAddControlPoint(WorldWindow wwd, ActivityMarker marker,
        Point mousePoint);

    protected abstract void doRemoveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint);

    protected abstract void doMoveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    protected abstract void doResizeAtControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    //**************************************************************//
    //********************  Default Event Handling  ****************//
    //**************************************************************//

    protected void doMoveMarkerLaterally(WorldWindow wwd,  Marker marker,
        Point mousePoint, Point previousMousePoint)
    {
        // Intersect a ray throuh each mouse point, with a geoid passing through the reference elevation. Since
        // most airspace control points follow a fixed altitude, this will track close to the intended mouse position.
        // If either ray fails to intersect the geoid, then ignore this event. Use the difference between the two
        // intersected positions to move the control point's location.

        if (!(marker instanceof Movable))
        {
            return;
        }

        Movable movable = (Movable) marker;
        View view = wwd.getView();
        Globe globe = wwd.getModel().getGlobe();

        Position refPos = movable.getReferencePosition();

        // Convert the reference position into a cartesian point. This assumes that the reference elevation is defined
        // by the airspace's lower altitude.
        Vec4 refPoint = null;
        if (refPoint == null)
            refPoint = globe.computePointFromPosition(refPos);

        // Convert back to a position.
        refPos = globe.computePositionFromPoint(refPoint);

        Line ray = view.computeRayFromScreenPoint(mousePoint.getX(), mousePoint.getY());
        Line previousRay = view.computeRayFromScreenPoint(previousMousePoint.getX(), previousMousePoint.getY());

        Vec4 vec = AirspaceEditorUtil.intersectGlobeAt(wwd, refPos.getElevation(), ray);
        Vec4 previousVec = AirspaceEditorUtil.intersectGlobeAt(wwd, refPos.getElevation(), previousRay);

        if (vec == null || previousVec == null)
        {
            return;
        }

        Position pos = globe.computePositionFromPoint(vec);
        Position previousPos = globe.computePositionFromPoint(previousVec);
        LatLon change = pos.subtract(previousPos);

        movable.move(new Position(change.getLatitude(), change.getLongitude(), 0.0));

        this.fireMarkerMoved(new MarkerEditEvent(wwd, marker, this));
    }


}
