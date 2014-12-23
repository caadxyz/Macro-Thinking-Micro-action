/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceRenderer;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerRenderer;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: MarkerLayer.java 12781 2009-11-10 04:39:00Z tgaskins $
 */
public class ActivityMarkerLayer extends AbstractLayer
{
	private final java.util.Collection<Marker>  markers = new java.util.concurrent.ConcurrentLinkedQueue<Marker>();
	private Iterable<Marker> markersOverride;
	private MarkerRenderer markerRenderer = new MarkerRenderer();
    

    public ActivityMarkerLayer()
    {
    }

 
    public void addMarker(Marker marker)
    {
        if (marker == null)
        {
            String msg = "nullValue.AirspaceIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.markersOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.markers.add(marker);
    }
    
    
    public void addMarkers(Iterable<Marker> markers)
    {
        if (markers == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.markersOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        for (Marker marker : markers)
        {
            // Internal list of airspaces does not accept null values.
            if (marker != null)
                this.markers.add(marker);
        }
    }
    
    
    public void removeMarker(Marker marker)
    {
        if (marker == null)
        {
            String msg = "nullValue.AirspaceIsNull";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (this.markersOverride != null)
        {
            String msg = Logging.getMessage("generic.LayerIsUsingCustomIterable");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        this.markers.remove(marker);
    }

    
    

    public Iterable<Marker> getMarkers()
    {
        return markers;
    }

    public void setMarkers(Iterable<Marker> markers)
    {   this.markersOverride = markers;
        clearMarkers();;
    }
    
    private void clearMarkers()
    {
        if (this.markers != null && this.markers.size() > 0)
            this.markers.clear();
    }

    


    public double getElevation()
    {
        return this.getMarkerRenderer().getElevation();
    }

    public void setElevation(double elevation)
    {
        this.getMarkerRenderer().setElevation(elevation);
    }

    public boolean isOverrideMarkerElevation()
    {
        return this.getMarkerRenderer().isOverrideMarkerElevation();
    }

    public void setOverrideMarkerElevation(boolean overrideMarkerElevation)
    {
        this.getMarkerRenderer().setOverrideMarkerElevation(overrideMarkerElevation);
    }

    public boolean isKeepSeparated()
    {
        return this.getMarkerRenderer().isKeepSeparated();
    }

    public void setKeepSeparated(boolean keepSeparated)
    {
        this.getMarkerRenderer().setKeepSeparated(keepSeparated);
    }

    public boolean isEnablePickSizeReturn()
    {
        return this.getMarkerRenderer().isEnablePickSizeReturn();
    }

    public void setEnablePickSizeReturn(boolean enablePickSizeReturn)
    {
        this.getMarkerRenderer().setEnablePickSizeReturn(enablePickSizeReturn);
    }

    /**
     * Opacity is not applied to layers of this type because each marker has an attribute set with opacity control.
     *
     * @param opacity the current opacity value, which is ignored by this layer.
     */
    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
    }

    /**
     * Returns the layer's opacity value, which is ignored by this layer because each of its markers has an attribute
     * with its own opacity control.
     *
     * @return The layer opacity, a value between 0 and 1.
     */
    @Override
    public double getOpacity()
    {
        return super.getOpacity();
    }

    protected MarkerRenderer getMarkerRenderer()
    {
        return markerRenderer;
    }

    protected void setMarkerRenderer(MarkerRenderer markerRenderer)
    {
        this.markerRenderer = markerRenderer;
    }

    protected void doRender(DrawContext dc)
    {
        this.draw(dc, null);
    }

    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.draw(dc, pickPoint);
    }

    protected void draw(DrawContext dc, java.awt.Point pickPoint)
    {
        if (this.markers == null)
            return;

        if (dc.getVisibleSector() == null)
            return;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return;

        if (dc.isPickingMode())
            this.getMarkerRenderer().pick(dc, this.markers, pickPoint, this);
        else
            this.getMarkerRenderer().render(dc, this.markers);
    }

    @Override
    public String toString()
    {
        return Logging.getMessage("layers.MarkerLayer.Name");
    }
}
