/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.GL;
import javax.media.opengl.glu.*;

/**
 * Represents a geometric cylinder. <code>Cylinder</code>s are immutable.
 *
 * @author Tom Gaskins
 * @version $Id: Cylinder.java 12989 2010-01-09 06:27:20Z tgaskins $
 */
public class Cylinder implements Extent, Renderable
{
    private final Vec4 bottomCenter; // point at center of cylinder base
    private final Vec4 topCenter; // point at center of cylinder top
    private final Vec4 axisUnitDirection; // axis as unit vector from bottomCenter to topCenter
    private final double cylinderRadius;
    private final double cylinderHeight;

    /**
     * Create a Cylinder from two points and a radius.
     *
     * @param bottomCenter   the center point of of the cylinder's base.
     * @param topCenter      the center point of the cylinders top.
     * @param cylinderRadius the cylinder's radius.
     *
     * @throws IllegalArgumentException if the radius is zero or the top or bottom point is null or they are
     *                                  coincident.
     */
    public Cylinder(Vec4 bottomCenter, Vec4 topCenter, double cylinderRadius)
    {
        if (bottomCenter == null || topCenter == null || bottomCenter.equals(topCenter))
        {
            String message = Logging.getMessage(
                bottomCenter == null || topCenter == null ? "nullValue.EndPointIsNull" : "generic.EndPointsCoincident");

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (cylinderRadius <= 0)
        {
            String message = Logging.getMessage("Geom.Cylinder.RadiusIsZeroOrNegative", cylinderRadius);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.bottomCenter = bottomCenter;
        this.topCenter = topCenter;
        this.cylinderHeight = this.bottomCenter.distanceTo3(this.topCenter);
        this.cylinderRadius = cylinderRadius;
        this.axisUnitDirection = this.topCenter.subtract3(this.bottomCenter).normalize3();
    }

    public Vec4 getAxisUnitDirection()
    {
        return axisUnitDirection;
    }

    public Vec4 getBottomCenter()
    {
        return bottomCenter;
    }

    public Vec4 getTopCenter()
    {
        return topCenter;
    }

    public double getCylinderRadius()
    {
        return cylinderRadius;
    }

    public double getCylinderHeight()
    {
        return cylinderHeight;
    }

    public String toString()
    {
        return this.cylinderRadius + ", " + this.bottomCenter.toString() + ", " + this.topCenter.toString() + ", "
            + this.axisUnitDirection.toString();
    }

    public Intersection[] intersect(Line line)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double[] tVals = new double[2];
        if (!intcyl(line.getOrigin(), line.getDirection(), this.bottomCenter, this.axisUnitDirection,
            this.cylinderRadius, tVals))
            return null;

        if (!clipcyl(line.getOrigin(), line.getDirection(), this.bottomCenter, this.topCenter,
            this.axisUnitDirection, tVals))
            return null;

        if (!Double.isInfinite(tVals[0]) && !Double.isInfinite(tVals[1]) && tVals[0] >= 0.0 && tVals[1] >= 0.0)
            return new Intersection[] {new Intersection(line.getPointAt(tVals[0]), false),
                new Intersection(line.getPointAt(tVals[1]), false)};
        if (!Double.isInfinite(tVals[0]) && tVals[0] >= 0.0)
            return new Intersection[] {new Intersection(line.getPointAt(tVals[0]), false)};
        if (!Double.isInfinite(tVals[1]) && tVals[1] >= 0.0)
            return new Intersection[] {new Intersection(line.getPointAt(tVals[1]), false)};
        return null;
    }

    public boolean intersects(Line line)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return intersect(line) != null;
    }

    // Taken from "Graphics Gems IV", Section V.2, page 356.

    private boolean intcyl(Vec4 raybase, Vec4 raycos, Vec4 base, Vec4 axis, double radius, double[] tVals)
    {
        boolean hit; // True if ray intersects cyl
        Vec4 RC; // Ray base to cylinder base
        double d; // Shortest distance between the ray and the cylinder
        double t, s; // Distances along the ray
        Vec4 n, D, O;
        double ln;

        RC = raybase.subtract3(base);
        n = raycos.cross3(axis);

        // Ray is parallel to the cylinder's axis.
        if ((ln = n.getLength3()) == 0.0)
        {
            d = RC.dot3(axis);
            D = RC.subtract3(axis.multiply3(d));
            d = D.getLength3();
            tVals[0] = Double.NEGATIVE_INFINITY;
            tVals[1] = Double.POSITIVE_INFINITY;
            // True if ray is in cylinder.
            return d <= radius;
        }

        n = n.normalize3();
        d = Math.abs(RC.dot3(n)); // Shortest distance.
        hit = (d <= radius);

        // If ray hits cylinder.
        if (hit)
        {
            O = RC.cross3(axis);
            t = -O.dot3(n) / ln;
            O = n.cross3(axis);
            O = O.normalize3();
            s = Math.abs(Math.sqrt(radius * radius - d * d) / raycos.dot3(O));
            tVals[0] = t - s; // Entering distance.
            tVals[1] = t + s; // Exiting distance.
        }

        return hit;
    }

    // Taken from "Graphics Gems IV", Section V.2, page 356.

    private boolean clipcyl(Vec4 raybase, Vec4 raycos, Vec4 bot, Vec4 top, Vec4 axis, double[] tVals)
    {
        double dc, dwb, dwt, tb, tt;
        double in, out; // Object intersection distances.

        in = tVals[0];
        out = tVals[1];

        dc = axis.dot3(raycos);
        dwb = axis.dot3(raybase) - axis.dot3(bot);
        dwt = axis.dot3(raybase) - axis.dot3(top);

        // Ray is parallel to the cylinder end-caps.
        if (dc == 0.0)
        {
            if (dwb <= 0.0)
                return false;
            if (dwt >= 0.0)
                return false;
        }
        else
        {
            // Intersect the ray with the bottom end-cap.
            tb = -dwb / dc;
            // Intersect the ray with the top end-cap.
            tt = -dwt / dc;

            // Bottom is near cap, top is far cap.
            if (dc >= 0.0)
            {
                if (tb > out)
                    return false;
                if (tt < in)
                    return false;
                if (tb > in && tb < out)
                    in = tb;
                if (tt > in && tt < out)
                    out = tt;
            }
            // Bottom is far cap, top is near cap.
            else
            {
                if (tb < in)
                    return false;
                if (tt > out)
                    return false;
                if (tb > in && tb < out)
                    out = tb;
                if (tt > in && tt < out)
                    in = tt;
            }
        }

        tVals[0] = in;
        tVals[1] = out;
        return in < out;
    }

    private double intersects(Plane plane, double effectiveRadius)
    {
        // Test the distance from the first cylinder end-point.
        double dq1 = plane.dot(this.bottomCenter);
        boolean bq1 = dq1 <= -effectiveRadius;

        // Test the distance from the top of the cylinder.
        double dq2 = plane.dot(this.topCenter);
        boolean bq2 = dq2 <= -effectiveRadius;

        if (bq1 && bq2) // both beyond effective radius; cylinder is on negative side of plane
            return -1;

        if (bq1 == bq2) // both within effective radius; can't draw any conclusions
            return 0;

        return 1; // Cylinder almost certainly intersects
    }

    private double intersectsAt(Plane plane, double effectiveRadius, Vec4[] endpoints)
    {
        // Test the distance from the first end-point.
        double dq1 = plane.dot(endpoints[0]);
        boolean bq1 = dq1 <= -effectiveRadius;

        // Test the distance from the possibly reduced second cylinder end-point.
        double dq2 = plane.dot(endpoints[1]);
        boolean bq2 = dq2 <= -effectiveRadius;

        if (bq1 && bq2) // endpoints more distant from plane than effective radius; cylinder is on neg. side of plane
            return -1;

        if (bq1 == bq2) // endpoints less distant from plane than effective radius; can't draw any conclusions
            return 0;

        // Compute and return the endpoints of the cylinder on the positive side of the plane.
        double t = (effectiveRadius + dq1) / plane.getNormal().dot3(endpoints[0].subtract3(endpoints[1]));

        Vec4 newEndPoint = endpoints[0].add3(endpoints[1].subtract3(endpoints[0]).multiply3(t));
        if (bq1) // Truncate the lower end of the cylinder
            endpoints[0] = newEndPoint;
        else // Truncate the upper end of the cylinder
            endpoints[1] = newEndPoint;

        return t;
    }

    private double getEffectiveRadius(Plane plane)
    {
        // Determine the effective radius of the cylinder axis relative to the plane.
        double dot = plane.getNormal().dot3(this.axisUnitDirection);
        double scale = 1d - dot * dot;
        if (scale <= 0)
            return 0;
        else
            return this.cylinderRadius * Math.sqrt(scale);
    }

    public boolean intersects(Plane plane)
    {
        if (plane == null)
        {
            String message = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double effectiveRadius = this.getEffectiveRadius(plane);
        return this.intersects(plane, effectiveRadius) >= 0;
    }

    public boolean intersects(Frustum frustum)
    {
        if (frustum == null)
        {
            String message = Logging.getMessage("nullValue.FrustumIsNull");

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double intersectionPoint;
        Vec4[] endPoints = new Vec4[] {this.bottomCenter, this.topCenter};

        double effectiveRadius = this.getEffectiveRadius(frustum.getNear());
        intersectionPoint = this.intersectsAt(frustum.getNear(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        // Near and far have the same effective radius.
        intersectionPoint = this.intersectsAt(frustum.getFar(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getLeft());
        intersectionPoint = this.intersectsAt(frustum.getLeft(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getRight());
        intersectionPoint = this.intersectsAt(frustum.getRight(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getTop());
        intersectionPoint = this.intersectsAt(frustum.getTop(), effectiveRadius, endPoints);
        if (intersectionPoint < 0)
            return false;

        effectiveRadius = this.getEffectiveRadius(frustum.getBottom());
        intersectionPoint = this.intersectsAt(frustum.getBottom(), effectiveRadius, endPoints);
        return intersectionPoint >= 0;
    }
//
//    public boolean intersects(Frustum frustum)
//    {
//        if (frustum == null)
//        {
//            String message = Logging.getMessage("nullValue.FrustumIsNull");
//
//            Logging.logger().severe(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        double intersectionPoint;
//
//        double effectiveRadius = this.getEffectiveRadius(frustum.getNear());
//        intersectionPoint = this.intersectsAt(frustum.getNear(), effectiveRadius, 1d);
//        if (intersectionPoint < 0)
//            return false;
//
//        // Near and far have the same effective radius.
//        intersectionPoint = this.intersectsAt(frustum.getFar(), effectiveRadius, 1d);
//        if (intersectionPoint < 0)
//            return false;
//
//        effectiveRadius = this.getEffectiveRadius(frustum.getLeft());
//        intersectionPoint = this.intersectsAt(frustum.getLeft(), effectiveRadius, 1d);
//        if (intersectionPoint < 0)
//            return false;
//
//        effectiveRadius = this.getEffectiveRadius(frustum.getRight());
//        intersectionPoint = this.intersectsAt(frustum.getRight(), effectiveRadius, 1d);
//        if (intersectionPoint < 0)
//            return false;
//
//        effectiveRadius = this.getEffectiveRadius(frustum.getTop());
//        intersectionPoint = this.intersectsAt(frustum.getTop(), effectiveRadius, 1d);
//        if (intersectionPoint < 0)
//            return false;
//
//        effectiveRadius = this.getEffectiveRadius(frustum.getBottom());
//        intersectionPoint = this.intersectsAt(frustum.getBottom(), effectiveRadius, 1d);
//        return intersectionPoint >= 0;
//    }

    public Vec4 getCenter()
    {
        Vec4 b = this.bottomCenter;
        Vec4 t = this.topCenter;
        return new Vec4(
            (b.x + t.x) / 2.0,
            (b.y + t.y) / 2.0,
            (b.z + t.z) / 2.0);
    }

    public double getDiameter()
    {
        return 2 * this.getRadius();
    }

    public double getRadius()
    {
        // return the radius of the enclosing sphere
        double halfHeight = this.bottomCenter.distanceTo3(this.topCenter) / 2.0;
        return Math.sqrt(halfHeight * halfHeight + this.cylinderRadius * this.cylinderRadius);
    }

    /**
     * Obtain the height of this <code>Cylinder</code>.
     *
     * @return the distance between the bottom and top of this <code>Cylinder</code>
     */
    public final double getHeight()
    {
        return this.cylinderHeight;
    }

    /**
     * Display the cylinder.
     *
     * @param dc the current draw context.
     *
     * @throws IllegalArgumentException if the draw context is null.
     */
    public void render(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute a matrix that will transform world coordinates to cylinder coordinates. The negative z-axis
        // will point from the cylinder's bottomCenter to its topCenter. The y-axis will be a vector that is
        // perpendicular to the cylinder's axisUnitDirection. Because the cylinder is symmetric, it does not matter
        // in what direction the y-axis points, as long as it is perpendicular to the z-axis.
        double tolerance = 1e-6;
        Vec4 upVector = (this.axisUnitDirection.cross3(Vec4.UNIT_Y).getLength3() <= tolerance) ?
            Vec4.UNIT_NEGATIVE_Z : Vec4.UNIT_Y;
        Matrix transformMatrix = Matrix.fromModelLookAt(this.bottomCenter, this.topCenter, upVector);
        double[] matrixArray = new double[16];
        transformMatrix.toArray(matrixArray, 0, false);

        javax.media.opengl.GL gl = dc.getGL();

        OGLStackHandler ogsh = new OGLStackHandler();
        ogsh.pushAttrib(gl, GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT | GL.GL_TRANSFORM_BIT | GL.GL_DEPTH_BUFFER_BIT);
        try
        {
            // The cylinder is drawn with as a wireframe plus a center axis. It's drawn in two passes in order to
            // visualize the portions of the cylinder above and below an intersecting surface.
            
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);
            gl.glEnable(GL.GL_DEPTH_TEST);

            // Draw the axis
            gl.glDepthFunc(GL.GL_LEQUAL); // draw the part that would normally be visible
            gl.glColor4f(1f, 1f, 1f, 0.4f);
            gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(this.bottomCenter.x, this.bottomCenter.y, this.bottomCenter.z);
            gl.glVertex3d(this.topCenter.x, this.topCenter.y, this.topCenter.z);
            gl.glEnd();

            gl.glDepthFunc(GL.GL_GREATER); // draw the part that is behind an intersecting surface
            gl.glColor4f(1f, 0f, 1f, 0.4f);
            gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(this.bottomCenter.x, this.bottomCenter.y, this.bottomCenter.z);
            gl.glVertex3d(this.topCenter.x, this.topCenter.y, this.topCenter.z);
            gl.glEnd();

            // Draw the exterior wireframe
            ogsh.pushModelview(gl);
            gl.glMultMatrixd(matrixArray, 0);

            GLUquadric quadric = dc.getGLU().gluNewQuadric();
            dc.getGLU().gluQuadricDrawStyle(quadric, GLU.GLU_LINE);

            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glColor4f(1f, 1f, 1f, 0.5f);
            dc.getGLU().gluCylinder(quadric, this.cylinderRadius, this.cylinderRadius, this.cylinderHeight, 30, 30);

            gl.glDepthFunc(GL.GL_GREATER);
            gl.glColor4f(1f, 0f, 1f, 0.4f);
            dc.getGLU().gluCylinder(quadric, this.cylinderRadius, this.cylinderRadius, this.cylinderHeight, 30, 30);

            dc.getGLU().gluDeleteQuadric(quadric);
        }
        finally
        {
            ogsh.pop(gl);
        }
    }
}
