/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.Logging;

import java.util.List;

/**
 * @author Tom Gaskins
 * @version $Id: EllipsoidalGlobe.java 12998 2010-01-09 11:14:08Z tgaskins $
 */
public class EllipsoidalGlobe extends WWObjectImpl implements Globe
{
    protected final double equatorialRadius;
    protected final double polarRadius;
    protected final double es;
    private final Vec4 center;
    private ElevationModel elevationModel;
    private Tessellator tessellator;

    public EllipsoidalGlobe(double equatorialRadius, double polarRadius, double es, ElevationModel em)
    {
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.es = es; // assume it's consistent with the two radii
        this.center = Vec4.ZERO;
        this.elevationModel = em;
        this.tessellator = (Tessellator) WorldWind.createConfigurationComponent(AVKey.TESSELLATOR_CLASS_NAME);
    }

    public EllipsoidalGlobe(double equatorialRadius, double polarRadius, double es, ElevationModel em, Vec4 center)
    {
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.es = es; // assume it's consistent with the two radii
        this.center = center;
        this.elevationModel = em;
        this.tessellator = (Tessellator) WorldWind.createConfigurationComponent(AVKey.TESSELLATOR_CLASS_NAME);
    }

    protected class StateKey
    {
        protected Globe globe;
        protected final Tessellator tessellator;
        protected double verticalExaggeration;

        public StateKey(DrawContext dc)
        {
            this.globe = dc.getGlobe();
            this.tessellator = EllipsoidalGlobe.this.tessellator;
            this.verticalExaggeration = dc.getVerticalExaggeration();
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            StateKey stateKey = (StateKey) o;

            if (Double.compare(stateKey.verticalExaggeration, verticalExaggeration) != 0)
                return false;
            if (globe != null ? !globe.equals(stateKey.globe) : stateKey.globe != null)
                return false;
            if (tessellator != null ? !tessellator.equals(stateKey.tessellator) : stateKey.tessellator != null)
                return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            int result;
            long temp;
            result = (globe != null ? globe.hashCode() : 0);
            result = 31 * result + (tessellator != null ? tessellator.hashCode() : 0);
            temp = verticalExaggeration != +0.0d ? Double.doubleToLongBits(verticalExaggeration) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    public Object getStateKey(DrawContext dc)
    {
        return new StateKey(dc);
    }

    public Tessellator getTessellator()
    {
        return tessellator;
    }

    public void setTessellator(Tessellator tessellator)
    {
        this.tessellator = tessellator;
    }

    public ElevationModel getElevationModel()
    {
        return elevationModel;
    }

    public void setElevationModel(ElevationModel elevationModel)
    {
        this.elevationModel = elevationModel;
    }

    public double getRadius()
    {
        return this.equatorialRadius;
    }

    public double getEquatorialRadius()
    {
        return this.equatorialRadius;
    }

    public double getPolarRadius()
    {
        return this.polarRadius;
    }

    public double getMaximumRadius()
    {
        return this.equatorialRadius;
    }

    public double getRadiusAt(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.computePointFromPosition(latitude, longitude, 0d).getLength3();
    }

    public double getRadiusAt(LatLon latLon)
    {
        if (latLon == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return this.computePointFromPosition(latLon.getLatitude(), latLon.getLongitude(), 0d).getLength3();
    }

    public double getEccentricitySquared()
    {
        return this.es;
    }

    public double getDiameter()
    {
        return this.equatorialRadius * 2;
    }

    public Vec4 getCenter()
    {
        return this.center;
    }

    public double getMaxElevation()
    {
        return this.elevationModel != null ? this.elevationModel.getMaxElevation() : 0;
    }

    public double getMinElevation()
    {
        return this.elevationModel != null ? this.elevationModel.getMinElevation() : 0;
    }

    public double[] getMinAndMaxElevations(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.elevationModel != null ? this.elevationModel.getExtremeElevations(sector) : new double[] {0, 0};
    }

    public Extent getExtent()
    {
        return this;
    }

    public boolean intersects(Frustum frustum)
    {
        if (frustum == null)
        {
            String message = Logging.getMessage("nullValue.FrustumIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return frustum.intersects(this);
    }

    public Intersection[] intersect(Line line)
    {
        return this.intersect(line, this.equatorialRadius, this.polarRadius);
    }

    public Intersection[] intersect(Line line, double altitude)
    {
        return this.intersect(line, this.equatorialRadius + altitude, this.polarRadius + altitude);
    }

    protected Intersection[] intersect(Line line, double equRadius, double polRadius)
    {
        if (line == null)
        {
            String message = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Taken from Lengyel, 2Ed., Section 5.2.3, page 148.

        double m = equRadius / polRadius; // "ratio of the x semi-axis length to the y semi-axis length"
        double n = 1d;                    // "ratio of the x semi-axis length to the z semi-axis length"
        double m2 = m * m;
        double n2 = n * n;
        double r2 = equRadius * equRadius; // nominal radius squared //equRadius * polRadius;

        double vx = line.getDirection().x;
        double vy = line.getDirection().y;
        double vz = line.getDirection().z;
        double sx = line.getOrigin().x;
        double sy = line.getOrigin().y;
        double sz = line.getOrigin().z;

        double a = vx * vx + m2 * vy * vy + n2 * vz * vz;
        double b = 2d * (sx * vx + m2 * sy * vy + n2 * sz * vz);
        double c = sx * sx + m2 * sy * sy + n2 * sz * sz - r2;

        double discriminant = discriminant(a, b, c);
        if (discriminant < 0)
            return null;

        double discriminantRoot = Math.sqrt(discriminant);
        if (discriminant == 0)
        {
            Vec4 p = line.getPointAt((-b - discriminantRoot) / (2 * a));
            return new Intersection[] {new Intersection(p, true)};
        }
        else // (discriminant > 0)
        {
            Vec4 near = line.getPointAt((-b - discriminantRoot) / (2 * a));
            Vec4 far = line.getPointAt((-b + discriminantRoot) / (2 * a));
            if (c >= 0) // Line originates outside the Globe.
                return new Intersection[] {new Intersection(near, false), new Intersection(far, false)};
            else // Line originates inside the Globe.
                return new Intersection[] {new Intersection(far, false)};
        }
    }

    static private double discriminant(double a, double b, double c)
    {
        return b * b - 4 * a * c;
    }

    /**
     * Determines if and where a <code>Triangle</code> intersects the globe ellipsoid at a given elevation.
     *
     * @param t         the <code>Trinagle</code>.
     * @param elevation the elevation to test for.
     *
     * @return an array of two <code>Intersection</code> or null if no intersection was found.
     */
    public Intersection[] intersect(Triangle t, double elevation)
    {
        if (t == null)
        {
            String message = Logging.getMessage("nullValue.TriangleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        boolean bA = isPointAboveElevation(t.getA(), elevation);
        boolean bB = isPointAboveElevation(t.getB(), elevation);
        boolean bC = isPointAboveElevation(t.getC(), elevation);

        if (!(bA ^ bB) && !(bB ^ bC))
            return null; // all triangle points are either above or below the given elevation

        Intersection[] inter = new Intersection[2];
        int idx = 0;

        // Assumes that intersect(Line) returns only one intersection when the line
        // originates inside the ellipsoid at the given elevation.
        if (bA ^ bB)
            if (bA)
                inter[idx++] = intersect(new Line(t.getB(), t.getA().subtract3(t.getB())), elevation)[0];
            else
                inter[idx++] = intersect(new Line(t.getA(), t.getB().subtract3(t.getA())), elevation)[0];

        if (bB ^ bC)
            if (bB)
                inter[idx++] = intersect(new Line(t.getC(), t.getB().subtract3(t.getC())), elevation)[0];
            else
                inter[idx++] = intersect(new Line(t.getB(), t.getC().subtract3(t.getB())), elevation)[0];

        if (bC ^ bA)
            if (bC)
                inter[idx] = intersect(new Line(t.getA(), t.getC().subtract3(t.getA())), elevation)[0];
            else
                inter[idx] = intersect(new Line(t.getC(), t.getA().subtract3(t.getC())), elevation)[0];

        return inter;
    }

    public boolean intersects(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return line.distanceTo(this.center) <= this.equatorialRadius;
    }

    public boolean intersects(Plane plane)
    {
        if (plane == null)
        {
            String msg = Logging.getMessage("nullValue.PlaneIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double dq1 = plane.dot(this.center);
        return dq1 <= this.equatorialRadius;
    }

    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution,
        double[] elevations)
    {
        return this.elevationModel != null ?
            this.elevationModel.getElevations(sector, latlons, targetResolution, elevations) : 0;
    }

    public double getElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.elevationModel != null ? this.elevationModel.getElevation(latitude, longitude) : 0;
    }

    public Vec4 computePointFromPosition(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(position.getLatitude(), position.getLongitude(), position.getElevation());
    }

    public Vec4 computePointFromLocation(LatLon location)
    {
        if (location == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(location.getLatitude(), location.getLongitude(), 0);
    }

    public Vec4 computePointFromPosition(LatLon latLon, double metersElevation)
    {
        if (latLon == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(latLon.getLatitude(), latLon.getLongitude(), metersElevation);
    }

    public Vec4 computePointFromPosition(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.geodeticToCartesian(latitude, longitude, metersElevation);
    }

    public Position computePositionFromPoint(Vec4 point)
    {
        if (point == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.cartesianToGeodetic(point);
    }

    /**
     * Returns the normal to the Globe at the specified position.
     *
     * @param latitude  the latitude of the position.
     * @param longitude the longitude of the position.
     *
     * @return the Globe normal at the specified position.
     */
    public Vec4 computeSurfaceNormalAtLocation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double cosLat = latitude.cos();
        double cosLon = longitude.cos();
        double sinLat = latitude.sin();
        double sinLon = longitude.sin();

        double eqSquared = this.equatorialRadius * this.equatorialRadius;
        double polSquared = this.polarRadius * this.polarRadius;

        double x = cosLat * sinLon / eqSquared;
        double y = (1.0 - this.es) * sinLat / polSquared;
        double z = cosLat * cosLon / eqSquared;

        return new Vec4(x, y, z).normalize3();
    }

    /**
     * Returns the normal to the Globe at the specified cartiesian point.
     *
     * @param point the cartesian point.
     *
     * @return the Globe normal at the specified point.
     */
    public Vec4 computeSurfaceNormalAtPoint(Vec4 point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double eqSquared = this.equatorialRadius * this.equatorialRadius;
        double polSquared = this.polarRadius * this.polarRadius;

        double x = (point.x - this.center.x) / eqSquared;
        double y = (point.y - this.center.y) / polSquared;
        double z = (point.z - this.center.z) / eqSquared;

        return new Vec4(x, y, z).normalize3();
    }

    public Vec4 computeNorthPointingTangentAtLocation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Latitude is treated clockwise as rotation about the X-axis. We flip the latitude value so that a positive
        // rotation produces a clockwise rotation (when facing the axis).
        latitude = latitude.multiply(-1.0);

        double cosLat = latitude.cos();
        double sinLat = latitude.sin();
        double cosLon = longitude.cos();
        double sinLon = longitude.sin();

        // The north-pointing tangent is derived by rotating the vector (0, 1, 0) about the Y-axis by longitude degrees,
        // then rotating it about the X-axis by -latitude degrees. This can be represented by a combining two rotation
        // matrices Rlat, and Rlon, then transforming the vector (0, 1, 0) by the combined transform:
        //
        // NorthTangent = (Rlon * Rlat) * (0, 1, 0)
        //
        // Since the input vector only has a Y coordinate, this computation can be simplified. The simplified
        // computation is shown here as NorthTangent = (x, y, z).
        //
        double x = sinLat * sinLon;
        //noinspection UnnecessaryLocalVariable
        double y = cosLat;
        double z = sinLat * cosLon;

        return new Vec4(x, y, z).normalize3();
    }

    /**
     * Returns the cartesian transform Matrix that maps model coordinates to a local coordinate system at (latitude,
     * longitude, metersElevation). They X axis is mapped to the vector tangent to the globe and pointing East. The Y
     * axis is mapped to the vector tangent to the Globe and pointing to the North Pole. The Z axis is mapped to the
     * Globe normal at (latitude, longitude, metersElevation). The origin is mapped to the cartesian position of
     * (latitude, longitude, metersElevation).
     *
     * @param latitude        the latitude of the position.
     * @param longitude       the longitude of the position.
     * @param metersElevation the number of meters above or below mean sea level.
     *
     * @return the cartesian transform Matrix that maps model coordinates to the local coordinate system at the
     *         specified position.
     */
    public Matrix computeModelCoordinateOriginTransform(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Vec4 point = this.geodeticToCartesian(latitude, longitude, metersElevation);
        // Transform to the cartesian coordinates of (latitude, longitude, metersElevation).
        Matrix transform = Matrix.fromTranslation(point);
        // Rotate the coordinate system to match the longitude.
        // Longitude is treated as counter-clockwise rotation about the Y-axis.
        transform = transform.multiply(Matrix.fromRotationY(longitude));
        // Rotate the coordinate systme to match the latitude.
        // Latitude is treated clockwise as rotation about the X-axis. We flip the latitude value so that a positive
        // rotation produces a clockwise rotation (when facing the axis).
        transform = transform.multiply(Matrix.fromRotationX(latitude.multiply(-1.0)));
        return transform;
    }

    /**
     * Returns the cartesian transform Matrix that maps model coordinates to a local coordinate system at (latitude,
     * longitude, metersElevation). They X axis is mapped to the vector tangent to the globe and pointing East. The Y
     * axis is mapped to the vector tangent to the Globe and pointing to the North Pole. The Z axis is mapped to the
     * Globe normal at (latitude, longitude, metersElevation). The origin is mapped to the cartesian position of
     * (latitude, longitude, metersElevation).
     *
     * @param position the latitude, longitude, and number of meters above or below mean sea level.
     *
     * @return the cartesian transform Matrix that maps model coordinates to the local coordinate system at the
     *         specified position.
     */
    public Matrix computeModelCoordinateOriginTransform(Position position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.computeModelCoordinateOriginTransform(position.getLatitude(), position.getLongitude(),
            position.getElevation());
    }

    public Position getIntersectionPosition(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Intersection[] intersections = this.intersect(line);
        if (intersections == null)
            return null;

        return this.computePositionFromPoint(intersections[0].getIntersectionPoint());
    }

    /**
     * Maps a position to world Cartesian coordinates. The Y axis points to the north pole. The Z axis points to the
     * intersection of the prime meridian and the equator, in the equatorial plane. The X axis completes a right-handed
     * coordinate system, and is 90 degrees east of the Z axis and also in the equatorial plane. Sea level is at z =
     * zero.
     *
     * @param latitude        the latitude of the position.
     * @param longitude       the longitude of the position.
     * @param metersElevation the number of meters above or below mean sea level.
     *
     * @return The Cartesian point corresponding to the input position.
     */
    protected Vec4 geodeticToCartesian(Angle latitude, Angle longitude, double metersElevation)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double cosLat = Math.cos(latitude.radians);
        double sinLat = Math.sin(latitude.radians);
        double cosLon = Math.cos(longitude.radians);
        double sinLon = Math.sin(longitude.radians);

        double rpm = // getRadius (in meters) of vertical in prime meridian
            this.equatorialRadius / Math.sqrt(1.0 - this.es * sinLat * sinLat);

        double x = (rpm + metersElevation) * cosLat * sinLon;
        double y = (rpm * (1.0 - this.es) + metersElevation) * sinLat;
        double z = (rpm + metersElevation) * cosLat * cosLon;

        return new Vec4(x, y, z);
    }

    protected Position cartesianToGeodetic(Vec4 cart)
    {
        if (cart == null)
        {
            String message = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // according to
        // H. Vermeille,
        // Direct transformation from geocentric to geodetic ccordinates,
        // Journal of Geodesy (2002) 76:451-454
        double ra2 = 1 / (this.equatorialRadius * equatorialRadius);

        double X = cart.z;
        //noinspection SuspiciousNameCombination
        double Y = cart.x;
        double Z = cart.y;
        double e2 = this.es;
        double e4 = e2 * e2;

        double XXpYY = X * X + Y * Y;
        double sqrtXXpYY = Math.sqrt(XXpYY);
        double p = XXpYY * ra2;
        double q = Z * Z * (1 - e2) * ra2;
        double r = 1 / 6.0 * (p + q - e4);
        double s = e4 * p * q / (4 * r * r * r);
        double t = Math.pow(1 + s + Math.sqrt(s * (2 + s)), 1 / 3.0);
        double u = r * (1 + t + 1 / t);
        double v = Math.sqrt(u * u + e4 * q);
        double w = e2 * (u + v - q) / (2 * v);
        double k = Math.sqrt(u + v + w * w) - w;
        double D = k * sqrtXXpYY / (k + e2);
        double lon = 2 * Math.atan2(Y, X + sqrtXXpYY);
        double sqrtDDpZZ = Math.sqrt(D * D + Z * Z);
        double lat = 2 * Math.atan2(Z, D + sqrtDDpZZ);
        double elevation = (k + e2 - 1) * sqrtDDpZZ / k;

        return Position.fromRadians(lat, lon, elevation);
    }

    /**
     * Returns a cylinder that minimally surrounds the sector at a specified vertical exaggeration.
     *
     * @param verticalExaggeration the vertical exaggeration to apply to the globe's elevations when computing the
     *                             cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     *
     * @return The minimal bounding cylinder in Cartesian coordinates.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    public Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double[] minAndMaxElevations = this.getMinAndMaxElevations(sector);
        return this.computeBoundingCylinder(verticalExaggeration, sector,
            minAndMaxElevations[0], minAndMaxElevations[1]);
    }

    /**
     * Returns a cylinder that minimally surrounds the specified minimum and maximum elevations in the sector at a
     * specified vertical exaggeration.
     *
     * @param verticalExaggeration the vertical exaggeration to apply to the minimum and maximum elevations when
     *                             computing the cylinder.
     * @param sector               the sector to return the bounding cylinder for.
     * @param minElevation         the minimum elevation of the bounding cylinder.
     * @param maxElevation         the maximum elevation of the bounding cylinder.
     *
     * @return The minimal bounding cylinder in Cartesian coordinates.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    public Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector,
        double minElevation, double maxElevation)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute the exaggerated minimum and maximum heights.
        double minHeight = minElevation * verticalExaggeration;
        double maxHeight = maxElevation * verticalExaggeration;

        if (minHeight == maxHeight)
            maxHeight = minHeight + 1; // ensure the top and bottom of the cylinder won't be coincident

        // If the sector spans both poles in latitude, or spans greater than 180 degrees in longitude, we cannot use the
        // sector's Cartesian quadrilateral to compute a bounding cylinde. This is because the quadrilateral is either
        // smaller than the geometry defined by the sector (when deltaLon >= 180), or the quadrilateral degenerates to
        // two points (when deltaLat >= 180). So we compute a bounging cylinder that spans the equator and covers the
        // sector's latitude range. In some cases this cylinder may be too large, but we're typically not interested
        // in culling these cylinders since the sector will span most of the globe.
        if (sector.getDeltaLatDegrees() >= 180d || sector.getDeltaLonDegrees() >= 180d)
        {
            return this.computeBoundsFromSectorLatitudeRange(sector, minHeight, maxHeight);
        }
        // Otherwise, create a standard bounding cylinder that minimally surrounds the specified sector and elevations.
        else
        {
            return this.computeBoundsFromSectorQuadrilateral(sector, minHeight, maxHeight);
        }
    }

    public SectorGeometryList tessellate(DrawContext dc)
    {
        if (this.tessellator == null)
        {
            this.tessellator = (Tessellator) WorldWind.createConfigurationComponent(AVKey.TESSELLATOR_CLASS_NAME);
        }

        return this.tessellator.tessellate(dc);
    }

    /**
     * Determines whether a point is above a given elevation
     *
     * @param point     the <code>Vec4</code> point to test.
     * @param elevation the elevation to test for.
     *
     * @return true if the given point is above the given elevation.
     */
    public boolean isPointAboveElevation(Vec4 point, double elevation)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return (point.x() * point.x()) / ((this.equatorialRadius + elevation) * (this.equatorialRadius + elevation))
            + (point.y() * point.y()) / ((this.polarRadius + elevation) * (this.polarRadius + elevation))
            + (point.z() * point.z()) / ((this.equatorialRadius + elevation) * (this.equatorialRadius + elevation))
            - 1 > 0;
    }

    /**
     * Returns a cylinder that minimally surrounds the specified height range in the sector.
     *
     * @param sector    the sector to return the bounding cylinder for.
     * @param minHeight the minimum height to include in the bounding cylinder.
     * @param maxHeight the maximum height to include in the bounding cylinder.
     *
     * @return The minimal bounding cylinder in Cartesian coordinates.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    protected Cylinder computeBoundsFromSectorQuadrilateral(Sector sector, double minHeight, double maxHeight)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Get three non-coincident points on the sector's quadrilateral. We choose the north or south pair that is
        // closest to the equator, then choose a third point from the opposite pair. We use maxHeight as elevation
        // because we want to bound the largest potential quadrilateral for the sector.
        Vec4 p0, p1, p2;
        if (Math.abs(sector.getMinLatitude().degrees) <= Math.abs(sector.getMaxLatitude().degrees))
        {
            p0 = this.computePointFromPosition(sector.getMinLatitude(), sector.getMaxLongitude(), maxHeight); // SE
            p1 = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), maxHeight); // SW
            p2 = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMinLongitude(), maxHeight); // NW
        }
        else
        {
            p0 = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMinLongitude(), maxHeight); // NW
            p1 = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), maxHeight); // NE
            p2 = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), maxHeight); // SW
        }

        // Compute the center, axis, and radius of the circle that circumscribes the three points.
        // This circle is guaranteed to circumscribe all four points of the sector's Cartesian quadrilateral.
        Vec4[] centerOut = new Vec4[1];
        Vec4[] axisOut = new Vec4[1];
        double[] radiusOut = new double[1];
        if (!this.computeCircleThroughPoints(p0, p1, p2, centerOut, axisOut, radiusOut))
        {
            // If the computation failed, then two of the points are coincident. Fall back to creating a bounding
            // cylinder based on the vertices of the sector. This bounding cylinder won't be as tight a fit, but
            // it will be correct.
            return this.computeBoundsFromSectorVertices(sector, minHeight, maxHeight);
        }
        Vec4 centerPoint = centerOut[0];
        Vec4 axis = axisOut[0];
        double radius = radiusOut[0];

        // Compute the sector's lowest projection along the cylinder axis. We test opposite corners of the sector
        // using minHeight. One of these will be the lowest point in the sector.
        Vec4 extremePoint = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), minHeight);
        double minProj = extremePoint.subtract3(centerPoint).dot3(axis);
        extremePoint = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), minHeight);
        minProj = Math.min(minProj, extremePoint.subtract3(centerPoint).dot3(axis));
        // Compute the sector's highest projection along the cylinder axis. We only need to use the point at the
        // sector's centroid with maxHeight. This point is guaranteed to be the highest point in the sector.
        LatLon centroid = sector.getCentroid();
        extremePoint = this.computePointFromPosition(centroid.getLatitude(), centroid.getLongitude(), maxHeight);
        double maxProj = extremePoint.subtract3(centerPoint).dot3(axis);

        Vec4 bottomCenterPoint = axis.multiply3(minProj).add3(centerPoint);
        Vec4 topCenterPoint = axis.multiply3(maxProj).add3(centerPoint);

        return new Cylinder(bottomCenterPoint, topCenterPoint, radius);
    }

    /**
     * Compute the Cylinder that surrounds the equator, and has height defined by the sector's minumum and maximum
     * latitudes (including maxHeight).
     *
     * @param sector    the sector to return the bounding cylinder for.
     * @param minHeight the minimum height to include in the bounding cylinder.
     * @param maxHeight the maximum height to include in the bounding cylinder.
     *
     * @return the minimal bounding cylinder in Cartesianl coordinates.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected Cylinder computeBoundsFromSectorLatitudeRange(Sector sector, double minHeight, double maxHeight)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 centerPoint = Vec4.ZERO;
        Vec4 axis = Vec4.UNIT_Y;
        double radius = this.getEquatorialRadius() + maxHeight;

        // Compute the sector's lowest projection along the cylinder axis. This will be a point of minimum latitude
        // with maxHeight.
        Vec4 extremePoint = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), maxHeight);
        double minProj = extremePoint.subtract3(centerPoint).dot3(axis);
        // Compute the sector's lowest highest along the cylinder axis. This will be a point of maximum latitude
        // with maxHeight.
        extremePoint = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), maxHeight);
        double maxProj = extremePoint.subtract3(centerPoint).dot3(axis);

        Vec4 bottomCenterPoint = axis.multiply3(minProj).add3(centerPoint);
        Vec4 topCenterPoint = axis.multiply3(maxProj).add3(centerPoint);

        return new Cylinder(bottomCenterPoint, topCenterPoint, radius);
    }

    /**
     * Returns a cylinder that surrounds the specified height range in the zero-area sector. The returned cylinder won't
     * be as tight a fit as <code>computeBoundsFromSectorQuadrilateral</code>.
     *
     * @param sector    the sector to return the bounding cylinder for.
     * @param minHeight the minimum height to include in the bounding cylinder.
     * @param maxHeight the maximum height to include in the bounding cylinder.
     *
     * @return The minimal bounding cylinder in Cartesian coordinates.
     *
     * @throws IllegalArgumentException if <code>sector</code> is null
     */
    protected Cylinder computeBoundsFromSectorVertices(Sector sector, double minHeight, double maxHeight)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute the top center point as the surface point with maxHeight at the sector's centroid.
        LatLon centroid = sector.getCentroid();
        Vec4 topCenterPoint = this.computePointFromPosition(centroid.getLatitude(), centroid.getLongitude(), maxHeight);
        // Compute the axis as the surface normal at the sector's centroid.
        Vec4 axis = this.computeSurfaceNormalAtPoint(topCenterPoint);

        // Compute the four corner points of the sector with minHeight.
        Vec4 southwest = this.computePointFromPosition(sector.getMinLatitude(), sector.getMinLongitude(), minHeight);
        Vec4 southeast = this.computePointFromPosition(sector.getMinLatitude(), sector.getMaxLongitude(), minHeight);
        Vec4 northeast = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMaxLongitude(), minHeight);
        Vec4 northwest = this.computePointFromPosition(sector.getMaxLatitude(), sector.getMinLongitude(), minHeight);

        // Compute the bottom center point as the lowest projection along the axis.
        double minProj = southwest.subtract3(topCenterPoint).dot3(axis);
        minProj = Math.min(minProj, southeast.subtract3(topCenterPoint).dot3(axis));
        minProj = Math.min(minProj, northeast.subtract3(topCenterPoint).dot3(axis));
        minProj = Math.min(minProj, northwest.subtract3(topCenterPoint).dot3(axis));
        Vec4 bottomCenterPoint = axis.multiply3(minProj).add3(topCenterPoint);

        // Compute the radius as the maximum distance from the top center point to any of the corner points.
        double radius = topCenterPoint.distanceTo3(southwest);
        radius = Math.max(radius, topCenterPoint.distanceTo3(southeast));
        radius = Math.max(radius, topCenterPoint.distanceTo3(northeast));
        radius = Math.max(radius, topCenterPoint.distanceTo3(northwest));

        return new Cylinder(bottomCenterPoint, topCenterPoint, radius);
    }

    /**
     * Computes the center, axis, and radius of the circle that circumscribes the specified points. If the points are
     * oriented in a clockwise winding order, the circle's axis will point toward the viewer. Otherwise the axis will
     * point away from the viewer. Values are returned in the first element of centerOut, axisOut, and radiusOut. The
     * caller must provide a preallocted arrays of length one or greater for each of these values.
     *
     * @param p0        the first point.
     * @param p1        the second point.
     * @param p2        the third point.
     * @param centerOut preallocated array to hold the circle's center.
     * @param axisOut   preallocated array to hold the circle's axis.
     * @param radiusOut preallocated array to hold the circle's radius.
     *
     * @return true if the computation was successful; false otherwise.
     *
     * @throws IllegalArgumentException if <code>p0</code>, <code>p1</code>, or <code>p2</code> is null
     */
    private boolean computeCircleThroughPoints(Vec4 p0, Vec4 p1, Vec4 p2, Vec4[] centerOut, Vec4[] axisOut,
        double[] radiusOut)
    {
        if (p0 == null || p1 == null || p2 == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 v0 = p1.subtract3(p0);
        Vec4 v1 = p2.subtract3(p1);
        Vec4 v2 = p2.subtract3(p0);

        double d0 = v0.dot3(v2);
        double d1 = -v0.dot3(v1);
        double d2 = v1.dot3(v2);

        double t0 = d1 + d2;
        double t1 = d0 + d2;
        double t2 = d0 + d1;

        double e0 = d0 * t0;
        double e1 = d1 * t1;
        double e2 = d2 * t2;

        double max_e = Math.max(Math.max(e0, e1), e2);
        double min_e = Math.min(Math.min(e0, e1), e2);

        double E = e0 + e1 + e2;

        double tolerance = 1e-6;
        if (Math.abs(E) <= tolerance * (max_e - min_e))
            return false;

        double radiusSquared = 0.5d * t0 * t1 * t2 / E;
        // the three points are collinear -- no circle with finite radius is possible
        if (radiusSquared < 0d)
            return false;

        double radius = Math.sqrt(radiusSquared);

        Vec4 center = p0.multiply3(e0 / E);
        center = center.add3(p1.multiply3(e1 / E));
        center = center.add3(p2.multiply3(e2 / E));

        Vec4 axis = v2.cross3(v0);
        axis = axis.normalize3();

        if (centerOut != null)
            centerOut[0] = center;
        if (axisOut != null)
            axisOut[0] = axis;
        if (radiusOut != null)
            radiusOut[0] = radius;
        return true;
    }

    /**
     * Construct an elevation model given a key for a configuration source and the source's default value.
     *
     * @param key          the key identifying the configuration property in {@link Configuration}.
     * @param defaultValue the default value of the property to use if it's not found in {@link Configuration}.
     *
     * @return a new elevation model configured according to the configuration source.
     */
    public static ElevationModel makeElevationModel(String key, String defaultValue)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            throw new IllegalArgumentException(msg);
        }

        Object configSource = Configuration.getStringValue(key, defaultValue);
        return (ElevationModel) BasicFactory.create(AVKey.ELEVATION_MODEL_FACTORY, configSource);
    }
}