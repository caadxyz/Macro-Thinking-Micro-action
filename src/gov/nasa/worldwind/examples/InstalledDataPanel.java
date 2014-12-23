/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.DataDescriptor;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * @author dcollins
 * @version $Id: InstalledDataPanel.java 7393 2008-11-07 00:12:17Z dcollins $
 */
public class InstalledDataPanel extends JPanel
{
    public static final String REFRESH_ACTION_COMMAND = "InstalledDataPanel.RefreshActionCommand";
    public static final String INSTALL_ACTION_COMMAND = "InstalledDataPanel.InstallActionCommand";
    public static final String UNINSTALL_ACTION_COMMAND = "InstalledDataPanel.UninstallActionCommand";
    public static final String PROPERTIES_ACTION_COMMAND = "InstalledDataPanel.PropertiesActionCommand";
    public static final String ZOOM_ACTION_COMMAND = "InstalledDataPanel.ZoomActionCommand";

    protected static final String COLUMN_INSTALLED = "Installed";
    protected static final String COLUMN_NAME = "Name";
    protected static final String COLUMN_DATA_STORE_LOCATION = "Data store location";
    protected static final String COLUMN_DATA_STORE_PATH = "Path";

    protected static final int ROW_OBJECT = -1;

    private EventListenerList listenerList;
    private ActionListener actionListenerDelegate;
    // ToolBar components.
    private JToolBar toolBar;
    private JButton refreshButton;
    private JButton installButton;
    private JButton uninstallButton;
    private JButton propertiesButton;
    private JButton zoomButton;
    // Table components.
    private DataDescriptorTableModel tableModel;
    private JTable table;
    private JScrollPane scrollPane;

    public InstalledDataPanel()
    {
        this.listenerList = new EventListenerList();
        this.actionListenerDelegate = new ActionListenerDelegate(this);
        this.makeComponents();
        this.layoutComponents();
    }

    public void update(String installPath)
    {
        this.fill(installPath);
        this.revalidate();
        this.repaint();
    }

    public JButton getRefreshButton()
    {
        return this.refreshButton;
    }

    public JButton getInstallButton()
    {
        return this.installButton;
    }

    public JButton getUninstallButton()
    {
        return this.uninstallButton;
    }

    public JButton getPropertiesButton()
    {
        return this.propertiesButton;
    }

    public JButton getZoomButton()
    {
        return this.zoomButton;
    }

    public Iterable<DataDescriptor> getDataDescriptors()
    {
        java.util.List<DataDescriptor> objects = new java.util.ArrayList<DataDescriptor>();

        Object o;
        int rowCount = this.table.getRowCount();
        for (int row = 0; row < rowCount; row++)
            if ((o = this.table.getModel().getValueAt(row, ROW_OBJECT)) != null)
                if (o instanceof DataDescriptor)
                    objects.add((DataDescriptor) o);

        return objects;
    }

    public Iterable<DataDescriptor> getSelectedDataDescriptors()
    {
        java.util.List<DataDescriptor> selectedObjects = new java.util.ArrayList<DataDescriptor>();

        int[] selectedRows = this.table.getSelectedRows();
        if (selectedRows != null)
        {
            Object o;
            for (int row : selectedRows)
                if ((o = this.table.getModel().getValueAt(row, ROW_OBJECT)) != null)
                    if (o instanceof DataDescriptor)
                        selectedObjects.add((DataDescriptor) o);
        }

        return selectedObjects;
    }

    public ActionListener[] getActionListeners()
    {
        return this.listenerList.getListeners(ActionListener.class);
    }

    public void addActionListener(ActionListener listener)
    {
        this.listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener)
    {
        this.listenerList.remove(ActionListener.class, listener);
    }

    protected JTable getTable()
    {
        return this.table;
    }

    protected JToolBar getToolBar()
    {
        return this.toolBar;
    }

    protected void fill(String installPath)
    {
        // Clear any DataDescriptors from the table.
        this.tableModel.setRowValues(null);

        FileStore fileStore = WorldWind.getDataFileStore();
        if (fileStore == null)
            return;

        java.util.List<? extends DataDescriptor> dataDescriptors = fileStore.findDataDescriptors(installPath);
        this.tableModel.setRowValues(dataDescriptors);
    }

    protected void makeComponents()
    {
        this.toolBar = new JToolBar();

        this.refreshButton = new JButton("Refresh");
        this.installButton= new JButton("Install new data");
        this.uninstallButton  = new JButton("Uninstall selected");
        this.propertiesButton = new JButton("Properties");
        this.zoomButton = new JButton("Zoom to selected");

        this.refreshButton.setActionCommand(REFRESH_ACTION_COMMAND);
        this.installButton.setActionCommand(INSTALL_ACTION_COMMAND);
        this.uninstallButton.setActionCommand(UNINSTALL_ACTION_COMMAND);
        this.propertiesButton.setActionCommand(PROPERTIES_ACTION_COMMAND);
        this.zoomButton.setActionCommand(ZOOM_ACTION_COMMAND);

        this.refreshButton.addActionListener(this.actionListenerDelegate);
        this.installButton.addActionListener(this.actionListenerDelegate);
        this.uninstallButton.addActionListener(this.actionListenerDelegate);
        this.propertiesButton.addActionListener(this.actionListenerDelegate);
        this.zoomButton.addActionListener(this.actionListenerDelegate);

        this.toolBar.add(this.refreshButton);
        this.toolBar.add(this.installButton);
        this.toolBar.add(this.uninstallButton);
        this.toolBar.add(this.propertiesButton);
        this.toolBar.add(this.zoomButton);
        
        this.tableModel = new DataDescriptorTableModel();
        this.setupTableModel(this.tableModel);
        this.table = new JTable(this.tableModel);
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.table.setColumnSelectionAllowed(false);
        this.scrollPane = new JScrollPane(this.table);
    }

    protected void layoutComponents()
    {
        this.setLayout(new BorderLayout());
        this.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Installed Data")));

        this.toolBar.setFloatable(false);
        this.toolBar.setRollover(true);
        this.toolBar.setBackground(this.toolBar.getBackground().darker());
        this.add(this.toolBar, BorderLayout.NORTH);

        this.table.getColumn(COLUMN_INSTALLED).setMaxWidth(60);
        this.table.getColumn(COLUMN_NAME).setPreferredWidth(200);
        this.table.getColumn(COLUMN_DATA_STORE_LOCATION).setPreferredWidth(100);
        this.table.getColumn(COLUMN_DATA_STORE_PATH).setPreferredWidth(100);
        this.scrollPane.setPreferredSize(new Dimension(this.scrollPane.getPreferredSize().width, 160));
        this.add(this.scrollPane, BorderLayout.CENTER);
    }

    private void setupTableModel(DataDescriptorTableModel tableModel)
    {
        java.util.List<String>   columnNames  = new ArrayList<String>();
        java.util.List<Class<?>> columnTypes  = new ArrayList<Class<?>>();
        java.util.List<String>   columnParams = new ArrayList<String>();

        columnNames.add(COLUMN_INSTALLED);
        columnTypes.add(Boolean.class);
        columnParams.add(AVKey.INSTALLED);

        columnNames.add(COLUMN_NAME);
        columnTypes.add(String.class);
        columnParams.add(AVKey.DATASET_NAME);

        columnNames.add(COLUMN_DATA_STORE_LOCATION);
        columnTypes.add(java.io.File.class);
        columnParams.add(AVKey.FILE_STORE_LOCATION);

        columnNames.add(COLUMN_DATA_STORE_PATH);
        columnTypes.add(String.class);
        columnParams.add(AVKey.DATA_CACHE_NAME);

        tableModel.setColumnNames(columnNames);
        tableModel.setColumnTypes(columnTypes);
        tableModel.setColumnParameters(columnParams);
    }

    private static class ActionListenerDelegate implements ActionListener
    {
        private InstalledDataPanel panel;

        public ActionListenerDelegate(InstalledDataPanel panel)
        {
            this.panel = panel;
        }

        public void actionPerformed(ActionEvent e)
        {
            // Guaranteed to return a non-null array
            Object[] listeners = this.panel.listenerList.getListenerList();
            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == ActionListener.class)
                {
                    ((ActionListener) listeners[i + 1]).actionPerformed(e);
                }
            }
        }
    }

    protected static class DataDescriptorTableModel extends AbstractTableModel
    {
        private java.util.List<String> columnNames  = new java.util.ArrayList<String>();
        private java.util.List<Class>  columnTypes  = new java.util.ArrayList<Class>();
        private java.util.List<String> columnParams = new java.util.ArrayList<String>();
        private java.util.List<DataDescriptor> rowValues = new java.util.ArrayList<DataDescriptor>();

        public DataDescriptorTableModel()
        {
        }

        public int getColumnCount()
        {
            return this.columnParams.size();
        }

        public int getRowCount()
        {
            return this.rowValues.size();
        }

        public String getColumnName(int columnIndex)
        {
            if (columnIndex < 0 || columnIndex >= this.columnNames.size())
                return super.getColumnName(columnIndex);

            return this.columnNames.get(columnIndex);
        }

        public Class<?> getColumnClass(int columnIndex)
        {
            if (columnIndex < 0 || columnIndex >= this.columnTypes.size())
                return Object.class;

            return this.columnTypes.get(columnIndex);
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (rowIndex < 0 || rowIndex >= this.rowValues.size())
                return null;

            DataDescriptor descriptor = this.rowValues.get(rowIndex);
            if (descriptor == null)
                return null;

            if (columnIndex == ROW_OBJECT)
                return descriptor;
            else if (columnIndex < 0 || columnIndex >= this.columnParams.size())
                return null;

            String parameter = this.columnParams.get(columnIndex);
            if (parameter == null)
                return null;

            return descriptor.getValue(parameter);
        }

        public java.util.List<String> getColumnNames()
        {
            return java.util.Collections.unmodifiableList(this.columnNames);
        }

        public void setColumnNames(Iterable<? extends String> newColumnNames)
        {
            this.columnNames.clear();
            if (newColumnNames != null)
                for (String name : newColumnNames)
                    this.columnNames.add(name);
            this.fireTableRowsUpdated(TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW);
        }

        public java.util.List<Class> getColumnTypes()
        {
            return java.util.Collections.unmodifiableList(this.columnTypes);
        }

        public void setColumnTypes(Iterable<Class<?>> newColumnTypes)
        {
            this.columnTypes.clear();
            if (newColumnTypes != null)
                for (Class cls : newColumnTypes)
                    this.columnTypes.add(cls);
            this.fireTableStructureChanged();
        }

        public java.util.List<String> getColumnParameters()
        {
            return java.util.Collections.unmodifiableList(this.columnParams);
        }

        public void setColumnParameters(Iterable<? extends String> newColumnParameters)
        {
            this.columnParams.clear();
            if (newColumnParameters != null)
                for (String parameter : newColumnParameters)
                    this.columnParams.add(parameter);
            this.fireTableStructureChanged();
        }

        public java.util.List<DataDescriptor> getRowValues()
        {
            return java.util.Collections.unmodifiableList(this.rowValues);
        }

        public void setRowValues(Iterable<? extends DataDescriptor> newRowValues)
        {
            this.rowValues.clear();
            if (newRowValues != null)
                for (DataDescriptor descriptor : newRowValues)
                    this.rowValues.add(descriptor);
            this.fireTableDataChanged();
        }
    }
}
