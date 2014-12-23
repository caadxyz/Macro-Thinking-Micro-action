/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import javax.media.opengl.GL;
import java.awt.*;
import java.util.Arrays;

/**
 * Surface renderable.
 *
 * @author Patrick Murris
 * @version $Id: AbstractSurfaceRenderable.java 12645 2009-09-24 17:37:14Z dcollins $
 */
public abstract class AbstractSurfaceRenderable extends AbstractSurfaceObject implements PreRenderable, Renderable
{
    private double opacity = 1d;

    protected TiledSurfaceObjectRenderer renderer;
    protected final OGLStateSupport stateSupport = new OGLStateSupport();
    protected final PickSupport pickSupport = new PickSupport();

    //*** SurfaceObject

    abstract public Iterable<? extends Sector> getSectors(DrawContext dc, double texelSizeRadians);

    abstract public void doRenderToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height);

    public double getOpacity()
    {
        return this.opacity;
    }

    public void setOpacity(double opacity)
    {
        this.opacity = opacity < 0 ? 0 : opacity > 1 ? 1 : opacity;  // clamp to 0..1
        this.updateModifiedTime();
    }

    //*** PreRender and render

    public void preRender(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.getRenderer().preRender(dc);
    }

    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.getRenderer().render(dc);
    }

    protected TiledSurfaceObjectRenderer getRenderer()
    {
        if (this.renderer == null)
        {
            this.renderer = new TiledSurfaceObjectRenderer();
            this.renderer.setPickEnabled(false);
            this.renderer.setSurfaceObjects(java.util.Arrays.asList(this));
        }

        return this.renderer;
    }

    // *** Utility methods

    protected Angle getViewHeading(DrawContext dc)
    {
        Angle heading = Angle.ZERO;
        if (dc.getView() instanceof OrbitView)
            heading = dc.getView().getHeading();
        return heading;
    }

    protected double computePixelSizeAtLocation(DrawContext dc, LatLon location)
    {
        Globe globe = dc.getGlobe();
        Vec4 locationPoint = globe.computePointFromPosition(location.getLatitude(), location.getLongitude(),
            globe.getElevation(location.getLatitude(), location.getLongitude()));
        double distance = dc.getView().getEyePoint().distanceTo3(locationPoint);
        return dc.getView().computePixelSizeAtDistance(distance);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected double computeDrawPixelSize(DrawContext dc, Sector sector, int width, int height)
    {
        return dc.getGlobe().getRadius() * sector.getDeltaLatRadians() / height;
    }

    protected Vec4 computeDrawPoint(LatLon location, Sector sector, int x, int y, int width, int height)
    {
        Matrix transform = Matrix.fromGeographicToViewport(sector, x, y, width, height);
        Vec4 point = new Vec4(location.getLongitude().degrees, location.getLatitude().degrees, 1);
        return point.transformBy4(transform);
    }

    protected Sector computeRotatedSectorBounds(Sector sector, LatLon location, Angle heading)
    {
        if (Math.abs(heading.degrees) < .001)
            return sector;

        LatLon[] corners = new LatLon[] {
            new LatLon(sector.getMaxLatitude(), sector.getMinLongitude()),  // nw
            new LatLon(sector.getMaxLatitude(), sector.getMaxLongitude()),  // ne
            new LatLon(sector.getMinLatitude(), sector.getMinLongitude()),  // sw
            new LatLon(sector.getMinLatitude(), sector.getMaxLongitude()),  // se
        };
        // Rotate corners around location
        for (int i = 0; i < corners.length; i++)
        {
            Angle azimuth = LatLon.greatCircleAzimuth(location, corners[i]);
            Angle distance = LatLon.greatCircleDistance(location, corners[i]);
            corners[i] = LatLon.greatCircleEndPosition(location, azimuth.add(heading), distance);
        }

        return Sector.boundingSector(Arrays.asList(corners));
    }

    protected void applyPremultipliedAlphaColor(GL gl, Color color, double opacity)
    {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] = (float) WWMath.clamp(opacity, 0, 1);
        compArray[0] *= compArray[3];
        compArray[1] *= compArray[3];
        compArray[2] *= compArray[3];
        gl.glColor4fv(compArray, 0);
    }

    protected void applyNonPremultipliedAlphaColor(GL gl, Color color, double opacity)
    {
        float[] compArray = new float[4];
        color.getRGBComponents(compArray);
        compArray[3] = (float) WWMath.clamp(opacity, 0, 1);
        gl.glColor4fv(compArray, 0);
    }
}
