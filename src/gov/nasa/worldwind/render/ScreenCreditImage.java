/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.Logging;

import java.awt.*;

/**
 * @author tag
 * @version $Id: ScreenCreditImage.java 12720 2009-10-14 18:32:50Z tgaskins $
 */
public class ScreenCreditImage extends ScreenImage implements ScreenCredit
{
    private String link;
    private Rectangle viewport;

    public ScreenCreditImage(Object imageSource)
    {
        if (imageSource == null)
        {
            String msg = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setImageSource(imageSource);
    }

    public void setViewport(Rectangle viewport)
    {
        if (viewport == null)
        {
            String msg = Logging.getMessage("nullValue.ViewportIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.viewport = viewport;
        this.setScreenLocation(new Point(viewport.x, viewport.y));
    }

    public Rectangle getViewport()
    {
        return this.viewport;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getLink()
    {
        return this.link;
    }
}
