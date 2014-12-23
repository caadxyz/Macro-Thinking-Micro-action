/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city.editor;

import java.util.EventListener;

/**
 * @author dcollins
 * @version $Id: AirspaceEditListener.java 8772 2009-02-05 23:50:46Z dcollins $
 */
public interface MarkerEditListener extends EventListener
{
    void MarkerMoved(MarkerEditEvent e);

    void MarkerResized(MarkerEditEvent e);

    void controlPointAdded(MarkerEditEvent e);

    void controlPointRemoved(MarkerEditEvent e);

    void controlPointChanged(MarkerEditEvent e);
}
