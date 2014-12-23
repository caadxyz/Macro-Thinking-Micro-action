package gov.nasa.worldwind.render.city;

import java.awt.BorderLayout;
import java.awt.Dimension;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.city.Graphy;


import javax.swing.JFrame;


public class Papplet_Display {
	
    WorldWindowGLCanvas wwd;
	Papplet_Display(){
		this.wwd=new WorldWindowGLCanvas();
		wwd.setModel(new BasicModel()); 
		wwd.getModel().setShowWireframeExterior(true);
		
	}
	public static JFrame start()
	    {     

    	   
           
    	   try
	        {
	            final JFrame frame = new JFrame();
	            frame.setTitle("papplet display");
	            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	            frame.setLocation(200, 0);
	            Dimension d=new Dimension();
	            d.height=600;
	            d.width=800;	      
	            frame.setSize(d);
	            Graphy papplet =new Graphy();
	       	    papplet.init();
	       	    frame.add(papplet,BorderLayout.CENTER);
	            java.awt.EventQueue.invokeLater(new Runnable()
	            {
	                public void run()
	                {
	                    frame.setVisible(true);
	                }
	            });

	            return frame;
	        }
	        catch (Exception e)
	        {
	            e.printStackTrace();
	            return null;
	        }
	    }
       
       
       public Vec4 computePointFromPosition(DrawContext dc, Angle latitude, Angle longitude)
    	    { 
    	        return dc.getGlobe().computePointFromPosition(latitude, longitude, 0);
    	    }
	
     public static void main(String[] args)
    {
    	 Papplet_Display display=new Papplet_Display();
    	 
    	 //Papplet_Display.start();
    	 LatLon a=LatLon.fromDegrees(120,160);
    	 Vec4 vec;
    	 vec=display.wwd.getModel().getGlobe().computePointFromLocation(a);
    	 System.out.println(a);
    	 System.out.println(vec);
    	 System.out.println(vec.w);
    	 System.out.println(vec.x);
    	 System.out.println(vec.y);
    	 System.out.println(vec.z);
    	 
    }


}
