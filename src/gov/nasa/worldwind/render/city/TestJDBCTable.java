package gov.nasa.worldwind.render.city;

import javax.swing.*; 
import javax.swing.table.*; 
import java.sql.*; 
public class TestJDBCTable {
	public static void main (String[] args) {
		try {
            Class.forName("org.postgresql.Driver"); 
    	    String url = "jdbc:postgresql://localhost:5432/beijing"; 
    	    String name="postgres";
    	    String password="004017";
    	    Connection conn = DriverManager.getConnection(url,name, password); 
    	    String tableName = createSampleTable(conn);
            TableModel mod = new JDBCTableModel(conn, "degrees_mhd","select gid, the_geom from degrees_mhd");
            JTable jtable = new JTable (mod);
	        JScrollPane scroller =new JScrollPane (jtable, 
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	        JFrame frame = new JFrame ("JDBCTableModel demo");
	        frame.getContentPane().add (scroller);
	        frame.pack();
	        frame.setVisible (true);
            conn.close();
            } catch (Exception e) {
	        e.printStackTrace();
		}
	}

	public static String createSampleTable (Connection conn)
		throws SQLException {
        Statement statement = conn.createStatement();
		// drop table if it exists
		try {

	statement.execute ("DROP TABLE EMPLOYEES");
		} catch (SQLException sqle) {
	sqle.printStackTrace(); // if table !exists
		}
		
		statement.execute ("CREATE TABLE EMPLOYEES " + 
			   "(Name CHAR(20), Title CHAR(30), Salary INT)"); 
		statement.execute ("INSERT INTO EMPLOYEES VALUES " + 
			   "('Jill', 'CEO', 200000 )"); 
		statement.execute ("INSERT INTO EMPLOYEES VALUES " + 
			   "('Bob', 'VP', 195000 )"); 
		statement.execute ("INSERT INTO EMPLOYEES VALUES " + 
			       "('Omar', 'VP', 190000 )"); 
		statement.execute ("INSERT INTO EMPLOYEES VALUES " + 
			   "('Amy', 'Software Engineer', 50000 )"); 
		statement.execute ("INSERT INTO EMPLOYEES VALUES " + 
			   "('Greg', 'Software Engineer', 45000 )");

     statement.close();
	 return "employees"; 
	 }
}
