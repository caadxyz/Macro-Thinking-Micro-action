package gov.nasa.worldwind.render.city;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.util.ShapeUtils;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.MarkerAttributes;


public class ActivityFactory{
	    String type;
     	public ActivityFactory(String type)
         {this.type=type;}
     	public ActivityMarker createMarker(WorldWindow wwd, boolean fitShapeToViewport)
         {
     		Position position = ShapeUtils.getNewShapePosition(wwd);
            Angle heading = ShapeUtils.getNewShapeHeading(wwd, true); 
            MarkerAttributes attr;
            attr=new BasicMarkerAttributes(Material.LIGHT_GRAY, BasicMarkerShape.CYLINDER, 1d, 10, 5);
         
            ActivityMarker marker = new ActivityMarker(position, attr, heading);
            marker.setValue(AVKey.DISPLAY_NAME, CityBuilder.getNextName(toString(this.type)));
            
            return marker;
         }

         public ActivityMarkerEditor createEditor(ActivityMarker marker)
         {
        	 ActivityMarkerEditor editor = new ActivityMarkerEditor();
             editor.setMarker(marker);
             return editor;
         }


         public String toString(String type)
         {   String streetType=this.type+"Activity";
             return streetType;
         }
     }