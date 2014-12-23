/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.Hashtable;

/**
 * @author dcollins
 * @version $Id: InstalledData.java 12530 2009-08-29 17:55:54Z jterhorst $
 */
public class InstalledData extends ApplicationTemplate
{
    public static final String DATA_SOURCE_DESCRIPTOR = "DataSourceDescriptor";
    public static final String ACTION_COMMAND_VERTICAL_EXAGGERATION = "ActionCommandVerticalExaggeration";

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        private Controller controller;
        private LayerPanel layerPanel;
        private InstalledDataPanel installedDataPanel;
        private JFileChooser installDataFileChooser;

        public AppFrame()
        {
            super(true, false, false);
            this.controller = new Controller(this);
            this.makeComponents();
            this.makeDataFileChoosers();
            this.layoutComponents();
            this.controller.refreshInstalled();
        }

        public Controller getController()
        {
            return this.controller;
        }

        public LayerPanel getLayerPanel()
        {
            return this.layerPanel;
        }

        public InstalledDataPanel getInstalledDataPanel()
        {
            return this.installedDataPanel;
        }

        public JFileChooser getInstallDataFileChooser()
        {
            return this.installDataFileChooser;
        }

        private void makeDataFileChoosers()
        {
            DataStoreProducerFilter filter = new TiledRasterProducerFilter(TiledImageProducer.class);
            this.installDataFileChooser.addChoosableFileFilter(filter);

            filter = new TiledRasterProducerFilter(TiledElevationProducer.class);
            this.installDataFileChooser.addChoosableFileFilter(filter);

            filter = new DataStoreProducerFilter(WWDotNetLayerSetInstaller.class);
            this.installDataFileChooser.addChoosableFileFilter(filter);            
        }

        private void makeComponents()
        {
            WorldWindow wwd = this.getWwd();

            ElevationModel defaultModel = wwd.getModel().getGlobe().getElevationModel();
            CompoundElevationModel compoundModel = new CompoundElevationModel();
            compoundModel.addElevationModel(defaultModel);
            wwd.getModel().getGlobe().setElevationModel(compoundModel);

            this.layerPanel = new LayerPanel(wwd);
            this.installedDataPanel = new InstalledDataPanel();
            this.installedDataPanel.addActionListener(this.controller);

            this.installedDataPanel.getPropertiesButton().setEnabled(false);

            this.installDataFileChooser = new JFileChooser();
            this.installDataFileChooser.setMultiSelectionEnabled(true);
            this.installDataFileChooser.setAcceptAllFileFilterUsed(false);
        }

        private void layoutComponents()
        {
            JPanel panel = new JPanel(new BorderLayout());
            {
                panel.setBorder(new EmptyBorder(10, 0, 10, 0));
                JPanel controlPanel = new JPanel(new BorderLayout(0, 10));
                controlPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

                JPanel vePanel = new JPanel(new BorderLayout(0, 5));
                {
                    JLabel label = new JLabel("Vertical Exaggeration");
                    vePanel.add(label, BorderLayout.NORTH);

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
                            ActionEvent ae = new ActionEvent(ve, 0, ACTION_COMMAND_VERTICAL_EXAGGERATION);
                            controller.actionPerformed(ae);
                        }
                    });
                    vePanel.add(slider, BorderLayout.SOUTH);
                }
                controlPanel.add(vePanel, BorderLayout.SOUTH);
                controlPanel.add(vePanel, BorderLayout.SOUTH);

                panel.add(controlPanel, BorderLayout.SOUTH);

                this.layerPanel = new LayerPanel(this.getWwd(), null);
                panel.add(this.layerPanel, BorderLayout.CENTER);
            }

            this.getContentPane().add(panel, BorderLayout.WEST);
            this.getContentPane().add(this.installedDataPanel, BorderLayout.SOUTH);
        }
    }

    public static class DataStoreProducerFilter extends FileFilter
    {
        private Class<? extends DataStoreProducer> producerClass;
        private AVList parameters;

        public DataStoreProducerFilter(Class<? extends DataStoreProducer> producerClass, AVList parameters)
        {
            this.producerClass = producerClass;
            this.parameters = parameters;
        }

        public DataStoreProducerFilter(Class<? extends DataStoreProducer> producerClass)
        {
            this.producerClass = producerClass;
            this.parameters = new AVListImpl();
        }

        public DataStoreProducer createProducer() throws Exception
        {
            return this.producerClass.newInstance();
        }

        public Class<? extends DataStoreProducer> getProducerClass()
        {
            return this.producerClass;
        }

        public AVList getParameters()
        {
            return this.parameters;
        }

        public boolean accept(java.io.File f)
        {
            //noinspection SimplifiableIfStatement
            if (f.isDirectory())
                return true;

            DataStoreProducer producer;
            try
            {
                producer = this.createProducer();
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "ExceptionCreatingProducer", e);
                return false;
            }

            DataSource source = new BasicDataSource(f);
            return producer.acceptsDataSource(source);
        }

        public String getDescription()
        {
            DataStoreProducer producer;
            try
            {
                producer = this.createProducer();
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "ExceptionCreatingProducer", e);
                return null;
            }

            return producer.getDataSourceDescription();
        }

        public Object showControls(java.awt.Component component, java.io.File[] selectedFiles)
        {
            return this;
        }
    }

    public static class TiledRasterProducerFilter extends DataStoreProducerFilter
    {
        public TiledRasterProducerFilter(Class<? extends DataStoreProducer> producerClass, AVList parameters)
        {
            super(producerClass, parameters);
        }

        public TiledRasterProducerFilter(Class<? extends DataStoreProducer> producerClass)
        {
            super(producerClass);
        }

        public Object showControls(java.awt.Component component, java.io.File[] selectedFiles)
        {
            Object o = JOptionPane.showInputDialog(component, "Name:", null, JOptionPane.QUESTION_MESSAGE, null, null,
                "New Data Set");
            if (o == null)
                return null;

            this.getParameters().setValue(AVKey.DATASET_NAME, o.toString());
            this.getParameters().setValue(AVKey.DATA_CACHE_NAME, o.toString());

            return o;
        }
    }

    public static class Controller implements ActionListener
    {
        private AppFrame appFrame;

        public Controller(AppFrame appFrame)
        {
            this.appFrame = appFrame;
        }

        public AppFrame getAppFrame()
        {
            return this.appFrame;
        }

        public void actionPerformed(ActionEvent event)
        {
            if (InstalledDataPanel.REFRESH_ACTION_COMMAND.equals(event.getActionCommand()))
            {
                this.refreshInstalled();
            }
            else if (InstalledDataPanel.INSTALL_ACTION_COMMAND.equals(event.getActionCommand()))
            {
                this.selectAndInstall();
            }
            else if (InstalledDataPanel.UNINSTALL_ACTION_COMMAND.equals(event.getActionCommand()))
            {
                this.uninstallSelected();
            }
            else if (InstalledDataPanel.PROPERTIES_ACTION_COMMAND.equals(event.getActionCommand()))
            {

            }
            else if (InstalledDataPanel.ZOOM_ACTION_COMMAND.equals(event.getActionCommand()))
            {
                this.zoomToSelected();
            }
            else if (ACTION_COMMAND_VERTICAL_EXAGGERATION.equals(event.getActionCommand()))
            {
                Double ve = (Double) event.getSource();
                this.setVerticalExaggeration(ve);
            }
        }

        public String getInstallLocation()
        {
            java.io.File installLocation = null;
            for (java.io.File f : WorldWind.getDataFileStore().getLocations())
            {
                if (WorldWind.getDataFileStore().isInstallLocation(f.getPath()))
                {
                    installLocation = f;
                    break;
                }
            }
            return installLocation != null ? installLocation.getPath() : null;
        }

        public void synchronizeInstalledData(Iterable<DataDescriptor> installed)
        {
            LayerList layers = this.appFrame.getWwd().getModel().getLayers();
            CompoundElevationModel elevationModel =
                (CompoundElevationModel) this.appFrame.getWwd().getModel().getGlobe().getElevationModel();
            this.synchronizeLayers(installed, layers);
            this.synchronizeElevationModel(installed, elevationModel);
            this.appFrame.getLayerPanel().update(this.appFrame.getWwd());
        }

        public void refreshInstalled()
        {
            String installPath = this.getInstallLocation();
            this.appFrame.getInstalledDataPanel().update(installPath);

            Iterable<DataDescriptor> descriptors = this.appFrame.getInstalledDataPanel().getDataDescriptors();
            if (descriptors != null)
                this.synchronizeInstalledData(descriptors);
        }

        public void selectAndInstall()
        {
            String installLocation = this.getInstallLocation();
            if (installLocation == null || installLocation.length() < 1)
            {
                String message = "Controller.CannotFindInstallLocation";
                Logging.logger().severe(message);
                JOptionPane.showMessageDialog(this.appFrame,
                    "Cannot find install location", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser fc = this.appFrame.getInstallDataFileChooser();

            int retVal = fc.showOpenDialog(this.appFrame);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            FileFilter filter = fc.getFileFilter();
            if (filter == null || !(filter instanceof DataStoreProducerFilter))
            {
                String message = "Controller.InvalidFileFilter";
                Logging.logger().severe(message);
                JOptionPane.showMessageDialog(this.appFrame,
                    "Invalid file selection type", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            java.io.File[] selected = fc.getSelectedFiles();
            if (selected == null)
            {
                String message = "Controller.NoFileSelected";
                Logging.logger().severe(message);
                JOptionPane.showMessageDialog(this.appFrame,
                    "No file was selected", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            DataStoreProducerFilter producerFilter = ((DataStoreProducerFilter) filter);
            producerFilter.getParameters().clearList();

            Object o = producerFilter.showControls(this.appFrame, selected);
            if (o == null)
                return;

            DataSource[] sources = new DataSource[selected.length];
            for (int i = 0; i < selected.length; i++)
                sources[i] = new BasicDataSource(selected[i]);

            try
            {
                final DataStoreProducer producer = producerFilter.createProducer();
                final AVList parmeters = producerFilter.getParameters();
                Installer installer = new Installer(this, producer, parmeters, sources, installLocation);
                installer.installInNonUIThread(new Runnable() {
                    public void run() {
                        Controller.this.zoomToProductionResults(producer);
                    }
                });
            }
            catch (Exception e)
            {
                String message = "Controller.ExceptionCreatingInstaller: " + producerFilter.getProducerClass();
                Logging.logger().severe(message);
                JOptionPane.showMessageDialog(this.appFrame,
                    "Cannot create installer " + producerFilter.getProducerClass(), "Error", JOptionPane.ERROR_MESSAGE);   
            }
        }

        public void uninstallSelected()
        {
            Iterable<DataDescriptor> selected = this.appFrame.getInstalledDataPanel().getSelectedDataDescriptors();
            if (selected == null)
                return;

            Uninstaller uninstaller = new Uninstaller(this, selected);
            uninstaller.uninstallInNonUIThread();
        }

        public void zoomToSelected()
        {
            Iterable<DataDescriptor> selected = this.appFrame.getInstalledDataPanel().getSelectedDataDescriptors();
            if (selected == null)
                return;

            Sector sector = null;
            for (DataDescriptor descriptor : selected)
            {
                Object o = descriptor.getValue(AVKey.SECTOR);
                if (o != null && o instanceof Sector)
                    sector = (sector == null ? (Sector) o : sector.union((Sector) o));
            }

            if (sector == null)
            {
                JOptionPane.showMessageDialog(this.appFrame,
                    "Select installed data to zoom to", null, JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            try
            {
                this.zoomTo(sector);
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(this.appFrame,
                    "Cannot zoom to selected data", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public void zoomToProductionResults(DataStoreProducer producer)
        {
            Iterable<?> results = producer.getProductionResults();
            if (results == null)
                return;

            Sector sector = null;
            for (Object result : results)
            {
                if (result instanceof AVList)
                {
                    Object o = ((AVList) result).getValue(AVKey.SECTOR);
                    if (o != null && o instanceof Sector)
                        sector = (sector == null ? (Sector) o : sector.union((Sector) o));
                }
            }

            if (sector == null)
                return;

            try
            {
                this.zoomTo(sector);
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(this.appFrame,
                    "Cannot zoom to selected data", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public void setVerticalExaggeration(double verticalExaggeration)
        {
            this.getAppFrame().getWwd().getSceneController().setVerticalExaggeration(verticalExaggeration);
        }

        private void zoomTo(Sector sector)
        {
            if (sector == null)
            {
                String message = Logging.getMessage("nullValue.SectorIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            View view = this.appFrame.getWwd().getView();
            if (view == null)
            {
                String message = Logging.getMessage("nullValue.ViewIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            Globe globe = this.appFrame.getWwd().getModel().getGlobe();
            if (globe == null)
            {
                String message = Logging.getMessage("nullValue.GlobeIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            double ve = this.appFrame.getWwd().getSceneController().getVerticalExaggeration();
            Extent extent = globe.computeBoundingCylinder(ve, sector);

            Position centerPos = globe.computePositionFromPoint(extent.getCenter());
            double zoomDistance = 1.5 * extent.getRadius() / view.getFieldOfView().tanHalfAngle();

            ((BasicOrbitView) view).addPanToAnimator(
                centerPos, Angle.ZERO, Angle.ZERO, zoomDistance, true);

        }

        private void synchronizeLayers(Iterable<DataDescriptor> descriptors, LayerList layerList)
        {
            java.util.List<DataDescriptor> installedDescriptors = new java.util.ArrayList<DataDescriptor>();
            for (DataDescriptor d : descriptors)
                installedDescriptors.add(d);

            java.util.List<Layer> uninstalledLayers = new java.util.ArrayList<Layer>();

            // Remove layers with DataDescriptors that are no longer installed, and remove DataDescriptors that
            // are already installed as a layer.
            for (Layer layer : layerList)
            {
                Object o = layer.getValue(DATA_SOURCE_DESCRIPTOR);
                if (o != null && o instanceof DataDescriptor)
                {
                    DataDescriptor d = (DataDescriptor) o;
                    // If the layer references an installed DataDesriptor, then we can eliminate it from the list
                    // of new DataDescriptors.
                    if (installedDescriptors.contains(d))
                        installedDescriptors.remove(d);
                    // If the layer references a DataDescriptor that is no longer installed,
                    // then remove that layer.
                    else
                        uninstalledLayers.add(layer);
                }
            }

            // Remove layers for uninstalled DataDescriptors.
            for (Layer layer : uninstalledLayers)
                layerList.remove(layer);

            // Add layers for installedDataDescriptors.
            for (DataDescriptor d : installedDescriptors)
                this.createLayerForDescriptor(d);
        }

        private void synchronizeElevationModel(Iterable<DataDescriptor> descriptors,
                                               CompoundElevationModel compoundModel)
        {
            java.util.List<DataDescriptor> installedDescriptors = new java.util.ArrayList<DataDescriptor>();
            for (DataDescriptor d : descriptors)
                installedDescriptors.add(d);

            java.util.List<ElevationModel> modelList = compoundModel.getElevationModels();
            //java.util.List<ElevationModel> uninstalledModels = new java.util.ArrayList<ElevationModel>();

            // Remove models with DataDescriptors that are no longer installed, and remove DataDescriptors that
            // are already installed as a model.
            for (ElevationModel model : modelList)
            {
                Object o = model.getValue(DATA_SOURCE_DESCRIPTOR);
                if (o != null && o instanceof DataDescriptor)
                {
                    DataDescriptor d = (DataDescriptor) o;
                    // If the layer references an installed DataDesriptor, then we can eliminate it from the list
                    // of new DataDescriptors.
                    if (installedDescriptors.contains(d))
                        installedDescriptors.remove(d);
                    // If the layer references a DataDescriptor that is no longer installed,
                    // then remove that layer.
                    //else
                    //    uninstalledModels.add(model);
                }
            }

            // Remove layers for uninstalled DataDescriptors.
            //for (ElevationModel model : uninstalledModels)
            //    compoundModel.removeElevationModel(model);

            // Add layers for installedDataDescriptors.
            for (DataDescriptor d : installedDescriptors)
                this.createElevationModelForDescriptor(d, compoundModel);
        }

        private void createLayerForDescriptor(DataDescriptor descriptor)
        {
            if (AVKey.TILED_IMAGERY.equals(descriptor.getType()))
            {
                TiledImageLayer layer = new BasicTiledImageLayer(descriptor);
                layer.setNetworkRetrievalEnabled(false);
                layer.setValue(DATA_SOURCE_DESCRIPTOR, descriptor);
                if (descriptor.getName() != null)
                    layer.setName(descriptor.getName());

                ApplicationTemplate.insertBeforePlacenames(this.appFrame.getWwd(), layer);
            }
        }

        private void createElevationModelForDescriptor(DataDescriptor descriptor, CompoundElevationModel compoundModel)
        {
            if (AVKey.TILED_ELEVATIONS.equals(descriptor.getType()))
            {
                double HEIGHT_OF_MT_EVEREST = 8850d; // meters
                double DEPTH_OF_MARIANAS_TRENCH = -11000d; // meters

                descriptor.setValue(AVKey.ELEVATION_MAX, HEIGHT_OF_MT_EVEREST);
                descriptor.setValue(AVKey.ELEVATION_MIN, DEPTH_OF_MARIANAS_TRENCH);
                ElevationModel model = new BasicElevationModel(descriptor);
                model.setNetworkRetrievalEnabled(false);
                model.setValue(DATA_SOURCE_DESCRIPTOR, descriptor);

                compoundModel.addElevationModel(model);
            }
        }
    }

    public static class Installer extends WWObjectImpl
    {
        // UI components.
        private Controller controller;
        private ProgressMonitor installProgressMonitor;
        // Installation components.
        private DataStoreProducer producer;
        private AVList parameters;
        private DataSource[] dataSources;
        private String installLocation;

        public Installer(Controller controller,
                         DataStoreProducer producer, AVList parameters, DataSource[] dataSources,
                         String installLocation)
        {
            this.controller = controller;
            this.producer = producer;
            this.parameters = parameters;
            this.dataSources = dataSources;
            this.installLocation = installLocation;
        }

        public void propertyChange(final PropertyChangeEvent evt)
        {
            if (evt == null)
                return;

            if (AVKey.PROGRESS.equalsIgnoreCase(evt.getPropertyName()))
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Installer.this.updateInstallProgress(evt.getNewValue());
                    }
                });
            }
        }

        public void install() throws Exception
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Installer.this.beginInstall();
                }
            });

            try
            {
                this.parameters.setValue(AVKey.FILE_STORE_LOCATION, this.installLocation);

                this.producer.setStoreParameters(this.parameters);
                this.producer.removeAllDataSources();
                this.producer.offerAllDataSources(java.util.Arrays.asList(this.dataSources));

                producer.startProduction();
            }
            finally
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        Installer.this.endInstall();
                    }
                });
            }
        }

        public void installInNonUIThread(final Runnable onFinished)
        {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try
                    {
                        Installer.this.install();
                    }
                    catch (Exception e)
                    {
                        String message = "Controller.ExceptionWhileInstalling";
                        Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                        JOptionPane.showMessageDialog(Installer.this.controller.getAppFrame(),
                                e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }

                    if (onFinished != null)
                        SwingUtilities.invokeLater(onFinished);
                }
            });
            thread.start();
        }

        private void beginInstall()
        {
            this.producer.addPropertyChangeListener(this);

            this.controller.getAppFrame().getInstalledDataPanel().getInstallButton().setEnabled(false);
            this.controller.getAppFrame().getInstalledDataPanel().getUninstallButton().setEnabled(false);
            this.controller.getAppFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        private void endInstall()
        {
            this.producer.removePropertyChangeListener(this);

            if (this.installProgressMonitor != null)
                this.installProgressMonitor.close();
            this.installProgressMonitor = null;

            this.controller.getAppFrame().getInstalledDataPanel().getInstallButton().setEnabled(true);
            this.controller.getAppFrame().getInstalledDataPanel().getUninstallButton().setEnabled(true);
            this.controller.getAppFrame().setCursor(Cursor.getDefaultCursor());

            this.controller.getAppFrame().getController().refreshInstalled();
        }

        private void updateInstallProgress(Object newValue)
        {
            if (this.installProgressMonitor == null)
            {
                String message = "Installing...";
                this.installProgressMonitor = new ProgressMonitor(this.controller.getAppFrame(), message, null, 0, 100);
                this.installProgressMonitor.setMillisToDecideToPopup(100);
                this.installProgressMonitor.setMillisToPopup(0);
            }

            if (newValue != null && newValue instanceof Number)
            {
                double progress = ((Number) newValue).doubleValue();
                if (progress < 1.0)
                {
                    int percentComplete = (int) (100 * progress);
                    String note;
                    if (this.parameters.getStringValue(AVKey.DATASET_NAME) != null)
                        note = String.format("%s %02d%%", this.parameters.getStringValue(AVKey.DATASET_NAME), percentComplete);
                    else
                        note = String.format("%02d%%", percentComplete);

                    this.installProgressMonitor.setNote(note);
                    this.installProgressMonitor.setProgress(percentComplete);
                }
                else
                {
                    this.installProgressMonitor.close();
                    this.installProgressMonitor = null;
                }
            }
        }
    }

    public static class Uninstaller extends WWObjectImpl
    {
        private Controller controller;
        private Iterable<DataDescriptor> descriptors;

        public Uninstaller(Controller controller, Iterable<DataDescriptor> descriptors)
        {
            this.controller = controller;
            this.descriptors = descriptors;
        }

        public void uninstall()
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Uninstaller.this.beginUninstall();
                }
            });

            for (DataDescriptor descriptor : descriptors)
                this.uninstall(descriptor);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Uninstaller.this.endUninstall();
                }
            });
        }

        public void uninstallInNonUIThread()
        {
            Thread thread = new Thread(new Runnable()
            {
                public void run()
                {
                    Uninstaller.this.uninstall();
                }
            });
            thread.start();
        }

        private void uninstall(DataDescriptor descriptor)
        {
            java.io.File storeLocation = descriptor.getFileStoreLocation();
            String storePath = descriptor.getFileStorePath();

            java.io.File installLocation = new java.io.File(storeLocation, storePath);

            try
            {
                WWIO.deleteDirectory(installLocation);
            }
            catch (java.io.IOException e)
            {
                String message = "Controller.ExceptionWhileUninstalling";
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                JOptionPane.showMessageDialog(this.controller.getAppFrame(),
                    "Error during uninstallation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void beginUninstall()
        {
            this.controller.getAppFrame().getInstalledDataPanel().getInstallButton().setEnabled(false);
            this.controller.getAppFrame().getInstalledDataPanel().getUninstallButton().setEnabled(false);
            this.controller.getAppFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        private void endUninstall()
        {
            this.controller.getAppFrame().getInstalledDataPanel().getInstallButton().setEnabled(true);
            this.controller.getAppFrame().getInstalledDataPanel().getUninstallButton().setEnabled(true);
            this.controller.getAppFrame().setCursor(Cursor.getDefaultCursor());

            this.controller.getAppFrame().getController().refreshInstalled();
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Installed Data", AppFrame.class);
    }
}
