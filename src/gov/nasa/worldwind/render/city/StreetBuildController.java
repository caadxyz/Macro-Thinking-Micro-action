package gov.nasa.worldwind.render.city;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;

import org.postgis.Geometry;
import org.postgis.MultiLineString;
import org.postgis.PGgeometry;
import org.postgis.Point;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.airspaces.editor.PolylineEditor;

public class StreetBuildController implements ActionListener{
     private CityBuilderController controller;
     private Color red;
     private Color  blue;
     private Color  green;
     private Color yellow;
     
	 public StreetBuildController(CityBuilderController controller) {
	 this.controller=controller;
	 this.red= new Color(255, 0, 0);
	 this.blue=new Color(0, 0, 255);
	 this.green=new Color(0, 255, 0);
	 this.yellow=new Color(255, 255, 0);
    }
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(this.controller.getStreetView().cb2)) {
			if(this.controller.getStreetView().cb2.hasFocus()&& (this.controller.getSelectedPlEntry() != null)){
			String selectColor = (String) ((JComboBox) e.getSource())
					.getSelectedItem();
			Color color = null;
			if (selectColor.equals("Yellow"))
				color = this.yellow;
			else if (selectColor.equals("Red"))
				color = this.red;
			else if (selectColor.equals("Green"))
				color = this.green;
			else if (selectColor.equals("Blue"))
				color = this.blue;

			ShapeAttributes attributes;
			attributes = controller.getSelectedPlEntry().getAttributes();
			attributes.setOutlineMaterial(new Material(color));
			controller.getSelectedPlEntry().getPolyline().setAttributes(
					attributes);
			controller.getSelectedPlEntry().setValue("cityPline.color",
					color.toString());
			controller.getApp().getWwd().redraw();

			String string;
			int R = color.getRed();
			int G = color.getGreen();
			int B = color.getBlue();
			PolylineEntry[] plineEntry;
			plineEntry = this.controller.getPlSelectedEntries();
			int index_geom;
			if (plineEntry != null) {
				index_geom = this.controller.getPlModel().getIndexForEntry(
						plineEntry[0]);
				String gid = this.controller.getPlModel().getValueAt(
						index_geom, 0).toString();
				if (gid != null) {
					string = "UPDATE local_path SET color_r = " + R
							+ ", color_g=" + G + ",  color_b= " + B
							+ "  WHERE gid = '" + gid + "';";
					try {
						/*
						 * Create a statement and execute a select query.
						 */
						Statement s = this.controller.getApp().conn
								.createStatement();
						s.executeUpdate(string);
						s.close();
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}
			}

		}
		}
		
		
		
		if (e.getSource().equals(this.controller.getStreetView().factoryComboBox)) {
			if(this.controller.getStreetView().factoryComboBox.hasFocus()&& (controller.getSelectedPlEntry() != null)){
			String selectType = (String) ((JComboBox) e.getSource())
					.getSelectedItem();


			PolylineEntry[] plineEntry;
			plineEntry = this.controller.getPlSelectedEntries();
			plineEntry[0].setValue("cityPline.type", selectType.trim());
			int index_geom;
			if (plineEntry != null) {
				index_geom = this.controller.getPlModel().getIndexForEntry(
						plineEntry[0]);
				String gid = this.controller.getPlModel().getValueAt(
						index_geom, 0).toString();
				String string;
				if (gid != null) {
					string = "UPDATE local_path SET type = '" + selectType.trim()
							+ "'  WHERE gid = '" + gid + "';";
					try {
						/*
						 * Create a statement and execute a select query.
						 */
						Statement s = this.controller.getApp().conn
								.createStatement();
						s.executeUpdate(string);
						s.close();
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}
			}

		}
		}
		

	}
     
     
     public void newStreetDate(){
    	 
    		if (this.controller.getStreetView().getSelectedFactory().equals("L")) this.createNewPlEntry(CityBuilder.defaultStreetFactories[0]);
        	if (this.controller.getStreetView().getSelectedFactory().equals("M")) this.createNewPlEntry(CityBuilder.defaultStreetFactories[1]);
        	if (this.controller.getStreetView().getSelectedFactory().equals("S")) this.createNewPlEntry(CityBuilder.defaultStreetFactories[2]);
         
    	 
     }
     public void createNewPlEntry(PolylineFactory factory)
     {
     	CityPolyline cityPolyline = factory.createPolyline(this.controller.getApp().getWwd(), this.controller.isResizeNewShapesToViewport());
        cityPolyline.setClosed(false);
     	cityPolyline.setDrawInterior=false;
     	
     	ShapeAttributes ini_attribute =cityPolyline.getAttributes();
     	
     	String selectColor=(String) this.controller.getStreetView().cb2.getSelectedItem();
     	Color color = null;
         if (selectColor.equals("Yellow"))
             {color =this.yellow;
              cityPolyline.setValue("cityPline.color", "Yellow");
             }
         else if (selectColor.equals("Red"))
         { color = this.red;
         cityPolyline.setValue("cityPline.color", "Red");
        }
         else if (selectColor.equals("Green"))
             {color = this.green;
           cityPolyline.setValue("cityPline.color", "Green");
              }
         else if (selectColor.equals("Blue"))
         {    color = this.blue;
             cityPolyline.setValue("cityPline.color", "Blue");
             }
         else {color= this.red;
               cityPolyline.setValue("cityPline.color", "Red");}
         
         ini_attribute.setOutlineMaterial(new Material(color));
         
         
         float width=0;
         String widthString="";
         if(this.controller.getStreetView().sp.getValue() != null){ 
         width=Float.parseFloat((String)this.controller.getStreetView().sp.getValue()); 
         widthString=(String)this.controller.getStreetView().sp.getValue();
         cityPolyline.setValue("cityPline.width", widthString);
         } else { width=5; cityPolyline.setValue("cityPline.width", "5.0");} 
         ini_attribute.setOutlineWidth(width);        
        
        cityPolyline.setAttributes(ini_attribute);
        PolylineEditor editor = factory.createEditor(cityPolyline);
        PolylineEntry entry = new PolylineEntry(cityPolyline, editor);
         
		
        String string="";
        for( int i = 0; i < entry.getPolyline().getLocations().size(); i++) { 
			   string=string+entry.getPolyline().getLocations().get(i).longitude.degrees+ " ";
			   string=string+entry.getPolyline().getLocations().get(i).latitude.degrees + ",";
	        } 
		   
		   string=string.substring(0,string.length()-1);
		   String gid_number;
		   
		   string="INSERT INTO local_path( name, the_geom , color_r, color_g, color_b ,width,type) VALUES ( '"+cityPolyline.getValue(AVKey.DISPLAY_NAME)+
		        "',GeomFromText('MULTILINESTRING((" + string +"))', -1),'"+color.getRed()+
		        "', '"+color.getGreen()+
		        "', '"+color.getBlue()+
		        "', '"+widthString+
		        "', '"+cityPolyline.getValue("cityPline.type")+
		        "');";
           
		   try {
			   /* 
	    	    * Create a statement and execute a select query. 
	    	    */ 
	    	    Statement s = this.controller.getApp().conn.createStatement(); 
	    	    s.executeUpdate(string); 
	    	    ResultSet r = s.executeQuery("select gid from local_path where name='"+cityPolyline.getValue(AVKey.DISPLAY_NAME)+"'"); 	    	    
	    	    r.next();
	    	    gid_number= r.getObject(1).toString();
	    	    entry.setValue("cityPline.gid",gid_number);	
	    	    s.close(); 
           }
		   catch( Exception e ) { 
		    	  e.printStackTrace(); 
		    	  }  
         this.controller.addPlEntry(entry);
         this.controller.selectPlEntry(entry, true);
     }
     
       public void deleteStreetDate(){
    	   
		   String string="";
		   PolylineEntry[] plineEntry;
		   plineEntry=this.controller.getPlSelectedEntries();		  
		   int index_geom;
		   index_geom=this.controller.getPlModel().getIndexForEntry(plineEntry[0]);
		   String name= this.controller.getPlModel().getValueAt(index_geom, 0).toString();

		   
           string="DELETE FROM local_path WHERE gid = '"+ name+"';";
           try {
			   /* 
	    	    * Create a statement and execute a select query. 
	    	    */ 
	    	    Statement s = this.controller.getApp().conn.createStatement(); 
	    	    s.executeQuery(string); 
	    	    s.close(); 
           }
		   catch( Exception e ) { 
		    	  //e.printStackTrace(); 
		    	  }
		   this.controller.removePlEntries(Arrays.asList(this.controller.getPlSelectedEntries()));
    	   
       }
	   public void openStreetData(){
		
	    	 final ArrayList<CityPolyline> polylines = new ArrayList<CityPolyline>();
	         try { 
	    	    	 
	    	    /* 
	    	    * Create a statement and execute a select query. 
	    	    */ 
	    	    Statement s = this.controller.getApp().conn.createStatement(); 
	    	    ResultSet r = s.executeQuery("select gid, the_geom, color_r, color_g,  color_b, name, width, type  from local_path"); 
	    	    
	    	    //ResultSet r = s.executeQuery("select ST_AsText(geom) as geom,id from gtest"); 
	    	    int ii=0;
	    	    while( r.next() ) { 
	    	      /* 
	    	      * Retrieve the geometry as an object then cast it to the geometry type. 
	    	      * Print things out. 
	    	      */ 
	    	        ii++;
	    	        ArrayList<LatLon> positions= new ArrayList<LatLon>();
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
	    	    	    positions.add(latlon);
	    	    	    
	    	    	    
	    	            } 
	    	    	}
	                       	        
	    	        CityPolyline polyline =new CityPolyline(positions);
	                polylines.add(polyline);	                
	                
	                //polyline.setValue(AVKey.DISPLAY_NAME, name);
	                polyline.setValue("cityPline.gid", gid);
	                if (name !=null){
	                polyline.setValue(AVKey.DISPLAY_NAME, name);
	                }
	                if (type !=null){
		                polyline.setValue("cityPline.type", type);
		                }
	                ShapeAttributes attributes = new BasicShapeAttributes();
				    if (color != null) {

					if (color.equals(this.yellow))
						polyline.setValue("cityPline.color", "Yellow");

					else if (color.equals(this.red)){
						polyline.setValue("cityPline.color", "Red");
					}
					else if (color.equals(this.green))
						polyline.setValue("cityPline.color", "Green");
					else if (color.equals(this.blue))
						polyline.setValue("cityPline.color", "Blue");
					attributes.setOutlineMaterial(new Material(color));
				   } else
					attributes.setOutlineMaterial(Material.RED);
				    
				    

				attributes.setDrawOutline(true);
				attributes.setOutlineOpacity(0.8);
				if (width !=0){
					attributes.setOutlineWidth(width);
					polyline.setValue("cityPline.width", ((String)r.getObject(7)).trim());	
				}
				else {attributes.setOutlineWidth(2);}
				polyline.setAttributes(attributes);

			}
	    	     s.close(); 
	    	  } 
	    	  catch( Exception e ) { 
	    	  e.printStackTrace(); 
	    	  } 
	          this.controller.setPolylines(polylines);
	          this.controller.setEnabled(true);
	          this.controller.getApp().setCursor(null);
	          this.controller.getApp().getWwd().redraw();
	   
	   
	   
	   }
	   
	   public void saveStreetDate(){
		   
		   
		   String string="";
		   PolylineEntry[] plineEntry;
		   plineEntry=this.controller.getPlSelectedEntries();		  
		   int index_geom;
		   index_geom=this.controller.getPlModel().getIndexForEntry(plineEntry[0]);
		   String gid= this.controller.getPlModel().getValueAt(index_geom, 0).toString();
		   
		   String name=this.controller.getPlModel().getValueAt(index_geom, 1).toString();
		   
		   System.out.println(gid);
		   System.out.println(name);
          for( int i = 0; i < plineEntry[0].getPolyline().getLocations().size(); i++) { 
			   string=string+plineEntry[0].getPolyline().getLocations().get(i).longitude.degrees+ " ";
			   string=string+plineEntry[0].getPolyline().getLocations().get(i).latitude.degrees + ",";
	        } 
		   
		   string=string.substring(0,string.length()-1);
		   //UPDATE local_path SET the_geom = GeometryFromText('MULTILINESTRING((1 1,2 2,3 3))',-1) WHERE gid = '92';
		   
           string="UPDATE local_path SET the_geom = GeometryFromText('MULTILINESTRING((" + string +"))',-1), name='"+name+"' WHERE gid = '"+gid +"';";
           try {
			   /* 
	    	    * Create a statement and execute a select query. 
	    	    */ 
	    	    Statement s = this.controller.getApp().conn.createStatement(); 
	    	    s.executeUpdate(string);
	    	    s.close(); 
           }
		   catch( Exception e ) { 
		    	  e.printStackTrace(); 
		    	  } 
           
		   
		   
	   }
	   

	   public void deSelectStreetDate(){
		   this.controller.selectPlEntry(null, true);
	   }
	   public void resizeStreetDate(){
		  if(this.controller.getStreetView().resizeAction.getValue("SmallIcon")==this.controller.getStreetView().iconResize )
		  {this.controller.getStreetView().resizeAction.putValue("SmallIcon", this.controller.getStreetView().iconResize_g);
		  this.controller.setResizeNewShapesToViewport(false);
		  }
		  else if (this.controller.getStreetView().resizeAction.getValue("SmallIcon")==this.controller.getStreetView().iconResize_g )
		  {this.controller.getStreetView().resizeAction.putValue("SmallIcon", this.controller.getStreetView().iconResize);
		  this.controller.setResizeNewShapesToViewport(true);
		  }
       }
	   public void modifyStreetDate(){
		   if(this.controller.getStreetView().modifyAction.getValue("SmallIcon")==this.controller.getStreetView().iconModify )
			  {this.controller.getStreetView().modifyAction.putValue("SmallIcon", this.controller.getStreetView().iconModify_g);
			   this.controller.setPlEnableEdit(false);
			   this.controller.getStreetView().entryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			  }
			  else if (this.controller.getStreetView().modifyAction.getValue("SmallIcon")==this.controller.getStreetView().iconModify_g )
			  {this.controller.getStreetView().modifyAction.putValue("SmallIcon", this.controller.getStreetView().iconModify );
			   this.controller.setPlEnableEdit(true);
			   this.controller.getStreetView().entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			  }  
      }
	  public void sqlStreetDate(){
		  
		  try {
			   
	    	    Statement s = this.controller.getApp().conn.createStatement(); 
	    	    s.executeUpdate(this.controller.getStreetView().textArea_sql.getText());
	    	    s.close(); 
          }
		   catch( Exception e ) { 
		    	  e.printStackTrace(); 
		    	  }
		   this.openStreetData();
		  
	  }
	   
	   



}
