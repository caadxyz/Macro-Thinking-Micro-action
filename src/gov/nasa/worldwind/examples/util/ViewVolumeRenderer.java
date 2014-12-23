/* Copyright (C) 2001, 2010 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved. 
*/
package gov.nasa.worldwind.examples.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: ViewVolumeRenderer.java 12997 2010-01-09 09:56:35Z tgaskins $
 */
public class ViewVolumeRenderer
{
    protected double size = 1d;

    public ViewVolumeRenderer()
    {
    }

    public double getSize()
    {
        return this.size;
    }

    public void setSize(double size)
    {
        this.size = size;
    }

    public void render(DrawContext dc, Matrix modelview, Matrix projection, Rectangle viewport)
    {
        if (dc.isPickingMode())
            return;

        dc.addOrderedRenderable(new OrderedViewVolume(modelview, projection, viewport));
    }

    protected void draw(DrawContext dc, Matrix modelview, Matrix projection, Rectangle viewport)
    {
        this.drawClipVolume(dc, modelview, projection, viewport);
        this.drawAxes(dc, modelview);
    }

    protected void drawAxes(DrawContext dc, Matrix modelview)
    {
        Matrix modelviewInv = modelview.getInverse();
        Vec4 origin = Vec4.UNIT_W.transformBy4(modelviewInv);
        Vec4 x = origin.add3(Vec4.UNIT_X.transformBy4(modelviewInv).multiply3(this.getSize()));
        Vec4 y = origin.add3(Vec4.UNIT_Y.transformBy4(modelviewInv).multiply3(this.getSize()));
        Vec4 z = origin.add3(Vec4.UNIT_Z.transformBy4(modelviewInv).multiply3(this.getSize()));

        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL.GL_CURRENT_BIT
            | GL.GL_LINE_BIT
            | GL.GL_POINT_BIT);
        try
        {
            gl.glEnable(GL.GL_BLEND);
            gl.glLineWidth(1f);
            gl.glPointSize(5f);
            OGLUtil.applyBlending(gl, false);

            OGLUtil.applyColor(gl, Color.WHITE, false);
            gl.glBegin(GL.GL_POINTS);
            gl.glVertex3d(origin.x, origin.y, origin.z);
            gl.glEnd();

            OGLUtil.applyColor(gl, Color.RED, false);
            drawLine(dc, origin, x);
            OGLUtil.applyColor(gl, Color.GREEN, false);
            drawLine(dc, origin, y);
            OGLUtil.applyColor(gl, Color.BLUE, false);
            drawLine(dc, origin, z);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected void drawClipVolume(DrawContext dc, Matrix modelview, Matrix projection, Rectangle viewport)
    {
        GL gl = dc.getGL();
        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl,
            GL.GL_CURRENT_BIT | GL.GL_COLOR_BUFFER_BIT | GL.GL_LINE_BIT | GL.GL_ENABLE_BIT | GL.GL_DEPTH_BUFFER_BIT);
        try
        {
            gl.glLineWidth(1f);
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);
            drawVolume(dc, modelview, projection, viewport);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }

    protected static void drawVolume(DrawContext dc, Matrix modelview, Matrix projection, Rectangle viewport)
    {
        Vec4 eye = Vec4.UNIT_W.transformBy4(modelview.getInverse());
        Vec4 near_ll = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(0, 0, 0));
        Vec4 near_lr = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(viewport.width, 0, 0));
        Vec4 near_ur = worldPointFromScreenPoint(dc, viewport, modelview, projection,
            new Vec4(viewport.width, viewport.height, 0));
        Vec4 near_ul = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(0, viewport.height, 0));
        Vec4 far_ll = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(0, 0, 1));
        Vec4 far_lr = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(viewport.width, 0, 1));
        Vec4 far_ur = worldPointFromScreenPoint(dc, viewport, modelview, projection,
            new Vec4(viewport.width, viewport.height, 1));
        Vec4 far_ul = worldPointFromScreenPoint(dc, viewport, modelview, projection, new Vec4(0, viewport.height, 1));

        GL gl = dc.getGL();

        // Draw the active view volume
        OGLUtil.applyColor(gl, Color.YELLOW, 0.5, false);
        drawQuad(dc, near_ul, near_ur, far_ur, far_ul);
        drawQuad(dc, far_ll, near_ll, near_ul, far_ul);
        drawQuad(dc, far_ll, far_lr, near_lr, near_ll);
        drawQuad(dc, near_lr, far_lr, far_ur, near_ur);
        drawQuad(dc, near_ll, near_lr, near_ur, near_ul);
        drawQuad(dc, far_ll, far_lr, far_ur, far_ul);

        // Draw it again with GL_GREATER in order to draw the underground portion
        gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT);
        gl.glDepthFunc(GL.GL_GREATER);
        OGLUtil.applyColor(gl, Color.YELLOW, 0.2, false); // below ground portion is less opaque
        drawQuad(dc, near_ul, near_ur, far_ur, far_ul);
        drawQuad(dc, far_ll, near_ll, near_ul, far_ul);
        drawQuad(dc, far_ll, far_lr, near_lr, near_ll);
        drawQuad(dc, near_lr, far_lr, far_ur, near_ur);
        drawQuad(dc, near_ll, near_lr, near_ur, near_ul);
        drawQuad(dc, far_ll, far_lr, far_ur, far_ul);
        gl.glPopAttrib();

        // Draw the space between the eye and the near plane
        OGLUtil.applyColor(gl, Color.WHITE, 0.1, false);
        drawTriangle(dc, eye, near_ur, near_ul);
        drawTriangle(dc, eye, near_ul, near_ll);
        drawTriangle(dc, eye, near_ll, near_lr);
        drawTriangle(dc, eye, near_lr, near_ur);

        // Draw the frame of the active volume
        OGLUtil.applyColor(gl, Color.BLACK, 1d, false);
        drawLine(dc, near_ll, near_lr);
        drawLine(dc, near_lr, near_ur);
        drawLine(dc, near_ur, near_ul);
        drawLine(dc, near_ul, near_ll);
        drawLine(dc, far_ll, far_lr);
        drawLine(dc, far_lr, far_ur);
        drawLine(dc, far_ur, far_ul);
        drawLine(dc, far_ul, far_ll);
        drawLine(dc, near_ll, far_ll);
        drawLine(dc, near_lr, far_lr);
        drawLine(dc, near_ur, far_ur);
        drawLine(dc, near_ul, far_ul);

        // Draw frame of the volume between the eye and the near plane
        OGLUtil.applyColor(gl, Color.GRAY, 1d, false);
        drawLine(dc, eye, near_ll);
        drawLine(dc, eye, near_lr);
        drawLine(dc, eye, near_ur);
        drawLine(dc, eye, near_ul);
    }

    protected static void drawLine(DrawContext dc, Vec4 a, Vec4 b)
    {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_LINES);
        gl.glVertex3d(a.x, a.y, a.z);
        gl.glVertex3d(b.x, b.y, b.z);
        gl.glEnd();
    }

    protected static void drawQuad(DrawContext dc, Vec4 ll, Vec4 lr, Vec4 ur, Vec4 ul)
    {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex3d(ll.x, ll.y, ll.z);
        gl.glVertex3d(lr.x, lr.y, lr.z);
        gl.glVertex3d(ur.x, ur.y, ur.z);
        gl.glVertex3d(ul.x, ul.y, ul.z);
        gl.glEnd();
    }

    protected static void drawTriangle(DrawContext dc, Vec4 a, Vec4 b, Vec4 c)
    {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_TRIANGLES);
        gl.glVertex3d(a.x, a.y, a.z);
        gl.glVertex3d(b.x, b.y, b.z);
        gl.glVertex3d(c.x, c.y, c.z);
        gl.glEnd();
    }

    protected static Vec4 worldPointFromScreenPoint(DrawContext dc, Rectangle viewport, Matrix modelview,
        Matrix projection, Vec4 screenPoint)
    {
        // GLU expects matrices as column-major arrays.
        double[] modelviewArray = new double[16];
        double[] projectionArray = new double[16];
        modelview.toArray(modelviewArray, 0, false);
        projection.toArray(projectionArray, 0, false);
        // GLU expects the viewport as a four-component array.
        int[] viewportArray = new int[] {viewport.x, viewport.y, viewport.width, viewport.height};

        double[] result = new double[3];
        if (!dc.getGLU().gluUnProject(
            screenPoint.x, screenPoint.y, screenPoint.z,
            modelviewArray, 0,
            projectionArray, 0,
            viewportArray, 0,
            result, 0))
        {
            return null;
        }

        return Vec4.fromArray3(result, 0);
    }

    protected class OrderedViewVolume implements OrderedRenderable
    {
        protected Matrix modelview;
        protected Matrix projection;
        protected Rectangle viewport;

        public OrderedViewVolume(Matrix modelview, Matrix projection, Rectangle viewport)
        {
            this.modelview = modelview;
            this.projection = projection;
            this.viewport = new Rectangle(viewport);
        }

        public double getDistanceFromEye()
        {
            return 0d;
        }

        public void render(DrawContext dc)
        {
            draw(dc, this.modelview, this.projection, this.viewport);
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
        }
    }
}
