/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.pick.Pickable;

import java.awt.*;

/**
 * @author tag
 * @version $Id: ScreenCredit.java 11421 2009-06-03 13:23:25Z tgaskins $
 */
public interface ScreenCredit extends Renderable, Pickable
{
    void setViewport(Rectangle viewport);

    Rectangle getViewport();

    void setOpacity(double opacity);

    double getOpacity();

    void setLink(String link);

    String getLink();
}
