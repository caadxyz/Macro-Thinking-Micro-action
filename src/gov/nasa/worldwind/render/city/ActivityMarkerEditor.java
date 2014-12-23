package gov.nasa.worldwind.render.city;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;
import gov.nasa.worldwind.render.airspaces.*;
import gov.nasa.worldwind.render.city.editor.AbstractMarkerEditor;
import gov.nasa.worldwind.render.city.editor.BasicMarkerControlPoint;
import gov.nasa.worldwind.render.city.editor.BasicMarkerControlPointRenderer;
import gov.nasa.worldwind.render.city.editor.MarkerControlPoint;
import gov.nasa.worldwind.render.city.editor.MarkerControlPointRenderer;
import gov.nasa.worldwind.render.city.editor.MarkerEditor;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;

public class ActivityMarkerEditor extends AbstractMarkerEditor
	{
	    private ActivityMarker activityMarker = null; // Can be null
	    private double minRadius = 1.0;
	    private double maxRadius = Double.MAX_VALUE;
	    private boolean alwaysShowRadiusControl = false;
	    
	    //the radius control_draw_distance is a parmeter of distance for draw the controler 
	    private double radiusControlDrawDistance = 14;
        public static final int RADIUS_CONTROL_ID = 1024;

	    public ActivityMarkerEditor(MarkerControlPointRenderer renderer)
	    {
	        super(renderer);
	    }

	    public ActivityMarkerEditor()
	    {
	        this(getDefaultRenderer());
	    }

	    public static MarkerControlPointRenderer getDefaultRenderer()
	    {
	        BasicMarkerControlPointRenderer renderer = new BasicMarkerControlPointRenderer();
	        renderer.setControlPointMarker(createDefaultMarker());
	        renderer.setEnableDepthTest(false);
	        return renderer;
	    }

	    public static Marker createDefaultMarker()
	    {
	        // Create an opaque blue sphere. By default the sphere has a 12 pixel radius, but its radius must be at least
	        // 0.1 meters .
	        MarkerAttributes attributes = new BasicMarkerAttributes(Material.BLUE, BasicMarkerShape.SPHERE, 1.0, 12, 0.1);
	        return new BasicMarker(null, attributes, null);
	    }



	    public  ActivityMarker getMarker()
	    {
	        return this.activityMarker;
	    }

	    public void setMarker(ActivityMarker activityMarker)
	    {
	    	this.activityMarker = activityMarker;
	    }

	    public double getMinRadius()
	    {
	        return this.minRadius;
	    }

	    public void setMinRadius(double radius)
	    {
	        if (radius < 0.0)
	        {
	            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
	            Logging.logger().severe(message);
	            throw new IllegalArgumentException(message);
	        }

	        this.minRadius = radius;
	    }

	    public double getMaxRadius()
	    {
	        return this.maxRadius;
	    }

	    public void setMaxRadius(double radius)
	    {
	        if (radius < 0.0)
	        {
	            String message = Logging.getMessage("generic.ArgumentOutOfRange", "radius < 0");
	            Logging.logger().severe(message);
	            throw new IllegalArgumentException(message);
	        }

	        this.maxRadius = radius;
	    }

	    public boolean isAlwaysShowRadiusControl()
	    {
	        return this.alwaysShowRadiusControl;
	    }

	    public void setAlwaysShowRadiusControl(boolean alwaysShow)
	    {
	        this.alwaysShowRadiusControl = alwaysShow;
	    }

	    public double getRadiusControlDrawDistance()
	    {
	        return radiusControlDrawDistance;
	    }

	    public void setRadiusControlDrawDistance(double distance)
	    {
	        if (distance < 0.0)
	        {
	            String message = Logging.getMessage("generic.ArgumentOutOfRange", "distance < 0");
	            Logging.logger().severe(message);
	            throw new IllegalArgumentException(message);
	        }

	        this.radiusControlDrawDistance = distance;
	    }

	    //**************************************************************//
	    //********************  Control Point Assembly  ****************//
	    //**************************************************************//

	    
	    //very important part to Haidong Ma
	    protected void assembleControlPoints(DrawContext dc)
	    {
	        // If the cursor passes near the edge of the sphere, draw a tangent control point that can be used to
	        // adjust the sphere's radius.

	        if (this.getMarker() == null) return;
	        
	        Vec4 point;
	        point =dc.getGlobe().computePointFromPosition(this.getMarker().getPosition(),this.getMarker().getPosition().getElevation() );
	       
	        MarkerControlPoint controlPoint = new BasicMarkerControlPoint(this, this.getMarker(),
	                RADIUS_CONTROL_ID, point);
	        this.addControlPoint(dc, controlPoint);
	            
	        
	    }



	    //**************************************************************//
	    //********************  Control Point Events  ******************//
	    //**************************************************************//



	    protected void doRemoveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint)
	    {
	    }

	    protected void doMoveControlPoint(WorldWindow wwd, MarkerControlPoint controlPoint,
	        Point mousePoint, Point previousMousePoint)
	    {
	        if (controlPoint.getLocationIndex() == RADIUS_CONTROL_ID)
	        {
	        }
	    }

		@Override
		protected MarkerControlPoint doAddControlPoint(WorldWindow wwd,
				ActivityMarker marker, Point mousePoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void doResizeAtControlPoint(WorldWindow wwd,
				MarkerControlPoint controlPoint, Point mousePoint,
				Point previousMousePoint) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MarkerControlPoint addControlPoint(WorldWindow wwd,
				Marker marker, Point mousePoint) {
			// TODO Auto-generated method stub
			return null;
		}



	    


	}
