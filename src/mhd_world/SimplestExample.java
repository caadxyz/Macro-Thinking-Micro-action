package mhd_world;

import java.awt.Color;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.examples.util.LayerManagerLayer;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.airspaces.CappedCylinder;
import gov.nasa.worldwind.util.BasicDragger;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.Sector;

import javax.swing.*;

import mhd_world.analytics.AnalyticSurface;
import mhd_world.analytics.AnalyticSurfaceLegend;
import mhd_world.analytics.AnalyticSurface.GridPointAttributes;

public class SimplestExample extends JFrame
{
	 private Sector sector;
	 
	public SimplestExample()
    {
		final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new java.awt.Dimension(800, 600));
        this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
        
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        m.setShowWireframeExterior(true);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(true);
        
        wwd.setModel(m);
        
        
       //get the layer list of this model and setup the layer contoller of this model
         Layer layer;
         layer=new LayerManagerLayer(wwd);
         m.getLayers().add(layer);
         
         // add the renderable layer on this model
         RenderableLayer layer_mhd = new RenderableLayer();
         m.getLayers().add(layer_mhd);
         

        // Add an image
        SurfaceImage si = new SurfaceImage("images/400x230-splash-nww.png", Sector.fromDegrees(35, 45, -115, -95));      
        layer_mhd.addRenderable(si);
        
        
        // add an sector        
        SurfaceSector sc=new SurfaceSector(Sector.fromDegrees(35, 36, -115, -114));
        layer_mhd.addRenderable(sc);
        
        
        //add analyticsurface
        double HUE_BLUE = 0d / 360d;
        double HUE_RED =  240d / 360d;        
        double minValue = Double.MAX_VALUE;
        double maxValue = -Double.MAX_VALUE;
        
        AnalyticSurface surface = new AnalyticSurface();
        surface.setSector(Sector.fromDegrees(35, 36, -115, -114));
        surface.setAltitude(100);
        
        surface.setDimensions(50, 50);
        surface.setClientLayer(layer_mhd);
        layer_mhd.addRenderable(surface); 
             
        ArrayList<AnalyticSurface.GridPointAttributes> attributesList
        = new ArrayList<AnalyticSurface.GridPointAttributes>();         

               
        double latStep = -surface.getSector().getDeltaLatDegrees() / 49;
        double lonStep = surface.getSector().getDeltaLonDegrees() / 49;
        double lat = surface.getSector().getMaxLatitude().degrees;
        Position[][] position=new  Position[50][50];
        ArrayList<Position> positions3 = new ArrayList<Position>();

        for (int y = 0; y < 50; y++)
        {
         double lon = surface.getSector().getMinLongitude().degrees;
            for (int x = 0; x < 50; x++)
            {
                      
                double e = m.getGlobe().getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));  
                position[x][y]=new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon),e*10+100);  
                positions3.add(position[x][y]);
                if (minValue > e*10)
                    minValue = e*10;
                if (maxValue < e*10)
                    maxValue = e*10;
                   lon += lonStep;
            }
            lat += latStep;
        }  
       
       lat = surface.getSector().getMaxLatitude().degrees;        
        for (int y = 0; y < 50; y++)
        {
         double lon = surface.getSector().getMinLongitude().degrees;
            for (int x = 0; x < 50; x++)
            {              
                double e = m.getGlobe().getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));                
                double hueFactor = WWMath.computeInterpolationFactor(e*10, minValue, maxValue);               
                System.out.println(hueFactor);
                Color color = Color.getHSBColor((float) WWMath.mixSmooth(hueFactor,HUE_RED,HUE_BLUE), 1f, 1f);
                attributesList.add( AnalyticSurface.createGridPointAttributes(e*10,color ,1)); 
                lon += lonStep;
            }
            lat += latStep;
        } 
        
        
        Polyline pline01=new Polyline(positions3);
        layer_mhd.addRenderable(pline01);        
        surface.setValues(attributesList);
       
       
        
 
        // create the color legend
          final double altitude = surface.getAltitude();
          final double verticalScale = surface.getVerticalScale();          
          Format legendLabelFormat = new DecimalFormat("# km")
          {
              public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
              {
                  double altitudeMeters = altitude + verticalScale * number;
                  double altitudeKm = altitudeMeters * WWMath.METERS_TO_KILOMETERS;
                  return super.format(altitudeKm, result, fieldPosition);
              }
          };
          AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(minValue, maxValue, HUE_RED, HUE_BLUE,
              AnalyticSurfaceLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
              AnalyticSurfaceLegend.createDefaultTitle("City Altitudes"));
          
          legend.setOpacity(0.8);
          legend.setScreenLocation(new Point(50, 300));
          legend.setClientLayer(layer_mhd);
          layer_mhd.addRenderable(createLegendRenderable(surface, 300, legend));
       

       
        
        //add a sphere shape
        LatLon position01 = new LatLon(Angle.fromDegrees(47.7477), Angle.fromDegrees(-123.6372));        
        Vec4 centerPoint = m.getGlobe().computePointFromPosition(position01.getLatitude(), position01.getLongitude(),500000);
        Sphere_Moving sphare=new  Sphere_Moving (centerPoint, 500000);
        layer_mhd.addRenderable(sphare);
        
        
        
        //add a pline
        double elevation = 10e3;
        
        ArrayList<Position> positions2 = new ArrayList<Position>();
        positions2.add(new Position(Angle.fromDegrees(0), Angle.fromDegrees(-150), elevation));
        positions2.add(new Position(Angle.fromDegrees(25), Angle.fromDegrees(-75), elevation));
        positions2.add(new Position(Angle.fromDegrees(50), Angle.fromDegrees(0), elevation));        
        Polyline pline=new Polyline(positions2);
        layer_mhd.addRenderable(pline);
        
        
        
        
        
        
           // add a airspace layer on this model
        AirspaceLayer airspace=new AirspaceLayer();
        m.getLayers().add(airspace);
       
        
        //add a Cylinder
        CappedCylinder cyl = new CappedCylinder();
        cyl.setCenter(LatLon.fromDegrees(47.7477, -123.6372));
        cyl.setRadius(300000.0);
        cyl.setAltitudes(5000.0, 1000000.0);
        airspace.addAirspace(cyl);
        
        

        // get the object of globe
        Globe my_globe;
        my_globe=wwd.getModel().getGlobe();
        
        
        
        
        
        
        
        // Set up to drag listener
        wwd.addSelectListener(new SelectListener()
        {
            private BasicDragger dragger = new BasicDragger(wwd);

            public void selected(SelectEvent event)
            {
                // Delegate dragging computations to a dragger.
                this.dragger.selected(event);
            }
        }); 
    
        
        ArrayList<LatLon> positions = new ArrayList<LatLon>();
        positions.add(LatLon.fromDegrees(44.16, 6.82));
        positions.add(LatLon.fromDegrees(44.16, 7.09));
        positions.add(LatLon.fromDegrees(44.30, 6.95));
        positions.add(LatLon.fromDegrees(44.16, 6.82));
        sector = Sector.boundingSector(positions);
        System.out.println(sector != null ? sector : "no sector");
        System.out.println(sector.getMinLatitude());
        System.out.println(sector.getMaxLatitude());
        
        
        
        
      
    
    
    }
	
	
	  
	
	Renderable createLegendRenderable(final AnalyticSurface surface, final double surfaceMinScreenSize,
   	        final AnalyticSurfaceLegend legend)
   	    {
   	        return new Renderable()
   	        {
   	            public void render(DrawContext dc)
   	            {
   	                Extent extent = surface.getExtent(dc);
   	                if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
   	                    return;

   	                if (WWMath.computeSizeInWindowCoordinates(dc, extent) < surfaceMinScreenSize)
   	                    return;

   	                legend.render(dc);
   	            }
   	        };
   	    }
	
	

    public static void main(String[] args)
    
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                JFrame frame = new SimplestExample();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    
    
    public Vec4 computePointFromPosition(DrawContext dc, Angle latitude, Angle longitude, double elevation,
            boolean terrainConformant)
        {
           
           
          
            double newElevation = elevation;           

            return dc.getGlobe().computePointFromPosition(latitude, longitude, newElevation);
        }
    
}