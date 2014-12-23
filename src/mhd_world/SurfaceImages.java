/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package mhd_world;

import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;

import javax.imageio.spi.IIORegistry;
import java.awt.*;
import java.util.*;

/**
 * @author tag
 * @version $Id$
 */
public class SurfaceImages extends ApplicationTemplate
{
    static
    {
        IIORegistry reg = IIORegistry.getDefaultInstance();
        reg.registerServiceProvider(GeotiffImageReaderSpi.inst());
    }
   
    private static final String TEST_PATTERN = "E:/java_learning/worldwind13007/src/images/white.png";
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, false);

            try
            {
               
                SurfaceImage si2 = new SurfaceImage(TEST_PATTERN, new ArrayList<LatLon>(Arrays.asList(
                    LatLon.fromDegrees(-90, -180),
                    LatLon.fromDegrees(-90, 180),
                    LatLon.fromDegrees(90, 180),
                    LatLon.fromDegrees(90, -180)
                    )));
              
                RenderableLayer layer = new RenderableLayer();
                layer.setName("Surface Images");
                layer.setPickEnabled(false);
                layer.addRenderable(si2);                
                insertBeforeLayerName(this.getWwd(), layer,"Stars");
                this.getLayerPanel().update(this.getWwd());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Surface Images", SurfaceImages.AppFrame.class);
    }
}
