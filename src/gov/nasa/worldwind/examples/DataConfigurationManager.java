/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.util.Logging;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author dcollins
 * @version $Id: DataConfigurationManager.java 11381 2009-06-01 18:34:30Z dcollins $
 */
public class DataConfigurationManager extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame implements ActionListener, PropertyChangeListener
    {
        protected ActionController controller;
        protected DataConfigFrame configFrame;

        public AppFrame()
        {
            this.initComponents();

            this.controller = new ActionController(this);
            this.configFrame = new DataConfigFrame(WorldWind.getDataFileStore(), this.getWwd());

            this.configFrame.addPropertyChangeListener(this);
            this.controller.setFileStoreConfigVisible(true);
        }

        public DataConfigFrame getConfigFrame()
        {
            return this.configFrame;
        }

        public void actionPerformed(ActionEvent e)
        {
            if (e == null)
                return;

            if (this.controller == null)
                return;

            this.controller.actionPerformed(e);
        }

        public void propertyChange(PropertyChangeEvent e)
        {
            if (e == null)
                return;

            if (this.controller == null)
                return;

            this.controller.propertyChange(e);
        }

        protected void initComponents()
        {
            JMenuBar menuBar = new JMenuBar();
            {
                JMenu menu = new JMenu("Window");
                JMenuItem item = new JMenuItem("Show Data Configurations");
                item.setActionCommand("FileStoreConfigVisible");
                item.addActionListener(this);
                menu.add(item);
                menuBar.add(menu);
            }
            this.setJMenuBar(menuBar);
        }
    }

    public static class DataConfigFrame extends JFrame implements PropertyChangeListener
    {
        protected DataConfigurationPanel dataConfigPanel;

        public DataConfigFrame(FileStore fileStore, WorldWindow worldWindow) throws HeadlessException
        {
            if (fileStore == null)
            {
                String msg = Logging.getMessage("nullValue.FileStoreIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            this.initComponents(fileStore, worldWindow);
            this.layoutComponents();
            this.setTitle("Data Configurations");
        }

        public void propertyChange(PropertyChangeEvent e)
        {
            if (e == null)
                return;

            this.firePropertyChange(e.getPropertyName(), e.getOldValue(), e.getNewValue());
        }

        protected void initComponents(FileStore fileStore, WorldWindow worldWindow)
        {
            this.dataConfigPanel = new DataConfigurationPanel(fileStore, worldWindow);
            this.dataConfigPanel.addPropertyChangeListener(this);
        }

        protected void layoutComponents()
        {
            this.getContentPane().setLayout(new BorderLayout(0, 0)); // hgap, vgap
            this.getContentPane().add(this.dataConfigPanel, BorderLayout.CENTER);

            this.setPreferredSize(new Dimension(400, 500));
            this.validate();
            this.pack();
        }
    }

    public static class ActionController implements ActionListener, PropertyChangeListener
    {
        protected AppFrame appFrame;

        public ActionController(AppFrame appFrame)
        {
            this.appFrame = appFrame;
        }

        public void actionPerformed(ActionEvent e)
        {
            if (e == null || e.getActionCommand() == null)
                return;

            if (e.getActionCommand().equals("FileStoreConfigVisible"))
            {
                this.setFileStoreConfigVisible(true);
            }
        }

        public void propertyChange(PropertyChangeEvent e)
        {
            if (e == null || e.getPropertyName() == null)
                return;

            if (e.getPropertyName().equals("LayersPanelUpdated"))
            {
                this.appFrame.getLayerPanel().update(this.appFrame.getWwd());
            }
        }

        public void setFileStoreConfigVisible(boolean visible)
        {
            if (this.appFrame == null)
                return;

            this.appFrame.getConfigFrame().setVisible(visible);
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Data Configuration", AppFrame.class);
    }
}
