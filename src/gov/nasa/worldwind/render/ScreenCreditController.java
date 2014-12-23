/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.pick.Pickable;
import gov.nasa.worldwind.util.BrowserOpener;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author tag
 * @version $Id: ScreenCreditController.java 11477 2009-06-06 01:56:55Z tgaskins $
 */
public class ScreenCreditController implements Renderable, SelectListener, Disposable, Pickable
{
    private int creditWidth = 32;
    private int creditHeight = 32;
    private int leftMargin = 200;
    private int bottomMargin = 1;
    private int separation = 10;
    private double baseOpacity = 0.5;
    private double highlightOpacity = 1;
    private WorldWindow wwd;

    public ScreenCreditController(WorldWindow wwd)
    {
        if (wwd == null)
        {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;
        wwd.getSceneController().setScreenCreditController(this);
        wwd.addSelectListener(this);
    }

    public void dispose()
    {
        wwd.removeSelectListener(this);
        if (wwd.getSceneController() == this)
            wwd.getSceneController().setScreenCreditController(null);
    }

    public void pick(DrawContext dc, Point pickPoint)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getScreenCredits() == null || dc.getScreenCredits().size() < 1)
            return;

        Set<Map.Entry<ScreenCredit, Long>> credits = dc.getScreenCredits().entrySet();

        int y = dc.getDrawableHeight() - (bottomMargin + creditHeight / 2);
        int x = leftMargin + creditWidth / 2;

        for (Map.Entry<ScreenCredit, Long> entry : credits)
        {
            ScreenCredit credit = entry.getKey();
            Rectangle viewport = new Rectangle(x, y, creditWidth, creditHeight);

            credit.setViewport(viewport);
            credit.pick(dc, pickPoint);

            x += (separation + creditWidth);
        }
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getScreenCredits() == null || dc.getScreenCredits().size() < 1)
            return;

        Set<Map.Entry<ScreenCredit, Long>> credits = dc.getScreenCredits().entrySet();

        int y = dc.getDrawableHeight() - (bottomMargin + creditHeight / 2);
        int x = leftMargin + creditWidth / 2;

        for (Map.Entry<ScreenCredit, Long> entry : credits)
        {
            ScreenCredit credit = entry.getKey();
            Rectangle viewport = new Rectangle(x, y, creditWidth, creditHeight);

            credit.setViewport(viewport);
            if (entry.getValue() == dc.getFrameTimeStamp())
            {
                Object po = dc.getPickedObjects().getTopObject();
                credit.setOpacity(po != null && po instanceof ScreenCredit ? this.highlightOpacity : this.baseOpacity);
                credit.render(dc);
            }

            x += (separation + creditWidth);
        }
    }

    public void selected(SelectEvent event)
    {
        Object po = event.getTopObject();

        if (po != null && po instanceof ScreenCredit)
        {
            if (event.getEventAction().equals(SelectEvent.LEFT_DOUBLE_CLICK))
            {
                openBrowser((ScreenCredit) po);
            }
        }
    }

    private Set<String> badURLsReported = new HashSet<String>();

    protected void openBrowser(ScreenCredit credit)
    {
        if (credit.getLink() != null && credit.getLink().length() > 0)
        {
            try
            {
                BrowserOpener.browse(new URL(credit.getLink()));
            }
            catch (MalformedURLException e)
            {
                if (!badURLsReported.contains(credit.getLink())) // report it only once
                {
                    String msg = Logging.getMessage("generic.URIInvalid",
                        credit.getLink() != null ? credit.getLink() : "null");
                    Logging.logger().warning(msg);
                    badURLsReported.add(credit.getLink());
                }
            }
            catch (Exception e)
            {
                String msg = Logging.getMessage("generic.ExceptionAttemptingToInvokeWebBrower for URL",
                    credit.getLink());
                Logging.logger().warning(msg);
            }
        }
    }
}
