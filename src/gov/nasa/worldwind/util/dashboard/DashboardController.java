/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util.dashboard;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: DashboardController.java 11460 2009-06-05 08:23:56Z tgaskins $
 */
public class DashboardController implements MouseListener, Disposable
{
    private DashboardDialog dialog;
    private Component component;
    private WorldWindow wwd;

    public DashboardController(WorldWindow wwd, Component component)
    {
        this.wwd = wwd;
        this.component = component;
        wwd.getInputHandler().addMouseListener(this);
    }

    public void dispose()
    {
        this.dialog.dispose();
        this.dialog = null;

        this.wwd.getInputHandler().removeMouseListener(this);
        this.wwd = null;

        this.component = null;
    }

    public void raiseDialog()
    {
        if (this.dialog == null)
        {
            this.dialog = new DashboardDialog(getParentFrame(this.component), wwd);
        }

        this.dialog.raiseDialog();
    }

    public void lowerDialog()
    {
        this.dialog.lowerDialog();
    }

    private Frame getParentFrame(Component comp)
    {
        return comp != null ? (Frame) SwingUtilities.getAncestorOfClass(Frame.class, comp) : null;
    }

    public void mouseClicked(MouseEvent e)
    {
        if ((e.getButton() == MouseEvent.BUTTON1
            && (e.getModifiers() & ActionEvent.CTRL_MASK) != 0
            && (e.getModifiers() & ActionEvent.ALT_MASK) != 0
            && (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0))
            raiseDialog();
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }
}
