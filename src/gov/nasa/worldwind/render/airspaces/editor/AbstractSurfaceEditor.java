/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.util.Logging;

import javax.swing.event.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author dcollins
 * @version $Id: AbstractAirspaceEditor.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public abstract class AbstractSurfaceEditor extends AbstractLayer implements SurfaceEditor
{
    private boolean armed;
    private boolean useRubberBand;
    private boolean keepControlPointsAboveTerrain;
    private SurfaceControlPointRenderer controlPointRenderer;
    private EventListenerList eventListeners = new EventListenerList();
    // List of control points from the last call to draw().
    private ArrayList<SurfaceControlPoint> currentControlPoints = new ArrayList<SurfaceControlPoint>();

    public AbstractSurfaceEditor(SurfaceControlPointRenderer renderer)
    {
        if (renderer == null)
        {
            String message = Logging.getMessage("nullValue.RendererIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.armed = false;
        this.useRubberBand = true;
        this.keepControlPointsAboveTerrain = false;
        this.controlPointRenderer = renderer;
    }

    public AbstractSurfaceEditor()
    {
        this(new BasicSurfaceControlPointRenderer());
    }

    public boolean isArmed()
    {
        return this.armed;
    }

    public void setArmed(boolean armed)
    {
        this.armed = armed;
    }

    public boolean isUseRubberBand()
    {
        return this.useRubberBand;
    }

    public void setUseRubberBand(boolean state)
    {
        this.useRubberBand = state;
    }

    public boolean isKeepControlPointsAboveTerrain()
    {
        return this.keepControlPointsAboveTerrain;
    }

    public void setKeepControlPointsAboveTerrain(boolean state)
    {
        this.keepControlPointsAboveTerrain = state;
    }
    
    public SurfaceControlPointRenderer getControlPointRenderer()
    {
        return this.controlPointRenderer;
    }

    public void setControlPointRenderer(SurfaceControlPointRenderer renderer)
    {
        if (renderer == null)
        {
            String message = Logging.getMessage("nullValue.RendererIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.controlPointRenderer = renderer;
    }

    public SurfaceEditListener[] getEditListeners()
    {
        return this.eventListeners.getListeners(SurfaceEditListener.class);
    }

    public void addEditListener(SurfaceEditListener listener)
    {
        this.eventListeners.add(SurfaceEditListener.class, listener);
    }

    public void removeEditListener(SurfaceEditListener listener)
    {
        this.eventListeners.remove(SurfaceEditListener.class, listener);
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

    protected java.util.List<SurfaceControlPoint> getCurrentControlPoints()
    {
        return this.currentControlPoints;
    }

    protected void setCurrentControlPoints(java.util.List<? extends SurfaceControlPoint> controlPointList)
    {
        this.currentControlPoints.clear();
        this.currentControlPoints.addAll(controlPointList);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void addControlPoint(DrawContext dc, SurfaceControlPoint controlPoint)
    {
        this.currentControlPoints.add(controlPoint);
    }

    protected abstract void assembleControlPoints(DrawContext dc);
    
    //**************************************************************//
    //********************  Control Point Events  ******************//
    //**************************************************************//

    public void moveSurfaceLaterally(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getSurfaceShape() == null || this.getSurfaceShape() != surfaceShape)
        {
            return;
        }

        this.doMoveSurfaceLaterally(wwd, surfaceShape, mousePoint, previousMousePoint);
    }

    
    public SurfaceControlPoint addControlPoint(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getSurfaceShape() == null || this.getSurfaceShape() != surfaceShape)
        {
            return null;
        }

        if (wwd == null || mousePoint == null)
        {
            return null;
        }

        return this.doAddControlPoint(wwd, surfaceShape, mousePoint);
    }

    public void removeControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getSurfaceShape() == null)
        {
            return;
        }

        if (wwd == null || controlPoint == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getSurfaceShape() != controlPoint.getSurfaceShape())
        {
            return;
        }

        this.doRemoveControlPoint(wwd, controlPoint);
    }

    public void moveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getSurfaceShape() == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getSurfaceShape() != controlPoint.getSurfaceShape())
        {
            return;
        }

        this.doMoveControlPoint(wwd, controlPoint, mousePoint, previousMousePoint);
    }

    public void resizeAtControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint)
    {
        // Include this test to ensure any derived implementation performs it.
        if (this.getSurfaceShape() == null)
        {
            return;
        }

        if (this != controlPoint.getEditor() || this.getSurfaceShape() != controlPoint.getSurfaceShape())
        {
            return;
        }

        this.doResizeAtControlPoint(wwd, controlPoint, mousePoint, previousMousePoint);
    }

    protected void fireSurfaceMoved(SurfaceEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        SurfaceEditListener[] listeners = this.eventListeners.getListeners(SurfaceEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].SurfaceMoved(e);
        }
    }

    protected void fireSurfaceResized(SurfaceEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        SurfaceEditListener[] listeners = this.eventListeners.getListeners(SurfaceEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].SurfaceResized(e);
        }
    }

    protected void fireControlPointAdded(SurfaceEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        SurfaceEditListener[] listeners = this.eventListeners.getListeners(SurfaceEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointAdded(e);
        }
    }

    protected void fireControlPointRemoved(SurfaceEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        SurfaceEditListener[] listeners = this.eventListeners.getListeners(SurfaceEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointRemoved(e);
        }
    }

    protected void fireControlPointChanged(SurfaceEditEvent e)
    {
        // Iterate over the listener list in reverse order. This has the effect of notifying the listeners in the
        // order they were added.
        SurfaceEditListener[] listeners = this.eventListeners.getListeners(SurfaceEditListener.class);
        for (int i = listeners.length - 1; i >= 0; i--)
        {
            listeners[i].controlPointChanged(e);
        }
    }

    protected abstract SurfaceControlPoint doAddControlPoint(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint);

    protected abstract void doRemoveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint);

    protected abstract void doMoveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    protected abstract void doResizeAtControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    //**************************************************************//
    //********************  Default Event Handling  ****************//
    //**************************************************************//

    protected void doMoveSurfaceLaterally(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint, Point previousMousePoint)
    {
        // Intersect a ray throuh each mouse point, with a geoid passing through the reference elevation. Since
        // most airspace control points follow a fixed altitude, this will track close to the intended mouse position.
        // If either ray fails to intersect the geoid, then ignore this event. Use the difference between the two
        // intersected positions to move the control point's location.

        if (!(surfaceShape instanceof Movable))
        {
            return;
        }

        Movable movable = (Movable) surfaceShape;
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

        this.fireSurfaceMoved(new SurfaceEditEvent(wwd, surfaceShape, this));
    }


}
