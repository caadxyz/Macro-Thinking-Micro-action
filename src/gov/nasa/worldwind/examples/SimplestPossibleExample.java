package gov.nasa.worldwind.examples;

import java.util.ArrayList;

import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.nasa.worldwind.*;

import javax.swing.*;

public class SimplestPossibleExample extends JFrame
{
    public SimplestPossibleExample()
    {
        WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
        this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
        wwd.setModel(new BasicModel());
        
        
        LatLon latlon;
        latlon=LatLon.fromDegrees(-90, 0);
        Position position=new Position(latlon,0);
        Vec4 vec;
        Vec4 vec_flat;
        vec=wwd.getModel().getGlobe().computePointFromLocation(latlon);
        MarkerLayer markerLayer=new MarkerLayer();
        Marker marker;
        marker=new BasicMarker(position,new  BasicMarkerAttributes() );
        ArrayList<Marker> markers=new ArrayList<Marker>();
        markers.add(marker);
        markerLayer.setMarkers(markers);
        wwd.getModel().getLayers().add(markerLayer);
        wwd.update(getGraphics());
        EarthFlat flatGlobe;
        flatGlobe=new EarthFlat();
        vec_flat=flatGlobe.toCartesian(Angle.fromDegrees(2),Angle.fromDegrees(0),0);
        System.out.println(flatGlobe.getProjection());
        System.out.println(vec);
        System.out.println(vec_flat);
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                JFrame frame = new SimplestPossibleExample();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
               
                
                

            }
        });
    }
}