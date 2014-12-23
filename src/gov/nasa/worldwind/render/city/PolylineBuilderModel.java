package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.city.PolylineEntry;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.table.AbstractTableModel;


public class PolylineBuilderModel extends AbstractTableModel
{
    private static String[] columnName = {"gid","name"};
    private static Class[]  columnClass = {String.class,String.class};
    private static String[] columnAttribute = 
       {"cityPline.gid",AVKey.DISPLAY_NAME};
    private ArrayList<PolylineEntry> entryList = new ArrayList<PolylineEntry>();
    
    public PolylineBuilderModel()
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
        PolylineEntry entry = this.entryList.get(rowIndex);
        return entry.getValue(columnAttribute[columnIndex]);
    }

    public void setValueAt(Object aObject, int rowIndex, int columnIndex)
    {
        PolylineEntry entry = this.entryList.get(rowIndex);
        String key = columnAttribute[columnIndex];
        entry.setValue(key, aObject);
    }

    public java.util.List<PolylineEntry> getEntries()
    {
        return Collections.unmodifiableList(this.entryList);
    }

    public void setEntries(Iterable<? extends PolylineEntry> entries)
    {
        this.entryList.clear();
        if (entries != null)
        {
            for (PolylineEntry entry : entries)
            {
                this.entryList.add(entry);
            }
        }

        this.fireTableDataChanged();
    }

    public void addEntry(PolylineEntry entry)
    {
        this.entryList.add(entry);
        int index = this.entryList.size() - 1;
        this.fireTableRowsInserted(index, index);
    }

    public void removeEntry(PolylineEntry entry)
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

    public PolylineEntry getEntry(int index)
    {
        return this.entryList.get(index);
    }

    public PolylineEntry setEntry(int index, PolylineEntry entry)
    {
        return this.entryList.set(index, entry);
    }

    public int getIndexForEntry(PolylineEntry entry)
    {
        return this.entryList.indexOf(entry);
    }
}