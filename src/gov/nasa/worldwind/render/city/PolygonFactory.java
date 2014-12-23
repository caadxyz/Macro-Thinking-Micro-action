package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.examples.util.ShapeUtils;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;

import gov.nasa.worldwind.render.airspaces.editor.CityPolygonEditor;

public  class PolygonFactory
{   String type;
    public PolygonFactory(String type)
    {this.type=type;
    }
    

    public CityPolygon createPolygon(WorldWindow wwd, boolean fitShapeToViewport)
    {
        CityPolygon poly = new CityPolygon();
        poly.setAttributes(CityBuilder.getDefaultPgAttributes());
        poly.setValue(AVKey.DISPLAY_NAME, CityBuilder.getNextName(toString(this.type)));
        poly.setAltitudes(0.0, 0.0);
        poly.setTerrainConforming(true, false);
        this.initializePolygon(wwd, poly, fitShapeToViewport);

        return poly;
    }

    public CityPolygonEditor createEditor(CityPolygon poly)
    {
        CityPolygonEditor editor = new CityPolygonEditor();
        editor.setPolygon(poly);
        CityBuilder.setPgEditorAttributes(editor);
        return editor;
    }

    protected void initializePolygon(WorldWindow wwd, CityPolygon polygon, boolean fitShapeToViewport)
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

        polygon.setAltitudes(0.0, maxElevation + sizeInMeters);
        polygon.setTerrainConforming(true, false);
        polygon.setLocations(locations);
    }

    public String toString(String type)
    {   String architectureType=this.type+"_Architecture";
        return architectureType;
    }
}