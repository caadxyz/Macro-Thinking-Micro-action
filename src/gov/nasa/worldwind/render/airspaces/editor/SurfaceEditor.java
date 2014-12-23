/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.layers.Layer;

import java.awt.*;

/**
 * @author dcollins
 * @version $Id: AirspaceEditor.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public interface SurfaceEditor extends Layer
{
    SurfaceShape getSurfaceShape();

    boolean isArmed();

    void setArmed(boolean armed);

    boolean isUseRubberBand();

    void setUseRubberBand(boolean state);

    boolean isKeepControlPointsAboveTerrain();

    void setKeepControlPointsAboveTerrain(boolean state);

    SurfaceControlPointRenderer getControlPointRenderer();

    void setControlPointRenderer(SurfaceControlPointRenderer renderer);

    SurfaceEditListener[] getEditListeners();

    void addEditListener(SurfaceEditListener listener);

    void removeEditListener(SurfaceEditListener listener);

    // TODO
    // the purposes of these methods may be okay, but there are some obvious problems:
    //
    // 1. any change in parameters would require a signature change (params should be bundled)
    //
    // 2. they do not allow the editor any control over how to respond to input
    //
    // 3. they assume the editor can do something reasonable with the call

    void moveSurfaceLaterally(WorldWindow wwd, SurfaceShape surfaceShape,
        Point mousePoint, Point previousMousePoint);



    SurfaceControlPoint addControlPoint(WorldWindow wwd, SurfaceShape surfaceShape, 
        Point mousePoint);

    void removeControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint);

    void moveControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    void resizeAtControlPoint(WorldWindow wwd, SurfaceControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);
}
