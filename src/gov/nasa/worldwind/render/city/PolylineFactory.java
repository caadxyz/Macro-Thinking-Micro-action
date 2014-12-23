package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.util.ShapeUtils;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.airspaces.editor.PolylineEditor;

public class PolylineFactory 
{   String type;
    boolean isClose;
	public PolylineFactory(String type, boolean isClose)
    {this.type=type;
     this.isClose=isClose;
    }

    public CityPolyline createPolyline(WorldWindow wwd, boolean fitShapeToViewport)
    {
    	CityPolyline poly = new CityPolyline();
    	poly.setAttributes(CityBuilder.getDefaultPlAttributes());    	
    	poly.setValue(AVKey.DISPLAY_NAME, CityBuilder.getNextName(toString(this.type)));
        poly.setValue("cityPline.type", this.type);
        this.initializePolyline(wwd, poly, fitShapeToViewport);
        return poly;
    }

    public PolylineEditor createEditor(CityPolyline polyline)
    {
        PolylineEditor editor = new PolylineEditor();
        editor.setPolyline((CityPolyline) polyline);
        return editor;
    }

    protected void initializePolyline(WorldWindow wwd, CityPolyline polyline, boolean fitShapeToViewport)
    {
        // Creates a rectangle in the center of the viewport. Attempts to guess at a reasonable size and height.

        Position position = ShapeUtils.getNewShapePosition(wwd);
        Angle heading = ShapeUtils.getNewShapeHeading(wwd, true);
        double sizeInMeters = fitShapeToViewport ?
            ShapeUtils.getViewportScaleFactor(wwd) : CityBuilder.DEFAULT_SHAPE_SIZE_METERS;

        java.util.List<LatLon> locations = ShapeUtils.createSquareInViewport(wwd, position, heading, sizeInMeters);

        double maxElevation = -Double.MAX_VALUE;
        Globe globe = wwd.getModel().getGlobe();

        for (LatLon ll : locations)
        {
            double e = globe.getElevation(ll.getLatitude(), ll.getLongitude());
            if (e > maxElevation)
                maxElevation = e;
        }

       
        polyline.setLocations(locations);
    }

    public String toString(String type)
    {   String streetType=this.type+"_Street";
        return streetType;
    }
}