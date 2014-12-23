/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * @author Patrick Murris
 * @version $Id: WWJDemo.java 12689 2009-10-04 17:42:00Z patrickmurris $
 */
public class WWJDemo extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            // Add some layers
            this.setupLayers();

            // Add vertical exaggeration slider panel
            this.getLayerPanel().add(makeVEControlPanel(), BorderLayout.SOUTH);
        }

        private void setupLayers()
        {
            // Add some imagery layers - layer class, enabled, opacity
            this.setupLayer(MSVirtualEarthLayer.class, false, 1);
            this.setupLayer(USDANAIPWMSImageLayer.class, false, 1);
            this.setupLayer(USGSDigitalOrtho.class, false, .6);
            this.setupLayer(USGSUrbanAreaOrtho.class, false, 1);
            this.setupLayer(USGSTopographicMaps.class, false, .7);
            this.setupLayer(OpenStreetMapLayer.class, false, 1);
            this.setupLayer(LatLonGraticuleLayer.class, false, .7);
            this.setupLayer(UTMGraticuleLayer.class, false, .7);
            this.setupLayer(MGRSGraticuleLayer.class, false, .7);

            // Add terrain profile layer
            Layer layer = this.setupLayer(TerrainProfileLayer.class, false, 1);
            if (layer != null)
            {
                TerrainProfileLayer tpl = (TerrainProfileLayer)layer;
                tpl.setEventSource(this.getWwd());
                tpl.setZeroBased(false);
            }

            // Add view controls
            layer = this.setupLayer(ViewControlsLayer.class, true, 1);
            if (layer != null)
            {
                ViewControlsLayer vcl = (ViewControlsLayer)layer;
                this.getWwd().addSelectListener(new ViewControlsSelectListener(this.getWwd(), vcl));
                vcl.setPosition(AVKey.NORTHEAST);
                vcl.setLocationOffset(new Vec4(0, -70, 0));
                vcl.setLayout(AVKey.VERTICAL);
                vcl.setShowVeControls(false);
            }

            // Make compass smaller
            layer = this.findLayer(CompassLayer.class);
            if (layer != null)
                ((CompassLayer)layer).setIconScale(.25);

            this.getLayerPanel().update(this.getWwd());
        }

        private Layer setupLayer(Class layerClass, boolean enabled, double opacity)
        {
            Layer layer = this.findLayer(layerClass);
            if (layer == null)
            {
                // Add layer to the layer list if not already in
                try
                {
                    layer = (Layer)layerClass.newInstance();
                    insertBeforePlacenames(this.getWwd(), layer);
                }
                catch (Exception e)
                {
                    return null;
                }
            }
            layer.setEnabled(enabled);
            layer.setOpacity(opacity);
            return layer;
        }

        private Layer findLayer(Class layerClass)
        {
            for (Layer layer : this.getWwd().getModel().getLayers())
                if (layer.getClass().equals(layerClass))
                    return layer;

            return null;
        }

        private JPanel makeVEControlPanel()
        {
            JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
            controlPanel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9),
                            new TitledBorder("Vertical Exaggeration")));

            JPanel vePanel = new JPanel(new BorderLayout(0, 5));
            {
                int MIN_VE = 1;
                int MAX_VE = 8;
                int curVe = (int) this.getWwd().getSceneController().getVerticalExaggeration();
                curVe = curVe < MIN_VE ? MIN_VE : (curVe > MAX_VE ? MAX_VE : curVe);
                JSlider slider = new JSlider(MIN_VE, MAX_VE, curVe);
                slider.setMajorTickSpacing(1);
                slider.setPaintTicks(true);
                slider.setSnapToTicks(true);
                Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
                labelTable.put(1, new JLabel("1x"));
                labelTable.put(2, new JLabel("2x"));
                labelTable.put(4, new JLabel("4x"));
                labelTable.put(8, new JLabel("8x"));
                slider.setLabelTable(labelTable);
                slider.setPaintLabels(true);
                slider.addChangeListener(new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        double ve = ((JSlider) e.getSource()).getValue();
                        getWwd().getSceneController().setVerticalExaggeration(ve);
                    }
                });
                vePanel.add(slider, BorderLayout.SOUTH);
            }
            controlPanel.add(vePanel, BorderLayout.SOUTH);
            return controlPanel;
        }

    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("NASA World Wind", AppFrame.class);
    }
}
