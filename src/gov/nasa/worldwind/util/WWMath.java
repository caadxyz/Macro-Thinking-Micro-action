/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;

/**
 * A collection of useful math methods, all static.
 *
 * @author tag
 * @version $Id: WWMath.java 12802 2009-11-17 23:51:59Z dcollins $
 */
public class WWMath
{
    public static final double SECOND_TO_MILLIS = 1000.0;
    public static final double MINUTE_TO_MILLIS = 60000.0;
    public static final double HOUR_TO_MILLIS = 3600000.0;

    public static final double METERS_TO_KILOMETERS = 1e-3;
    public static final double METERS_TO_MILES = 0.000621371192;
    public static final double METERS_TO_NAUTICAL_MILES = 0.000539956803;
    public static final double METERS_TO_YARDS = 1.0936133;
    public static final double METERS_TO_FEET = 3.280839895;

    public static final double SQUARE_METERS_TO_SQUARE_KILOMETERS = 1e-6;
    public static final double SQUARE_METERS_TO_SQUARE_MILES = 3.86102159e-7;
    public static final double SQUARE_METERS_TO_SQUARE_YARDS = 1.19599005;
    public static final double SQUARE_METERS_TO_SQUARE_FEET = 10.7639104;
    public static final double SQUARE_METERS_TO_HECTARES = 1e-4;
    public static final double SQUARE_METERS_TO_ACRES = 0.000247105381;

    /**
     * Convenience method to compute the log base 2 of a value.
     *
     * @param value the value to take the log of.
     *
     * @return the log base 2 of the specified value.
     */
    public static double logBase2(double value)
    {
        return Math.log(value) / Math.log(2d);
    }

    /**
     * Convenience method for testing whether a value is a power of two.
     *
     * @param value the value to test for power of 2
     *
     * @return true if power of 2, else false
     */
    public static boolean isPowerOfTwo(int value)
    {
        return (value == powerOfTwoCeiling(value));
    }

    /**
     * Returns the value that is the nearest power of 2 greater than or equal to the given value.
     *
     * @param reference the reference value. The power of 2 returned is greater than or equal to this value.
     *
     * @return the value that is the nearest power of 2 greater than or equal to the reference value
     */
    public static int powerOfTwoCeiling(int reference)
    {
        int power = (int) Math.ceil(Math.log(reference) / Math.log(2d));
        return (int) Math.pow(2d, power);
    }

    /**
     * Returns the value that is the nearest power of 2 less than or equal to the given value.
     *
     * @param reference the reference value. The power of 2 returned is less than or equal to this value.
     *
     * @return the value that is the nearest power of 2 less than or equal to the reference value
     */
    public static int powerOfTwoFloor(int reference)
    {
        int power = (int) Math.floor(Math.log(reference) / Math.log(2d));
        return (int) Math.pow(2d, power);
    }

    /**
     * Clamps a value to a given range.
     *
     * @param v   the value to clamp.
     * @param min the floor.
     * @param max the ceiling
     *
     * @return the nearest value such that min <= v <= max.
     */
    public static double clamp(double v, double min, double max)
    {
        return v < min ? min : v > max ? max : v;
    }

    /**
     * Returns the interpolation factor for <code>v</code> given the specified range <code>[x, y]</code>. The
     * interpolation factor is a number between 0 and 1 (inclusive), representing the value's relative position between
     * <code>x</code> and <code>y</code>. For example, 0 corresponds to <code>x</code>, 1 corresponds to <code>y</code>,
     * and anything in between corresponds to a linear combination of <code>x</code> and <code>y</code>.
     *
     * @param v the value to compute the interpolation factor for.
     * @param x the first value.
     * @param y the second value.
     *
     * @return the interpolation factor for <code>v</code> given the specified range <code>[x, y]</code>
     */
    public static double computeInterpolationFactor(double v, double x, double y)
    {
        return clamp((v - x) / (y - x), 0d, 1d);
    }

    /**
     * Returns the linear interpolation of <code>x</code> and <code>y</code> according to the function: <code>(1 - a) *
     * x + a * y</code>. The interpolation factor <code>a</code> defines the weight given to each value, and is clamped
     * to the range [0, 1]. If <code>a</code> is 0 or less, this returns x. If <code>a</code> is 1 or more, this returns
     * <code>y</code>. Otherwise, this returns the linear interpolation of <code>x</code> and <code>y</code>. For
     * example, when <code>a</code> is <code>0.5</code> this returns <code>(x + y)/2</code>.
     *
     * @param a the interpolation factor.
     * @param x the first value.
     * @param y the second value.
     *
     * @return the linear interpolation of <code>x</code> and <code>y</code>.
     */
    public static double mix(double a, double x, double y)
    {
        double t = clamp(a, 0d, 1d);
        return x + t * (y - x);
    }

    /**
     * Returns the smooth hermite interpolation of <code>x</code> and <code>y</code> according to the function: <code>(1
     * - t) * x + t * y</code>, where <code>t = a * a * (3 - 2 * a)</code>. The interpolation factor <code>a</code>
     * defines the weight given to each value, and is clamped to the range [0, 1]. If <code>a</code> is 0 or less, this
     * returns <code>x</code>. If <code>a</code> is 1 or more, this returns <code>y</code>. Otherwise, this returns the
     * smooth hermite interpolation of <code>x</code> and <code>y</code>. Like the linear function {@link #mix(double,
     * double, double)}, when <code>a</code> is <code>0.5</code> this returns <code>(x + y)/2</code>. But unlike the
     * linear function, the hermite function's slope gradually increases when <code>a</code> is near 0, then gradually
     * decreases when <code>a</code> is near 1. This is a useful property where a more gradual transition from
     * <code>x</code> to <code>y</code> is desired.
     *
     * @param a the interpolation factor.
     * @param x the first value.
     * @param y the second value.
     *
     * @return the smooth hermite interpolation of <code>x</code> and <code>y</code>.
     */
    public static double mixSmooth(double a, double x, double y)
    {
        double t = clamp(a, 0d, 1d);
        t = t * t * (3d - 2d * t);
        return x + t * (y - x);
    }

    /**
     * converts meters to feet.
     *
     * @param meters the value in meters.
     *
     * @return the value converted to feet.
     */
    public static double convertMetersToFeet(double meters)
    {
        return (meters * METERS_TO_FEET);
    }

    /**
     * converts meters to miles.
     *
     * @param meters the value in meters.
     *
     * @return the value converted to miles.
     */
    public static double convertMetersToMiles(double meters)
    {
        return (meters * METERS_TO_MILES);
    }

    /**
     * Converts time in seconds to time in milliseconds.
     *
     * @param seconds time in seconds.
     *
     * @return time in milliseconds.
     */
    public static double convertSecondsToMillis(double seconds)
    {
        return (seconds * SECOND_TO_MILLIS);
    }

    /**
     * Converts time in milliseconds to time in seconds.
     *
     * @param millis time in milliseconds.
     *
     * @return time in seconds.
     */
    public static double convertMillisToSeconds(double millis)
    {
        return millis / SECOND_TO_MILLIS;
    }

    /**
     * Converts time in minutes to time in milliseconds.
     *
     * @param minutes time in minutes.
     *
     * @return time in milliseconds.
     */
    public static double convertMinutesToMillis(double minutes)
    {
        return (minutes * MINUTE_TO_MILLIS);
    }

    /**
     * Converts time in milliseconds to time in minutes.
     *
     * @param millis time in milliseconds.
     *
     * @return time in minutes.
     */
    public static double convertMillisToMinutes(double millis)
    {
        return millis / MINUTE_TO_MILLIS;
    }

    /**
     * Converts time in hours to time in milliseconds.
     *
     * @param hours time in hours.
     *
     * @return time in milliseconds.
     */
    public static double convertHoursToMillis(double hours)
    {
        return (hours * HOUR_TO_MILLIS);
    }

    /**
     * Converts time in milliseconds to time in hours.
     *
     * @param mills time in milliseconds.
     *
     * @return time in hours.
     */
    public static double convertMillisToHours(double mills)
    {
        return mills / HOUR_TO_MILLIS;
    }

    /**
     * Returns the distance in model coordinates from the {@link gov.nasa.worldwind.View} eye point to the specified
     * {@link gov.nasa.worldwind.geom.Extent}. If the View eye point is inside the extent, this returns 0.
     *
     * @param dc     the {@link gov.nasa.worldwind.render.DrawContext} which the View eye point is obtained from.
     * @param extent the extent to compute the distance from.
     *
     * @return the distance from the View eye point to the extent, in model coordinates.
     *
     * @throws IllegalArgumentException if either the DrawContext or the extent is null.
     */
    public static double computeDistanceFromEye(DrawContext dc, Extent extent)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (extent == null)
        {
            String message = Logging.getMessage("nullValue.ExtentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double distance = dc.getView().getEyePoint().distanceTo3(extent.getCenter()) - extent.getRadius();
        return (distance < 0d) ? 0d : distance;
    }

    /**
     * Returns the size in window coordinates of the specified {@link gov.nasa.worldwind.geom.Extent} from the current
     * {@link gov.nasa.worldwind.View}. The returned size is an estimate of the Extent's diameter in window
     * coordinates.
     *
     * @param dc     the current draw context, from which the View is obtained from.
     * @param extent the extent to compute the window size for.
     *
     * @return size of the specified Extent from the specified View, in window coordinates (screen pixels).
     *
     * @throws IllegalArgumentException if either the DrawContext or the extent is null.
     */
    public static double computeSizeInWindowCoordinates(DrawContext dc, Extent extent)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (extent == null)
        {
            String message = Logging.getMessage("nullValue.ExtentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Estimate the size in window coordinates W from as follows. Given
        // R, the extent radius in meters
        // D, the distance from eye point to extent center, in meters
        //
        // compute S, the size of screen pixel at D, in meters/pixels. S is an estimate based on the viewport
        // size, the view projection, and D.
        //
        // Finally, estimate W, the extent's diameter in window coordinates (screen pixels).
        //
        // W : 2R * 1/S = (2R meters / S meters) * 1 pixels = 2R/S pixels

        double distance = dc.getView().getEyePoint().distanceTo3(extent.getCenter());
        double pixelSize = dc.getView().computePixelSizeAtDistance(distance);
        return 2d * extent.getRadius() / pixelSize;
    }

    /**
     * Returns the normal vector corresponding to the triangle defined by three vertices (a, b, c).
     *
     * @param a the triangle's first vertex.
     * @param b the triangle's second vertex.
     * @param c the triangle's third vertex.
     *
     * @return the triangle's normal vector.
     *
     * @throws IllegalArgumentException if any of the specified vertices are null.
     */
    public static Vec4 computeTriangleNormal(Vec4 a, Vec4 b, Vec4 c)
    {
        if (a == null || b == null || c == null)
        {
            String message = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double x = ((b.y - a.y) * (c.z - a.z)) - ((b.z - a.z) * (c.y - a.y));
        double y = ((b.z - a.z) * (c.x - a.x)) - ((b.x - a.x) * (c.z - a.z));
        double z = ((b.x - a.x) * (c.y - a.y)) - ((b.y - a.y) * (c.x - a.x));

        double length = (x * x) + (y * y) + (z * z);
        if (length == 0d)
            return new Vec4(x, y, z);

        length = Math.sqrt(length);
        return new Vec4(x / length, y / length, z / length);
    }

    /**
     * Returns the area enclosed by the specified locations, in angular degrees. If the specified locations do not
     * define a closed loop, then the loop is automatically closed by appending the first location to the last
     * location.
     *
     * @param locations the locations which define the geographic polygon.
     *
     * @return the area enclosed by the specified coordinates.
     *
     * @throws IllegalArgumentException if locations is null.
     */
    public static double computePolygonArea(Iterable<? extends LatLon> locations)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.Iterator<? extends LatLon> iter = locations.iterator();
        if (!iter.hasNext())
        {
            return 0;
        }

        double area = 0;
        LatLon firstLocation = iter.next();
        LatLon location = firstLocation;

        while (iter.hasNext())
        {
            LatLon nextLocation = iter.next();

            area += location.getLongitude().degrees * nextLocation.getLatitude().degrees;
            area -= nextLocation.getLongitude().degrees * location.getLatitude().degrees;

            location = nextLocation;
        }

        // Include the area connecting the last point to the first point, if they're not already equal.
        if (!location.equals(firstLocation))
        {
            area += location.getLongitude().degrees * firstLocation.getLatitude().degrees;
            area -= firstLocation.getLongitude().degrees * location.getLatitude().degrees;
        }

        area /= 2.0;
        return area;
    }

    /**
     * Returns the area enclosed by the specified (x, y) points (the z and w coordinates are ignored). If the specified
     * points do not define a closed loop, then the loop is automatically closed by simulating appending the first point
     * to the last point.
     *
     * @param points the (x, y) points which define the 2D polygon.
     *
     * @return the area enclosed by the specified coordinates.
     *
     * @throws IllegalArgumentException if points is null.
     */
    public static double computePolygonArea2(Iterable<? extends Vec4> points)
    {
        if (points == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.Iterator<? extends Vec4> iter = points.iterator();
        if (!iter.hasNext())
        {
            return 0;
        }

        double area = 0;
        Vec4 firstPoint = iter.next();
        Vec4 point = firstPoint;

        while (iter.hasNext())
        {
            Vec4 nextLocation = iter.next();

            area += point.x * nextLocation.y;
            area -= nextLocation.x * point.y;

            point = nextLocation;
        }

        // Include the area connecting the last point to the first point, if they're not already equal.
        if (!point.equals(firstPoint))
        {
            area += point.x * firstPoint.y;
            area -= firstPoint.x * point.y;
        }

        area /= 2.0;
        return area;
    }

    /**
     * Returns the winding order of the polygon described by the specified locations, with respect an axis perpendicular
     * to the (lat, lon) coordinates, and pointing in the direction of "positive elevation".
     *
     * @param locations the locations which define the geographic polygon.
     *
     * @return {@link AVKey#CLOCKWISE} if the polygon has clockwise winding order about the positive z axis, and {@link
     *         AVKey#COUNTER_CLOCKWISE} otherwise.
     */
    public static String computePolygonWindingOrder(Iterable<? extends LatLon> locations)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double area = computePolygonArea(locations);

        return (area < 0) ? AVKey.CLOCKWISE : AVKey.COUNTER_CLOCKWISE;
    }

    /**
     * Returns the winding order of the 2D polygon described by the specified (x, y) points (z and w coordinates are
     * ignored), with respect to the positive z axis.
     *
     * @param points the (x, y) points which define the 2D polygon.
     *
     * @return AVKey.CLOCKWISE if the polygon has clockwise winding order about the positive z axis, and
     *         AVKey.COUNTER_CLOCKWISE otherwise.
     */
    public static String computePolygonWindingOrder2(Iterable<? extends Vec4> points)
    {
        if (points == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double area = computePolygonArea2(points);

        return (area < 0) ? AVKey.CLOCKWISE : AVKey.COUNTER_CLOCKWISE;
    }

    /**
     * Returns whether the geographic polygon described by the specified locations defines a closed loop. If the
     * iterable holds fewer than two points, this always returns false. Therefore a polygon consisting of a single point
     * and the empty polygon are not considered closed loops.
     *
     * @param locations the locations which define the geographic polygon.
     *
     * @return true if the polygon defines a closed loop, and false otherwise.
     *
     * @throws IllegalArgumentException if the locations are null.
     */
    public static boolean isPolygonClosed(Iterable<? extends LatLon> locations)
    {
        if (locations == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.Iterator<? extends LatLon> iter = locations.iterator();
        if (!iter.hasNext())
        {
            return false;
        }

        LatLon firstLocation = iter.next();
        LatLon lastLocation = null;

        while (iter.hasNext())
        {
            lastLocation = iter.next();
        }

        return (lastLocation != null) && lastLocation.equals(firstLocation);
    }

    /**
     * Returns whether the 2D polygon described by the specified (x, y) points defines a closed loop (z and w
     * coordinates are ignored). If the iterable holds fewer than two points, this always returns false. Therefore a
     * polygon consisting of a single point and the empty polygon are not considered closed loops.
     *
     * @param points the (x, y) points which define the 2D polygon.
     *
     * @return true if the polygon defines a closed loop, and false otherwise.
     *
     * @throws IllegalArgumentException if the points are null.
     */
    public static boolean isPolygonClosed2(Iterable<? extends Vec4> points)
    {
        if (points == null)
        {
            String message = Logging.getMessage("nullValue.IterableIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.Iterator<? extends Vec4> iter = points.iterator();
        if (!iter.hasNext())
        {
            return false;
        }

        Vec4 firstPoint = iter.next();
        Vec4 lastPoint = null;

        while (iter.hasNext())
        {
            lastPoint = iter.next();
        }

        return (lastPoint != null) && (lastPoint.x == firstPoint.x) && (lastPoint.y == firstPoint.y);
    }

    // TODO: this is only valid for linear path type
    /**
     * Determines whether a {@link LatLon} location is located inside a given polygon.
     *
     * @param location  the location
     * @param locations the list of positions describing the polygon. Last one should be the same as the first one.
     *
     * @return true if the location is inside the polygon.
     */
    public static boolean isLocationInside(LatLon location, Iterable<? extends LatLon> locations)
    {
        if (location == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.util.Iterator<? extends LatLon> iter = locations.iterator();
        if (!iter.hasNext())
        {
            return false;
        }

        // Test for even/odd number of intersections with a constant latitude line going through the given location.
        boolean result = false;
        LatLon p1 = iter.next();
        while (iter.hasNext())
        {
            LatLon p2 = iter.next();

// Developped for clarity
//            double lat = location.getLatitude().degrees;
//            double lon = location.getLongitude().degrees;
//            double lat1 = p1.getLatitude().degrees;
//            double lon1 = p1.getLongitude().degrees;
//            double lat2 = p2.getLatitude().degrees;
//            double lon2 = p2.getLongitude().degrees;
//            if ( ((lat2 <= lat && lat < lat1) || (lat1 <= lat && lat < lat2))
//                    && (lon < (lon1 - lon2) * (lat - lat2) / (lat1 - lat2) + lon2) )
//                result = !result;

            if (((p2.getLatitude().degrees <= location.getLatitude().degrees
                && location.getLatitude().degrees < p1.getLatitude().degrees) ||
                (p1.getLatitude().degrees <= location.getLatitude().degrees
                    && location.getLatitude().degrees < p2.getLatitude().degrees))
                && (location.getLongitude().degrees < (p1.getLongitude().degrees - p2.getLongitude().degrees)
                * (location.getLatitude().degrees - p2.getLatitude().degrees)
                / (p1.getLatitude().degrees - p2.getLatitude().degrees) + p2.getLongitude().degrees))
                result = !result;

            p1 = p2;
        }
        return result;
    }
}
