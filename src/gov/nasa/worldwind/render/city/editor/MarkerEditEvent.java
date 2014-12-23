/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city.editor;


import gov.nasa.worldwind.render.city.ActivityMarker;
import gov.nasa.worldwind.render.markers.Marker;

import java.util.EventObject;

/**
 * @author dcollins
 * @version $Id: AirspaceEditEvent.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public class MarkerEditEvent extends EventObject
{
    private Marker marker;
    private MarkerEditor editor;
    private MarkerControlPoint controlPoint;

    public MarkerEditEvent(Object source, Marker marker, MarkerEditor editor, MarkerControlPoint controlPoint)
    {
        super(source);
        this.marker = marker;
        this.editor = editor;
        this.controlPoint = controlPoint;
    }

    public  MarkerEditEvent(Object source, Marker marker, MarkerEditor editor)
    {
        this(source, marker, editor, null);
    }

    public Marker getMarker()
    {
        return this.marker;
    }

    public MarkerEditor getEditor()
    {
        return this.editor;
    }

    public MarkerControlPoint getControlPoint()
    {
        return this.controlPoint;
    }
}
