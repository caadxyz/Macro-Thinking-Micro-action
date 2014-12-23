/* Copyright (C) 2001, 2009 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package mhd_world.analytics;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.*;
import java.util.ArrayList;
import java.util.zip.*;

/**
 * @author dcollins
 * @version $Id: AnalyticSurfaceDemo.java 12867 2009-12-09 04:52:13Z tgaskins $
 */
public class AnalyticSurfaceDemo extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        protected static final double HUE_BLUE = 240d / 360d;
        protected static final double HUE_RED = 0d / 360d;
        protected RenderableLayer analyticSurfaceLayer;

        public AppFrame()
        {
            this.initAnalyticSurfaceLayer();
        }

        protected void initAnalyticSurfaceLayer()
        {
            this.analyticSurfaceLayer = new RenderableLayer();
            this.analyticSurfaceLayer.setPickEnabled(false);
            this.analyticSurfaceLayer.setName("Analytic Surfaces");
            insertBeforePlacenames(this.getWwd(), this.analyticSurfaceLayer);
            this.getLayerPanel().update(this.getWwd());

            createRandomAltitudeSurface(HUE_BLUE, HUE_RED, 50, 50, this.analyticSurfaceLayer);

          
        }
    }

    
    protected static Renderable createLegendRenderable(final AnalyticSurface surface, final double surfaceMinScreenSize,
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

    //**************************************************************//
    //********************  Random Altitude Surface  ***************//
    //**************************************************************//

    protected static void createRandomAltitudeSurface(double minHue, double maxHue, int width, int height,
        RenderableLayer outLayer)
    {
        double minValue = -200e3;
        double maxValue = 200e3;

        AnalyticSurface surface = new AnalyticSurface();
        surface.setSector(Sector.fromDegrees(25, 35, -90, -80));
        surface.setAltitude(400e3);
        surface.setDimensions(width, height);
        surface.setClientLayer(outLayer);
        outLayer.addRenderable(surface);

        BufferWrapper Buffer = randomGridValues(width, height, minValue, maxValue);
        mixValuesOverTime(2000L, Buffer, Buffer, minValue, maxValue, minHue, maxHue, surface);

        AnalyticSurfaceAttributes attr = new AnalyticSurfaceAttributes();
        attr.setShadowOpacity(0.5);
        surface.setSurfaceAttributes(attr);



        
        
        
        
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

        AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(minValue, maxValue, minHue, maxHue,
            AnalyticSurfaceLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
            AnalyticSurfaceLegend.createDefaultTitle("Random Altitudes"));
        legend.setOpacity(0.8);
        legend.setScreenLocation(new Point(650, 300));
        legend.setClientLayer(outLayer);
        outLayer.addRenderable(createLegendRenderable(surface, 300, legend));
    }

    protected static void mixValuesOverTime(
        final long timeToMix,
        final BufferWrapper firstBuffer, final BufferWrapper secondBuffer,
        final double minValue, final double maxValue, final double minHue, final double maxHue,
        final AnalyticSurface surface)
    {
        Timer timer = new Timer(20, new ActionListener()
        {
            private long startTime = -1;

            public void actionPerformed(ActionEvent e)
            {
                if (this.startTime < 0)
                    this.startTime = System.currentTimeMillis();

                double t = (double) (e.getWhen() - this.startTime) / (double) timeToMix;
                int ti = (int) Math.floor(t);

                double a = t - ti;
                if ((ti % 2) == 0)
                    a = 1d - a;

                surface.setValues(createMixedColorGradientGridValues(
                    a, firstBuffer, secondBuffer, minValue, maxValue, minHue, maxHue));

                if (surface.getClientLayer() != null)
                    surface.getClientLayer().firePropertyChange(AVKey.LAYER, null, surface.getClientLayer());
            }
        });
        timer.start();
    }

    
    public static Iterable<? extends AnalyticSurface.GridPointAttributes> createMixedColorGradientGridValues(double a,
        BufferWrapper firstBuffer, BufferWrapper secondBuffer, double minValue, double maxValue,
        double minHue, double maxHue)
    {
        ArrayList<AnalyticSurface.GridPointAttributes> attributesList
            = new ArrayList<AnalyticSurface.GridPointAttributes>();

        long length = Math.min(firstBuffer.length(), secondBuffer.length());
        for (int i = 0; i < length; i++)
        {
            double value = WWMath.mixSmooth(a, firstBuffer.getDouble(i), secondBuffer.getDouble(i));
            attributesList.add(
                AnalyticSurface.createColorGradientAttributes(value, minValue, maxValue, minHue, maxHue));
        }

        return attributesList;
    }


    //**************************************************************//
    //********************  Random Grid Construction  **************//
    //**************************************************************//

    protected static final int DEFAULT_RANDOM_ITERATIONS =1000;
    protected static final double DEFAULT_RANDOM_SMOOTHING = 0.5d;
   
    public static BufferWrapper randomGridValues(int width, int height, double min, double max)
    {
        
        int numValues = width * height;
        double[] values = new double[numValues];        
           
            for (int y = 0; y < height; y++)
            {               
                for (int x = 0; x < width; x++)
                {                 
                  values[x + y * width]= Math.random()*1000000;
                }
            }           
            
        BufferFactory factory=new BufferFactory.DoubleBufferFactory();
        BufferWrapper buffer = factory.newBuffer(numValues);
        buffer.putDouble(0, values, 0, numValues);
        return buffer;
        
        
    }
    
    
    



  

    public static void main(String[] args)
    {
        ApplicationTemplate.start("Analytic Surface Demo", AppFrame.class);
    }
}