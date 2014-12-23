package gov.nasa.worldwind.render.city;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import org.postgis.PGgeometry;
import java.sql.*;
import java.util.*;
/**
 * an immutable table model built from getting metadata about a table in a jdbc
 * database
 */
public class JDBCTableModel extends AbstractTableModel{
	Object[][] contents;
	String[] columnNames;
	Class<?>[] columnClasses;
	

	public JDBCTableModel(Connection conn, String tableName)
		{
		super();
		getTableContents(conn, tableName,"SELECT * FROM " + tableName);
	}
	
	public JDBCTableModel(Connection conn, String tableName, String SQL)
	{
    super();
    getTableContents(conn, tableName,SQL);
    }

	protected void getTableContents(Connection conn, String tableName,String SQL )
			{
        
		// get metadata: what columns exist and what
		// types (classes) are they?
		/*
		DatabaseMetaData meta = conn.getMetaData();
		System.out.println("got meta = " + meta);
		ResultSet results = meta.getColumns(null, null, tableName, null);
		System.out.println("got column results");
		
        */
		ArrayList<String> colNamesList = new ArrayList<String>();
		ArrayList<Class<?>> colClassesList = new ArrayList<Class<?>>();
		try{
		Statement statement = conn.createStatement();
		ResultSet results = statement.executeQuery(SQL);
	    ResultSetMetaData metaResults=results.getMetaData();
	    for ( int i=1; i<metaResults.getColumnCount()+1;i++) {
			colNamesList.add(metaResults.getColumnName(i));
			System.out.println("name: " +metaResults.getColumnName(i));
			
			int dbType = metaResults.getColumnType(i);
			switch (dbType) {
			case Types.INTEGER:
				colClassesList.add(Integer.class);
				break;
			case Types.FLOAT:
				colClassesList.add(Float.class);
				break;
			case Types.DOUBLE:
			case Types.REAL:
				colClassesList.add(Double.class);
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				colClassesList.add(java.sql.Date.class);
				break;
			case 1111:
				colClassesList.add(PGgeometry.class);
				break;
			default:
				colClassesList.add(String.class);
				break;
			};
			System.out.println("type: " + metaResults.getColumnTypeName(i));
		}
		
		
		columnNames = new String[colNamesList.size()];
		colNamesList.toArray(columnNames);
		columnClasses = new Class[colClassesList.size()];
		colClassesList.toArray(columnClasses);

		// get data from table and put into contents array

		ArrayList<Object[]> rowList = new ArrayList<Object[]>();
		while (results.next()) {
			ArrayList<Object> cellList = new ArrayList<Object>();
			for (int i = 0; i < columnClasses.length; i++) {
				Object cellValue = null;
				if (columnClasses[i] == String.class)
					cellValue = results.getString(columnNames[i]);
				else if (columnClasses[i] == Integer.class)
					cellValue = new Integer(results.getInt(columnNames[i]));
				else if (columnClasses[i] == Float.class)
					cellValue = new Float(results.getInt(columnNames[i]));
				else if (columnClasses[i] == Double.class)
					cellValue = new Double(results.getDouble(columnNames[i]));
				else if (columnClasses[i] == java.sql.Date.class)
					cellValue = results.getDate(columnNames[i]);
				else if (columnClasses[i] == PGgeometry.class) {
					cellValue =results.getObject(columnNames[i]);
				} else	System.out.println("Can't assign " + columnNames[i]);
				cellList.add(cellValue);
			}// for
			Object[] cells = cellList.toArray();
			rowList.add(cells);

		} // while

		// finally create contents two-dim array
		contents = new Object[rowList.size()][];
		for (int i = 0; i < contents.length; i++)
			contents[i] = (Object[]) rowList.get(i);
		System.out.println("Created model with " + contents.length + " rows");

		// close stuff
		results.close();
		statement.close();
		}
		catch (SQLException e) {e.printStackTrace();}

	}

	// AbstractTableModel methods
	public int getRowCount(){return contents.length;}
    public int getColumnCount() {
		if (contents.length == 0)
			return 0;
		else
			return contents[0].length;
	}

	public Object getValueAt(int row, int column) {
		if(getColumnClass(column)==PGgeometry.class){
		PGgeometry geom = (PGgeometry) contents[row][column];
	    return	geom.toString();
	    }
		else return contents[row][column];
	}
	
	public PGgeometry getGeometry(int row, int column){
		if(getColumnClass(column)==PGgeometry.class){
			PGgeometry geom = (PGgeometry) contents[row][column];
		    return	geom;
		    }
		return null;
	}

	// overrides methods for which AbstractTableModel
	// has trivial implementations

	public Class<?> getColumnClass(int col) {
		return columnClasses[col];
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	 public boolean isCellEditable(int rowIndex, int columnIndex)
	    {
	        return false;
	    }
	
	
	
}

