/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.geom.Vec4;

/**
 * @author dcollins
 * @version $Id: AirspaceControlPoint.java 8748 2009-02-04 05:25:56Z dcollins $
 */
public interface SurfaceControlPoint
{
    SurfaceEditor getEditor();

    SurfaceShape getSurfaceShape();

    int getLocationIndex();

    Vec4 getPoint();

    Object getKey();
}
