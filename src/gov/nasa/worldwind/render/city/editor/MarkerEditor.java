/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city.editor;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.city.ActivityMarker;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.layers.Layer;

import java.awt.*;

/**
 * @author dcollins
 * @version $Id: AirspaceEditor.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public interface MarkerEditor extends Layer
{
    ActivityMarker getMarker();

    boolean isArmed();

    void setArmed(boolean armed);

    boolean isKeepControlPointsAboveTerrain();

    void setKeepControlPointsAboveTerrain(boolean state);

    MarkerControlPointRenderer getControlPointRenderer();

    void setControlPointRenderer(MarkerControlPointRenderer renderer);

    MarkerEditListener[] getEditListeners();

    void addEditListener(MarkerEditListener listener);

    void removeEditListener(MarkerEditListener listener);

    // TODO
    // the purposes of these methods may be okay, but there are some obvious problems:
    //
    // 1. any change in parameters would require a signature change (params should be bundled)
    //
    // 2. they do not allow the editor any control over how to respond to input
    //
    // 3. they assume the editor can do something reasonable with the call

    void moveMarkerLaterally(WorldWindow wwd, Marker marker,
        Point mousePoint, Point previousMousePoint);



    MarkerControlPoint addControlPoint(WorldWindow wwd, Marker marker, 
        Point mousePoint);

    void removeControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint);

    void moveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);

    void resizeAtControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
        Point mousePoint, Point previousMousePoint);
}
