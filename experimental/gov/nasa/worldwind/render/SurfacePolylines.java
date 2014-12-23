/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import java.util.*;

/**
 * This class renders fast multiple surface polylines in one pass. It relies on a {@link CompoundVecBuffer}.
 *
 * @author Dave Collins
 * @author Patrick Murris
 * @version $Id: SurfacePolylines.java 12829 2009-11-26 15:35:39Z patrickmurris $
 */
public class SurfacePolylines extends AbstractSurfaceShape implements Disposable
{
    protected Iterable<? extends Sector> sectors;
    protected CompoundVecBuffer buffer;
    protected Integer[] outlineDisplayLists;
    protected boolean needsOutlineTessellation = true;
    protected boolean crossesDateLine = false;

    public SurfacePolylines(CompoundVecBuffer buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.buffer = buffer;
    }

    public SurfacePolylines(Sector sector, CompoundVecBuffer buffer)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.sectors = Arrays.asList(sector);
        this.buffer = buffer;
    }

    /**
     * Get the underlying {@link CompoundVecBuffer} describing the geometry.
     *
     * @return the underlying {@link CompoundVecBuffer}.
     */
    public CompoundVecBuffer getBuffer()
    {
        return this.buffer;
    }

    public Iterable<? extends LatLon> getLocations(Globe globe)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.getLocations();
    }

    protected Iterable<? extends LatLon> getLocations(Globe globe, double edgeIntervalsPerDegree)
    {
        return getLocations(globe);
    }


    public Iterable<? extends LatLon> getLocations()
    {
        return this.buffer.getLocations();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setLocations(Iterable<? extends LatLon> iterable)
    {
        throw new UnsupportedOperationException();
    }


    public Position getReferencePosition()
    {
        Iterator<? extends LatLon> iterator = this.getLocations().iterator();
        if (iterator.hasNext())
            return new Position(iterator.next(), 0);

        return null;
    }

    protected Iterable<? extends Sector> doGetSectors(DrawContext dc, double texelSizeRadians)
    {
        if (this.sectors == null)
            this.sectors = getSurfaceShapeSupport().computeBoundingSectors(this.getLocations(), this.getPathType());

        return getSurfaceShapeSupport().adjustSectorsByBorderWidth(
            this.sectors, this.attributes.getOutlineWidth(), texelSizeRadians);
    }

    protected void doMoveTo(Position oldReferencePosition, Position newReferencePosition)
    {
        getSurfaceShapeSupport().doMoveTo(this.getBuffer(), oldReferencePosition, newReferencePosition);
        this.onGeometryChanged();
    }

    protected void onGeometryChanged()
    {
        this.sectors = null;
        this.needsOutlineTessellation = true;
        super.onShapeChanged();
    }

    public void dispose()
    {
        GLContext glContext = GLContext.getCurrent();
        if (glContext == null)
            return;

        this.disposeOfOutlineDisplayLists(glContext.getGL());
    }

    protected void disposeOfOutlineDisplayLists(GL gl)
    {
        if (this.outlineDisplayLists != null && this.outlineDisplayLists.length > 0)
        {
            for (int list : this.outlineDisplayLists)
                gl.glDeleteLists(list, 1);

            this.outlineDisplayLists = null;
        }
    }


    protected void assembleRenderState(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Intentionally left blank in order to override the superclass behavior with nothing.
    }

    protected void doRenderInteriorToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        // Polyline does not render an interior.
    }

    protected void doRenderOutlineToRegion(DrawContext dc, Sector sector, int x, int y, int width, int height)
    {
        getSurfaceShapeSupport().applyOutlineState(dc, this.attributes);
        this.drawOutline(dc, (int)Math.signum(sector.getCentroid().getLongitude().degrees));
    }

    protected void drawOutline(DrawContext dc, int hemisphereSign)
    {
        GL gl = dc.getGL();

        if (this.outlineDisplayLists == null || this.needsOutlineTessellation)
        {
            tessellateOutline(dc);
        }

        this.drawLists(gl, this.outlineDisplayLists);
        if (this.crossesDateLine)
        {
            // Apply hemisphere offset and draw again
            gl.glTranslated(360 * hemisphereSign, 0, 0);
            this.drawLists(gl, this.outlineDisplayLists);
            gl.glTranslated(-360 * hemisphereSign, 0, 0);
        }
    }

    protected void drawLists(GL gl, Integer[] lists)
    {
        for (Integer list : lists)
            gl.glCallList(list);
    }

    protected void tessellateOutline(DrawContext dc)
    {
        GL gl = dc.getGL();
        this.disposeOfOutlineDisplayLists(gl);
        ArrayList<Integer> displayLists = new ArrayList<Integer>();
        this.crossesDateLine = false;

        // Tessellate each part, note if crossing date line
        for (int i = 0; i < this.buffer.getNumSubBuffers(); i++)
            if (this.tessellatePart(gl, this.buffer.getSubBuffer(i), displayLists))
                this.crossesDateLine = true;

        this.outlineDisplayLists = new Integer[displayLists.size()];
        displayLists.toArray(this.outlineDisplayLists);
        this.needsOutlineTessellation = false;
    }

    protected boolean tessellatePart(GL gl, VecBuffer vecBuffer, List<Integer> displayLists)
    {
        int list = gl.glGenLists(1);
        gl.glNewList(list, GL.GL_COMPILE);
        gl.glBegin(GL.GL_LINE_STRIP);

        Iterable<double[]> iterable = vecBuffer.getCoords(3);
        boolean dateLineCrossed = false;
        int sign = 0; // hemisphere offset direction
        double previousLongitude = 0;
        for (double[] coords : iterable)
        {
            if (Math.abs(previousLongitude - coords[0]) > 180)
            {
                // Crossing date line, sum departure point longitude sign for hemisphere offset
                sign += (int)Math.signum(previousLongitude);
                dateLineCrossed = true;
            }
            previousLongitude = coords[0];
            coords[0] += sign * 360; // apply hemisphere offset
            gl.glVertex3dv(coords, 0);
        }

        gl.glEnd();
        gl.glEndList();
        displayLists.add(list);

        return dateLineCrossed;
    }

}
