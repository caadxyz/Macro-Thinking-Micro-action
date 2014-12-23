/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;

import java.util.EventObject;

/**
 * @author dcollins
 * @version $Id: AirspaceEditEvent.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public class SurfaceEditEvent extends EventObject
{
    private SurfaceShape surfaceShape;
    private SurfaceEditor editor;
    private SurfaceControlPoint controlPoint;

    public SurfaceEditEvent(Object source, SurfaceShape surfaceShape, SurfaceEditor editor, SurfaceControlPoint controlPoint)
    {
        super(source);
        this.surfaceShape = surfaceShape;
        this.editor = editor;
        this.controlPoint = controlPoint;
    }

    public SurfaceEditEvent(Object source, SurfaceShape surfaceShape, SurfaceEditor editor)
    {
        this(source, surfaceShape, editor, null);
    }

    public SurfaceShape getSurfaceShape()
    {
        return this.surfaceShape;
    }

    public SurfaceEditor getEditor()
    {
        return this.editor;
    }

    public SurfaceControlPoint getControlPoint()
    {
        return this.controlPoint;
    }
}
