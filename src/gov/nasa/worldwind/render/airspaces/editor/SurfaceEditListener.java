/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import java.util.EventListener;

/**
 * @author dcollins
 * @version $Id: AirspaceEditListener.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public interface SurfaceEditListener extends EventListener
{
    void SurfaceMoved(SurfaceEditEvent e);

    void SurfaceResized(SurfaceEditEvent e);

    void controlPointAdded(SurfaceEditEvent e);

    void controlPointRemoved(SurfaceEditEvent e);

    void controlPointChanged(SurfaceEditEvent e);
}
