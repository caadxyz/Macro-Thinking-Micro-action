package gov.nasa.worldwind.render.city;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.postgis.PGbox3d;
import org.postgis.PGgeometry;

import processing.core.PApplet;

public class DatabaseConn extends JFrame{
	
	protected static final String CONNECTION = "panel1.connectToDatabase";
	protected static final String TABLE = "panel1.table";
	protected static final String SQL = "panel1.SQL";
    private		JTabbedPane tabbedPane;
	
	private		JPanel		       panel1;
	private		JTextField	       panel1_field;
	private     JPasswordField     panel1_fieldPass; 
	private     JButton            panel1_JButton1;
	private     JButton            panel1_JButton2;
	private     List               panel1_List1;
	private     TextArea           panel1_textArea1;
	
	private		JPanel		       panel4;
	private     JTable             panel4_jtable;
	private     TableModel         mod;
	
	public      StreetBuilderPanel        panel5;
	public      ArchitectureBuilderPanel    panel6;
	public      EnvironmentBuilderPanel        panel7;
	public      ActivityBuilderPanel    panel8;
	public      JPanel panel9;
	public      JPanel panel10;
	
	
	private     PageController controller;
	private     CityBuilder.AppFrame appFrame;
	
	
	// NOTE: to reduce the amount of code in this example, it uses
	// panels with a NULL layout.  This is NOT suitable for
	// production code since it may not display correctly for
	// a look-and-feel.
	DatabaseConn(CityBuilder.AppFrame appFrame){
		
	setTitle( "Urban Parameter " );
	setSize( 200, 870 );
	setBackground( Color.gray );

	JPanel topPanel = new JPanel();
	topPanel.setLayout( new BorderLayout() );
	getContentPane().add( topPanel );
    
	this.appFrame=appFrame;
	this.controller=new 	PageController(this,appFrame);
	// Create the tab pages
	createPage1();
	createPage4();
	createPage5();
	createPage6();
	createPage7();
	createPage8();
	createPage9();
	createPage10();
		
	

	// Create a tabbed pane
	tabbedPane = new JTabbedPane();
	tabbedPane.addTab( "DateBase", panel1 );
	tabbedPane.addTab( "table",    panel4 );
	tabbedPane.addTab("Street Element", panel5);
	tabbedPane.addTab("Architecture Element", panel6);
	tabbedPane.addTab("Environment Element", panel7);
	tabbedPane.addTab("Activity Element", panel8);
	tabbedPane.addTab("Analyse", panel9);
	//tabbedPane.addTab("initialization", panel10);
    
	topPanel.add( tabbedPane, BorderLayout.CENTER );	
	}
	
	public void createPage1()
	{
		panel1 = new JPanel();
		panel1.setLayout( null );		
		JLabel label1 = new JLabel( "Username:" );
		label1.setBounds( 10, 15, 150, 20 );
		panel1.add( label1 );
		panel1_field = new JTextField();
		panel1_field.setBounds( 10, 35, 150, 20 );	
		
		panel1.add(panel1_field);
		JLabel label2 = new JLabel( "Password:" );
		label2.setBounds( 10, 60, 150, 20 );
		panel1.add( label2 );
		panel1_fieldPass = new JPasswordField();
		panel1_fieldPass.setBounds( 10, 80, 150, 20 );
		panel1.add(panel1_fieldPass);	
		
		panel1_JButton1= new JButton();	
		panel1_JButton1.setName("JButton1");
		panel1_JButton1.setText("connect to database");
		panel1_JButton1.setBounds( 10, 120, 150, 20);
		panel1_JButton1.setActionCommand(CONNECTION);
		panel1_JButton1.addActionListener(controller);
		panel1_JButton1.setToolTipText("connect to postgis database");
		panel1.add( panel1_JButton1 );
		
		panel1_List1=new List();
		panel1_List1.setBounds( 10, 160, 150, 200);
		//panel1_List1.setActionCommand(TABLE);
		panel1_List1.addActionListener(controller);
	    panel1.add(panel1_List1);
	    
	    panel1_JButton2= new JButton();	
	    panel1_JButton2.setName("JButton2" );
	    panel1_JButton2.setText("SQL Command");
	    panel1_JButton2.setBounds( 10, 400, 150, 20 );
	    panel1_JButton2.setActionCommand(SQL);
		panel1_JButton2.addActionListener(controller);
		panel1.add( panel1_JButton2 );
		
		panel1_textArea1= new TextArea();
		panel1_textArea1.setBounds( 10, 440, 150, 200 );
		panel1.add(panel1_textArea1);

		
    }
	
	public static class PageController implements ActionListener, ListSelectionListener{
		public  CityBuilder.AppFrame appFrame;
		public DatabaseConn databaseConn;
		public PageController (DatabaseConn databaseConn,CityBuilder.AppFrame appFrame){
			this.appFrame=appFrame;
			this.databaseConn=databaseConn;
		}
		
	
		@Override
		public void actionPerformed(ActionEvent e) {
		      //noinspection StringEquality
	        if (e.getActionCommand() == CONNECTION)
	        {   if (databaseConn.panel1_JButton1.getText()=="connect to database"){
	        	databaseConn.panel1_JButton1.setText("disconnect to database");
	            this.connectToDatabase();
	        	}
	        
	            else{databaseConn.panel1_JButton1.setText("connect to database");
	        	this.disConnectToDatabase();
	            }
	        
	        } 
	        if (e.getSource().equals(databaseConn.panel1_List1)){
	        List list=(List) e.getSource();
	        this.showTable(list);
	       }
	        
	        if (e.getActionCommand() == SQL){
	        	 this.SQLTable();
		       }
	    
		}
		
		
		
		private void SQLTable( ){
			 System.out.println(databaseConn.panel1_textArea1.getText());
			 String table=databaseConn.panel1_List1.getSelectedItem();
			 String SQL=databaseConn.panel1_textArea1.getText();
			 JTable jtable;
			 
			 databaseConn.mod = new JDBCTableModel(appFrame.conn,table,SQL);
				    //mod.addTableModelListener(databaseConn.controller);
			        jtable = new JTable (databaseConn.mod);
			        jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			        jtable.getSelectionModel().addListSelectionListener(this);
			        
			        JScrollPane scroller =new JScrollPane (jtable,
			        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			       // databaseConn.panel4.removeAll();
			        databaseConn.panel4.setLayout(new BorderLayout());
			        databaseConn.panel4.add(scroller,BorderLayout.CENTER);
			        System.out.println(jtable.getModel()); 
			        System.out.println(((AbstractTableModel)jtable.getModel()).getTableModelListeners()); 
		} 
		private void showTable( List list){
	    System.out.println(list.getSelectedItem());
	    }
		
		
		private void disConnectToDatabase() {
		    if (appFrame.conn !=null){
			try { appFrame.conn.close();
		          System.out.println("disconnect succeed");}
		    catch( Exception e ) { 
	    	  e.printStackTrace(); 
	    	  }
		    }
		}
		
		private void connectToDatabase() {
              try { 
	    	    /* 
	    	    * Load the JDBC driver and establish a connection. 
	    	    */
	    	    Class.forName("org.postgresql.Driver"); 
	    	    String url = "jdbc:postgresql://localhost:5432/beijing"; 
	    	    char[] pw = this.databaseConn.panel1_fieldPass.getPassword(); 
	    	    String name="postgres";
	    	    String password="004017";
	    	    
	    	    //if (name.equals(this.databaseConn.panel1_field.getText()) && password.equals(new String(pw))){
	    	    appFrame.conn= DriverManager.getConnection(url,name, password); 
	    	    
	    	    
	    	       /* 
		    	    * Add the geometry types to the connection. Note that you 
		    	    * must cast the connection to the pgsql-specific connection 
		    	    * implementation before calling the addDataType() method. 
		    	    */
	    	    ((org.postgresql.PGConnection)appFrame.conn).addDataType("geometry",PGgeometry.class);
	    	    ((org.postgresql.PGConnection)appFrame.conn).addDataType("box3d",PGbox3d.class);
	    	    
	    	    Statement s = appFrame.conn.createStatement();
	    	    String query="SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
                ResultSet rs = s.executeQuery(query); 
                String tableName = null;
                while(rs.next()) {
                tableName =(String) rs.getObject(1);
                databaseConn.panel1_List1.add(tableName );
                 }
                rs.close();
                System.out.println("connect succeed");
	            } 
	    	  catch( Exception e ) { 
	    	  e.printStackTrace(); 
	    	  } 
         }




		@Override
		public void valueChanged(ListSelectionEvent e) {
		ListSelectionModel 	sModel=(ListSelectionModel) e.getSource();
	    String mstring;
	    mstring=databaseConn.mod.getValueAt(sModel.getAnchorSelectionIndex(), 0).toString();
		System.out.println(mstring);
		}

	
	

		
	}
	


	
	public void createPage4()
	{   panel4 = new JPanel();
	}
	public void createPage5()
	
	{   panel5 = new StreetBuilderPanel(appFrame.plBuilderModel,appFrame.builderController, appFrame.streetController);
		appFrame.builderController.setStreetView(this.panel5);
		appFrame.builderController.setResizeNewShapesToViewport(true);
	}
	public void createPage6()
	
	{   panel6 = new ArchitectureBuilderPanel(appFrame.pgBuilderModel,appFrame.builderController, appFrame.architectureController);
		appFrame.builderController.setArchitectureView(this.panel6);
		appFrame.builderController.setResizeNewShapesToViewport(true);
	}
	public void createPage7()
	
	{   panel7 = new EnvironmentBuilderPanel(appFrame.peBuilderModel,appFrame.builderController);
		appFrame.builderController.setEnvironmentView(this.panel7);
		appFrame.builderController.setResizeNewShapesToViewport(true);
	}
	public void createPage8()
	
	{   panel8 = new ActivityBuilderPanel(appFrame.ppBuilderModel,appFrame.builderController);
		appFrame.builderController.setActivityView(this.panel8);
		appFrame.builderController.setResizeNewShapesToViewport(true);
	}
	public void createPage9()
	
	{   panel9 = new JPanel();
	    panel9.setLayout( new BorderLayout(30, 0));	
	    Graphy papplet =new Graphy();
	    papplet.init();
	    panel9.add(papplet,BorderLayout.CENTER);
	    
	}
    public void createPage10()
	
	{   
    	//panel10 = new InitializationPanel(this.appFrame);
    }

}//end of the class databaseConn
