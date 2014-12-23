package gov.nasa.worldwind.applications.sar;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.airspaces.Polygon;
import gov.nasa.worldwind.render.WWTexture;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.util.OGLStackHandler;

import javax.media.opengl.GL;
import java.util.List;

/**
 * Renders a textured plane at a given elevation.
 *
 * @author Patrick Murris
 * @version $Id: ElevationPlane.java 11126 2009-05-13 14:28:07Z tgaskins $
 */
public class ElevationPlane extends Polygon
{
    private Object imageSource;
    protected WWTexture texture;
    private double imageSize = 500; // meter

    protected OGLStackHandler osh = new OGLStackHandler();

    public ElevationPlane()
    {
        this.getRenderer().setEnableLighting(false);
    }

    /**
     * Get the source for the fill pattern image. Can be a file path to a local image or
     * a {@link java.awt.image.BufferedImage} reference.
     *
     * @return the source for the fill pattern image - can be null.
     */
    public Object getImageSource()
    {
        return this.imageSource;
    }

    /**
     * Set the source for the fill pattern image. Can be a file path to a local image or
     * a {@link java.awt.image.BufferedImage} reference.
     *
     * @param imageSource the source for the fill pattern image - can be null.
     */
    public void setImageSource(Object imageSource)
    {
        this.imageSource = imageSource;
        this.texture = null;
    }

    /**
     * Get the real world image size in meter. The image source is repeated so that one tile covers this distance.
     *
     * @return the real world image size in meter.
     */
    public double getImageSize()
    {
        return this.imageSize;
    }

    /**
     * Set the real world image size in meter. The image source will be repeated so that one tile will
     * covers this distance.
     *
     * @param sizeInMeter the real world image size in meter.
     */
    public void setImageSize(double sizeInMeter)
    {
        this.imageSize = sizeInMeter;
    }

    // Airspace Polygon overload
    protected void doRenderGeometry(DrawContext dc, String drawStyle, List<LatLon> locations, List<Boolean> edgeFlags)
    {
        this.beginRendering(dc);
        try
        {
            // Setup texture coordinates generation
            this.applyTextureState(dc);

            // Disable writing to depth buffer
            // TODO: let the application decide whether the plane should be translucent?
            dc.getGL().glDepthMask(false);
            
            // Draw
            super.doRenderGeometry(dc, drawStyle, locations, edgeFlags);
        }
        finally
        {
            this.endRendering(dc);
        }
    }

    protected void beginRendering(DrawContext dc)
    {
        // TODO: review attributes
        GL gl = dc.getGL();
        osh.pushAttrib(gl, GL.GL_COLOR_BUFFER_BIT // for alpha func
            | GL.GL_ENABLE_BIT
            | GL.GL_CURRENT_BIT
            | GL.GL_DEPTH_BUFFER_BIT // for depth func
            | GL.GL_TEXTURE_BIT // for texture env
            | GL.GL_TRANSFORM_BIT);
        osh.pushTextureIdentity(gl);
    }

    protected void endRendering(DrawContext dc)
    {
        osh.pop(dc.getGL());
    }
    
    protected void applyTextureState(DrawContext dc)
    {
        WWTexture texture = getTexture();
        if (texture == null)
            return;

        if (!texture.bind(dc))
            return;

        GL gl = dc.getGL();
        // Texture coordinates generation
        double[][] planes = this.computePlanes(dc);
        gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
        gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_OBJECT_LINEAR);
        gl.glTexGendv(GL.GL_S, GL.GL_OBJECT_PLANE, planes[0], 0);
        gl.glTexGendv(GL.GL_T, GL.GL_OBJECT_PLANE, planes[1], 0);
        gl.glEnable(GL.GL_TEXTURE_GEN_S);
        gl.glEnable(GL.GL_TEXTURE_GEN_T);
        // Pattern scaling
        gl.glMatrixMode(GL.GL_TEXTURE_MATRIX);
        gl.glScaled(1 / this.imageSize, 1 / this.imageSize, 1f);
        // Texture setup
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        // TODO: factor in polygon opacity?
    }

    protected double[][] computePlanes(DrawContext dc)
    {
        double[][] planes = new double[2][4];
        // Compute two planes perpendicular to the polygon at its reference position.
        Position center = this.getReferencePosition();
        Vec4 north = dc.getGlobe().computeNorthPointingTangentAtLocation(center.latitude, center.longitude);
        Vec4 normal = dc.getGlobe().computeSurfaceNormalAtLocation(center.latitude, center.longitude);
        Vec4 east = north.cross3(normal);
        north.toArray4(planes[0], 0); // texture coordinate s
        east.toArray4(planes[1], 0);  // texture coordinate t

        return planes;
    }

    protected WWTexture getTexture()
    {
        if (this.texture == null && this.imageSource != null)
            this.texture = new BasicWWTexture(this.imageSource);

        return this.texture;
    }


}
