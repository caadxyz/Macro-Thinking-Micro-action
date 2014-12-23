package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.city.PolylineEntry;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;


public class PolygonBuilderModel extends AbstractTableModel
{
    private static String[] columnName = {"Name","length"};
    private static Class[]  columnClass = {String.class,String.class};
    private static String[] columnAttribute = {AVKey.DISPLAY_NAME,"cityPolygon.length"};
    private ArrayList<PolygonEntry> entryList = new ArrayList<PolygonEntry>();
    public PolygonBuilderModel()
    {
    }

    public String getColumnName(int columnIndex)
    {
        return columnName[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex)
    {
        return columnClass[columnIndex];
    }

    public int getRowCount()
    {
        return this.entryList.size();
    }

    public int getColumnCount()
    {
        return 2;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return true;
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        PolygonEntry entry = this.entryList.get(rowIndex);
        return entry.getValue(columnAttribute[columnIndex]);
    }

    public void setValueAt(Object aObject, int rowIndex, int columnIndex)
    {
        PolygonEntry entry = this.entryList.get(rowIndex);
        String key = columnAttribute[columnIndex];
        entry.setValue(key, aObject);
    }

    public java.util.List<PolygonEntry> getEntries()
    {
        return Collections.unmodifiableList(this.entryList);
    }

    public void setEntries(Iterable<? extends PolygonEntry> entries)
    {
        this.entryList.clear();
        if (entries != null)
        {
            for (PolygonEntry entry : entries)
            {
                this.entryList.add(entry);
            }
        }

        this.fireTableDataChanged();
    }

    public void addEntry(PolygonEntry entry)
    {
        this.entryList.add(entry);
        int index = this.entryList.size() - 1;
        this.fireTableRowsInserted(index, index);
    }

    public void removeEntry(PolygonEntry entry)
    {
        int index = this.entryList.indexOf(entry);
        if (index != -1)
        {
            this.entryList.remove(entry);
            this.fireTableRowsDeleted(index, index);
        }
    }

    public void removeAllEntries()
    {
        this.entryList.clear();
        this.fireTableDataChanged();
    }

    public PolygonEntry getEntry(int index)
    {
        return this.entryList.get(index);
    }

    public PolygonEntry setEntry(int index, PolygonEntry entry)
    {
        return this.entryList.set(index, entry);
    }

    public int getIndexForEntry(PolygonEntry entry)
    {
        return this.entryList.indexOf(entry);
    }
}