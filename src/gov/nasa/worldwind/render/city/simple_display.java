package gov.nasa.worldwind.render.city;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.ActivityMarkerLayer;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SurfaceShapeLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.city.CityBuilder.AppFrame;
import gov.nasa.worldwind.render.city.CityBuilder.AppFrame.ControlerAction;
import gov.nasa.worldwind.view.firstperson.FlyToFlyViewAnimator;
import gov.nasa.worldwind.examples.SimplestPossibleExample;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.postgis.Geometry;
import org.postgis.MultiLineString;
import org.postgis.PGbox3d;
import org.postgis.PGgeometry;
import org.postgis.Point;

public class simple_display extends CityBuilder.AppFrame
	{
	    
	
       public simple_display()
	    {    
    	    
              //Layerlist layerlist;
	         LayerList layerList;
	         layerList=this.getWwd().getModel().getLayers();
	         for (Layer layer : layerList){
	         if(layer.getName() != "Street" && layer.getName() != "Architecture")	 
	         layer.setEnabled(false);
             }
	         
	         this.connectToDatabase();	         
	         final ArrayList<Polyline> polylines = new ArrayList<Polyline>();
	         try { 
	    	    	 
	    	    /* 
	    	    * Create a statement and execute a select query. 
	    	    */ 
	    	    Statement s = this.conn.createStatement(); 
	    	    ResultSet r = s.executeQuery("select gid, the_geom, color_r, color_g,  color_b, name, width, type  from local_path"); 
	            int ii=0;
	    	    while( r.next() ) { 
	    	      /* 
	    	      * Retrieve the geometry as an object then cast it to the geometry type. 
	    	      * Print things out. 
	    	      */ 
	    	        ii++;
	    	        ArrayList<Position> positions= new ArrayList<Position>();
	    	    	PGgeometry geom = (PGgeometry)r.getObject(2); 
	    	    	String gid;
	    	    	gid= r.getObject(1).toString();
	    	    	String name;
	    	    	name=r.getObject(6).toString();
	    	    	double width=0;
	    	    	if(r.getObject(7)!= null ){
	    	    		width=Float.parseFloat((String)r.getObject(7));;
	    	    	}
	    	    	String type=null;
	    	    	if(r.getObject(8)!= null){type=r.getObject(8).toString();}
	    	    	
	    	    	Color color=null;
	    	    	if(r.getObject(3)!= null ){	    	    	
	    	    	color = new Color(((Integer)r.getObject(3)).intValue(), ((Integer)r.getObject(4)).intValue(), ((Integer)r.getObject(5)).intValue());
	    	    	}
	    	    	if(geom.getGeometry().getType()== Geometry.MULTILINESTRING) { 
	    	    	MultiLineString pl = (MultiLineString)geom.getGeometry(); 
	    	    	
	    	    	LatLon latlon;
	    	    	
	    	    	for( int i = 0; i < pl.numPoints(); i++) { 
	    	    	    Point point = pl.getPoint(i); 
	    	    	    latlon= LatLon.fromDegrees(point.y, point.x);
	    	    	    positions.add(new Position(latlon,0));
	    	    	    
	    	    	    
	    	            } 
	    	    	}
	                       	        
	    	        
	    	    	
	    	    	
	    	    	Polyline polyline =new Polyline(positions);
	                polylines.add(polyline);                
                    polyline.setColor(Color.GREEN);
                    polyline.setLineWidth(5);
				    this.surfaceShapeLayer.addRenderable(polyline);
				

			}
	    	     s.close(); 
	    	  } 
	    	  catch( Exception e ) { 
	    	  e.printStackTrace(); 
	    	  } 
	    	  
	    	  View view = this.getWwd().getView();
	    	  LatLon latlon=LatLon.fromDegrees(39.85, 116.4);
	    	  Position position=new Position(latlon,10000);	  
	    	  view.setEyePosition(position);
	    	 // view.setEyePosition(eyePosition);
	    	  
	    	  makeToolBar(this, this.builderController);
              

	         
	         
	         
	         
	         
	        
	    }         
	 		private void connectToDatabase() {
	              try { 
		    	    /* 
		    	    * Load the JDBC driver and establish a connection. 
		    	    */
		    	    Class.forName("org.postgresql.Driver"); 
		    	    String url = "jdbc:postgresql://localhost:5432/beijing"; 
		    	    String name="postgres";
		    	    String password="004017";
		    	    
		    	    
		    	    this.conn= DriverManager.getConnection(url,name, password);
		            ((org.postgresql.PGConnection)conn).addDataType("geometry",PGgeometry.class);
		    	    ((org.postgresql.PGConnection)conn).addDataType("box3d",PGbox3d.class);
                    System.out.println("connect succeed");
		            } 
		    	  catch( Exception e ) { 
		    	  e.printStackTrace(); 
		    	  } 
	         } 
	 		
	 		


	        public static void makeToolBar(simple_display frame, final CityBuilderController controller)
	        { JToolBar toolBar = new JToolBar("controler");
	        
	        ControlerAction D_LayerAction=new ControlerAction( 
	        		"D_Layer", 
	        		new ImageIcon("./resources/layer.gif "), 
	        		"display layer", 
	        		'L',
	        		frame);
	        
	        ControlerAction H_LayerAction=new ControlerAction( 
	        		"H_Layer", 
	        		new ImageIcon("./resources/layer_h.gif "), 
	        		"hide layer",
	        		'H', 
	        		frame);
	       
	        ControlerAction fullJustifyAction   =   new   ControlerAction( 
	        		"Contraler",
	        		new   ImageIcon("./resources/builder_pln.gif "),   
	        		"Full   justify   text ",   
	        		'F', 
	        		frame);
	        ControlerAction   datebaseAction   =   new   ControlerAction( 
	        		"Datebase",   
	        		new   ImageIcon("./resources/conn.gif "),   
	        		"datebase action",   
	        		'D', 
	        		frame);
	        
	        
	        toolBar.add(D_LayerAction);
	        toolBar.add(H_LayerAction);
	        toolBar.add(fullJustifyAction);
	        toolBar.add(datebaseAction);
	        
	        
	        frame.getContentPane().add(toolBar,   BorderLayout.NORTH);
	        }
	        
	        
	        
	        public static class   ControlerAction   extends   AbstractAction   {
	            public AppFrame appFrame;
	            public   ControlerAction(String text,Icon icon, String description,char accelerator,AppFrame appFrame )   {
	                super(text,   icon);
	                this.appFrame=appFrame;
	                putValue(ACCELERATOR_KEY,   KeyStroke.getKeyStroke(accelerator,
	                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	                putValue(SHORT_DESCRIPTION,   description);
	            }

	            public   void   actionPerformed(ActionEvent   e)   {
	            	if(getValue(NAME)=="D_Layer"){ 
	            		appFrame.layerPanel.setVisible(true);} 
	            	if(getValue(NAME)=="H_Layer"){
	            		appFrame.layerPanel.setVisible(false);}
	                if(getValue(NAME)=="Contraler"){
	                	 JFrame controlFrame = new JFrame();
	                     controlFrame.getContentPane().add(new StreetBuilderPanel(appFrame.plBuilderModel,  appFrame.builderController, appFrame.streetController));
	                     controlFrame.pack();
	                     controlFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	                     controlFrame.setVisible(true);
	                     controlFrame.setTitle("controler");
	                     controlFrame.setAlwaysOnTop(true);
	                     };
	                 if(getValue(NAME)=="Datebase"){
	                    	appFrame.cityInterface.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	                    	appFrame.cityInterface.setVisible(true);
	                    	appFrame.cityInterface.setAlwaysOnTop(true);
	                         };
	            	
	            }
	        }


	    public static void main(String[] args)
	    {
	    	ApplicationTemplate.start("Micro Thinking & Micro Action", simple_display.class);
	    }
	}


