/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Arrays;

/**
 * This example illustrates how you might show video on the globe's surface. It uses a SurfaceImage to display one image
 * after another, each of which could correspond to a frame of video. The video frames are simulated by creating a new
 * buffered image for each frame. The same SurfaceImage is used. The image source of the SurfaceImage is continually set
 * to a new BufferedImage. (It would be more efficient to also re-use a single BufferedImage, but one objective of this
 * example is to show how to do this when the image can't be re-used.) The SurfaceImage location could also be
 * continually changed, but this example doesn't do that.
 *
 * @author tag
 * @version $Id: VideoOnTerrain.java 12859 2009-12-08 11:09:08Z patrickmurris $
 */
public class VideoOnTerrain extends ApplicationTemplate
{
    private static final int IMAGE_SIZE = 512;

    // These corners do not form a Sector, so SurfaceImage must generate a texture rather than simply using the source
    // image.
    private static final java.util.List<LatLon> CORNERS = Arrays.asList(
        LatLon.fromDegrees(37.8313, -105.0653),
        LatLon.fromDegrees(37.8313, -105.0396),
        LatLon.fromDegrees(37.8539, -105.04),
        LatLon.fromDegrees(37.8539, -105.0653)
    );

    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, true);

            RenderableLayer layer = new RenderableLayer();
            layer.setName("Video on terrain");
            layer.setOpacity(0.5);
            layer.setPickEnabled(false);
            insertBeforePlacenames(this.getWwd(), layer);
            this.layerPanel.update(this.getWwd()); // makes the ApplicationTemplate layer list show the new layer

            final SurfaceImage surfaceImage = new SurfaceImage(makeImage(), CORNERS);
            layer.addRenderable(surfaceImage);

            javax.swing.Timer timer = new javax.swing.Timer(50, new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    surfaceImage.setImageSource(makeImage(), CORNERS);
                    getWwd().redraw();
                }
            });
            timer.start();
        }

        private long counter;
        private long start = System.currentTimeMillis();

        private BufferedImage makeImage()
        {
            BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = image.createGraphics();

            g.setPaint(Color.WHITE);
            g.fill3DRect(0, 0, IMAGE_SIZE, IMAGE_SIZE, false);

            g.setPaint(Color.RED);
            g.setFont(Font.decode("ARIAL-BOLD-50"));

            g.drawString(Long.toString(++this.counter) + " frames", 10, IMAGE_SIZE / 4);
            g.drawString(Long.toString((System.currentTimeMillis() - start) / 1000) + " sec", 10, IMAGE_SIZE / 2);
            g.drawString("Heap:" + Long.toString(Runtime.getRuntime().totalMemory()), 10, 3 * IMAGE_SIZE / 4);

            g.dispose();

            return image;
        }
    }

    public static void main(String[] args)
    {
		Configuration.setValue(AVKey.INITIAL_LATITUDE, 37.8432);
		Configuration.setValue(AVKey.INITIAL_LONGITUDE, -105.0527);
		Configuration.setValue(AVKey.INITIAL_ALTITUDE, 7000);
        ApplicationTemplate.start("World Wind Video on Terrain", AppFrame.class);
    }
}
