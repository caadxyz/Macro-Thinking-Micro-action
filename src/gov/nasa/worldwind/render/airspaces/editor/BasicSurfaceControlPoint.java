/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.airspaces.editor;

import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.geom.Vec4;

/**
 * @author dcollins
 * @version $Id: BasicAirspaceControlPoint.java 8748 2009-02-04 05:25:56Z dcollins $
 */
public class BasicSurfaceControlPoint implements SurfaceControlPoint
{
    public static class BasicControlPointKey
    {
        private int locationIndex;
        

        public BasicControlPointKey(int locationIndex)
        {
            this.locationIndex = locationIndex;
        }

        public int getLocationIndex()
        {
            return this.locationIndex;
        }

        

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || this.getClass() != o.getClass())
                return false;

            BasicControlPointKey that = (BasicControlPointKey) o;
            return (this.locationIndex == that.locationIndex);
        }

        public int hashCode()
        {
            int result = this.locationIndex;
            result = 31 * result;
            return result;
        }
    }

    private SurfaceEditor editor;
    private SurfaceShape surfaceShape;
    private int locationIndex;
    private Vec4 point;

    public BasicSurfaceControlPoint(SurfaceEditor editor, SurfaceShape surfaceShape, int locationIndex, Vec4 point)
    {
        this.editor = editor;
        this.surfaceShape = surfaceShape;
        this.locationIndex = locationIndex;
        this.point = point;
    }

    public BasicSurfaceControlPoint(SurfaceEditor editor, SurfaceShape surfaceShape, Vec4 point)
    {
        this(editor, surfaceShape, -1, point);
    }

    public SurfaceEditor getEditor()
    {
        return this.editor;
    }

    public SurfaceShape getSurfaceShape()
    {
        return this.surfaceShape;
    }

    public int getLocationIndex()
    {
        return this.locationIndex;
    }


    public Vec4 getPoint()
    {
        return this.point;
    }

    public Object getKey()
    {
        return keyFor(this.locationIndex);
    }

    public static Object keyFor(int locationIndex)
    {
        return new BasicControlPointKey(locationIndex);    
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        BasicSurfaceControlPoint that = (BasicSurfaceControlPoint) o;

        // Editor and airspace are compared by references, because we're only concerned about the exact reference
        // a control point refers to, rather than an equivalent object.
        if (this.editor != that.editor)
            return false;
        if (this.surfaceShape != that.surfaceShape)
            return false;
        if (this.locationIndex != that.locationIndex)
            return false;
        //noinspection RedundantIfStatement
        if (this.point != null ? !this.point.equals(that.point) : that.point != null)
            return false;

        return true;
    }

    public int hashCode()
    {
        int result = this.editor != null ? this.editor.hashCode() : 0;
        result = 31 * result + (this.surfaceShape != null ? this.surfaceShape.hashCode() : 0);
        result = 31 * result + this.locationIndex;
        result = 31 * result + (this.point != null ? this.point.hashCode() : 0);
        return result;
    }
}
