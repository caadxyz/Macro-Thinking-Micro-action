package mhd_world;

import java.awt.Color;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;

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

public class TestExample extends JFrame
{
	
	private java.util.Timer timer;
	private java.util.TimerTask task;
	
	public TestExample()
    {
		
		this.timer = new Timer();
	    
		final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new java.awt.Dimension(800, 600));
        this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
        
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        m.setShowWireframeExterior(true);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(false);
        wwd.setModel(m);
        
        
       //get the layer list of this model and setup the layer contoller of this model
         Layer layer;
         layer=new LayerManagerLayer(wwd);
         m.getLayers().add(layer);
         
         // add the renderable layer on this model
         RenderableLayer layer_mhd = new RenderableLayer();
         m.getLayers().add(layer_mhd);

       
        //add a sphere shape
        LatLon position01 = new LatLon(Angle.fromDegrees(47.7477), Angle.fromDegrees(-123.6372));        
        Vec4 centerPoint = m.getGlobe().computePointFromPosition(position01.getLatitude(), position01.getLongitude(),500000);
        this.task = new Sphere_Moving(centerPoint, 500000);
        //Sphere_Moving sphare=new  Sphere_Moving ();
        layer_mhd.addRenderable((Sphere_Moving) this.task);
        
    
        
        
        // add a airspace layer on this model
        AirspaceLayer airspace=new AirspaceLayer();
        m.getLayers().add(airspace);
       
        
        //add a Cylinder
        CappedCylinder cyl = new CappedCylinder();
        cyl.setCenter(LatLon.fromDegrees(47.7477, -123.6372));
        cyl.setRadius(300000.0);
        cyl.setAltitudes(5000.0, 1000000.0);
        airspace.addAirspace(cyl);
        
        

        
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
    

      
    
    
    }
	
	public void start(int delay, int internal) {
	    timer.schedule(task, delay * 1000, internal * 1000);//利用timer.schedule方法
	  }
	
	

    public static void main(String[] args)
    
    {
    	
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
            	TestExample frame = new TestExample();
                frame.start(1,1);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
    
    

    
}