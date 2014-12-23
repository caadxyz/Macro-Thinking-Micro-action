/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.markers.BasicMarker;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.nasa.worldwind.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: TreeFiltering.java 12471 2009-08-17 23:40:14Z tgaskins $
 */
public class TreeFiltering extends ApplicationTemplate
{
    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame() throws IOException, ParserConfigurationException, SAXException
        {
            super(true, true, false);

            final MyMarkerLayer layer = new MyMarkerLayer(this.makeDatabase());
            layer.setKeepSeparated(false);
            layer.setPickEnabled(true);
            insertBeforePlacenames(this.getWwd(), layer);
            this.getLayerPanel().update(this.getWwd());

            this.getWwd().addPositionListener(new PositionListener()
            {
                public void moved(PositionEvent event)
                {
                    layer.setCursorLocation(event.getPosition());
                }
            });
        }

        private SectorTree.MarkerTree makeDatabase()
        {
            int treeDepth = 5;
            int minLat = 23, maxLat = 50, latDelta = 1;
            int minLon = -130, maxLon = -70, lonDelta = 1;
            SectorTree.MarkerTree tree = new SectorTree.MarkerTree(treeDepth);
            
            MarkerAttributes attrs = new BasicMarkerAttributes();

            for (int lat = minLat; lat <= maxLat; lat += latDelta)
            {
                for (int lon = minLon; lon <= maxLon; lon += lonDelta)
                {
                    tree.add(new BasicMarker(Position.fromDegrees(lat, lon, 0), attrs));
                }
            }

            return tree;
        }
    }

    private static class MyMarkerLayer extends MarkerLayer
    {
        private static final double[] REGION_SIZES = new double[] {5, 2};
        private static final long TIME_LIMIT = 5; // ms

        private SectorTree.MarkerTree database;
        private Position position;
        private Iterable<Marker> markers;

        public MyMarkerLayer(SectorTree.MarkerTree database)
        {
            this.database = database;
            this.setOverrideMarkerElevation(true);
            this.setKeepSeparated(false);
        }

        public void setCursorLocation(Position position)
        {
            this.position = position;
        }

        protected void draw(DrawContext dc, Point pickPoint)
        {
            if (this.position == null)
                return;

            // Refresh the visibility tree only during the pick pass, or the display pass if picking is disabled
            if (!this.isPickEnabled() || dc.isPickingMode() || this.markers == null)
                this.markers = this.getVisibleMarkers(dc);

            this.setMarkers(this.markers);
            super.draw(dc, pickPoint);
        }

        private Iterable<Marker> getVisibleMarkers(DrawContext dc)
        {
            ArrayList<Marker> markers = new ArrayList<Marker>();
            for (Sector sector : dc.getVisibleSectors(REGION_SIZES, TIME_LIMIT, this.computeSector()))
            {
                this.database.getItems(sector, markers);
            }

            return markers;
        }

        private Sector computeSector()
        {
            double size = 5;
            double lat = this.position.getLatitude().degrees;
            double lon = this.position.getLongitude().degrees;
            double minLat = Math.max(lat - size, -90);
            double maxLat = Math.min(lat + size, 90);
            double minLon = Math.max(lon - size, -180);
            double maxLon = Math.min(lon + size, 180);

            return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Filtering by Region", AppFrame.class);
    }
}
