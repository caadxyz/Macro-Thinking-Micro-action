/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.terrain.*;

import java.util.*;

/**
 * @author Tom Gaskins
 * @version $Id: Globe.java 12856 2009-12-04 03:45:25Z tgaskins $
 */
public interface Globe extends WWObject, Extent
{
    Extent getExtent();

    double getEquatorialRadius();

    double getPolarRadius();

    double getMaximumRadius();

    double getRadiusAt(Angle latitude, Angle longitude);

    double getElevation(Angle latitude, Angle longitude);

    double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] elevations);

    double getMaxElevation();

    double getMinElevation();

    Position getIntersectionPosition(Line line);

    double getEccentricitySquared();

    Vec4 computePointFromPosition(Angle latitude, Angle longitude, double metersElevation);

    Vec4 computePointFromPosition(Position position);

    Vec4 computePointFromLocation(LatLon location);

    Position computePositionFromPoint(Vec4 point);

    Vec4 computeSurfaceNormalAtLocation(Angle latitude, Angle longitude);

    Vec4 computeSurfaceNormalAtPoint(Vec4 point);

    Vec4 computeNorthPointingTangentAtLocation(Angle latitude, Angle longitude);

    Matrix computeModelCoordinateOriginTransform(Angle latitude, Angle longitude, double metersElevation);

    Matrix computeModelCoordinateOriginTransform(Position position);

    double getRadiusAt(LatLon latLon);

    double[] getMinAndMaxElevations(Sector sector);

    Intersection[] intersect(Line line, double altitude);

    Intersection[] intersect(Triangle t, double altitude);

    Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector);

    Cylinder computeBoundingCylinder(double verticalExaggeration, Sector sector,
                                     double minElevation, double maxElevation);

    Tessellator getTessellator();

    void setTessellator(Tessellator tessellator);

    SectorGeometryList tessellate(DrawContext dc);

    Object getStateKey(DrawContext dc);

    ElevationModel getElevationModel();

    void setElevationModel(ElevationModel elevationModel);

    boolean isPointAboveElevation(Vec4 point, double elevation);

    Vec4 computePointFromPosition(LatLon latLon, double metersElevation);
}
