/* Copyright (C) 2001, 2008 United States Government as represented by
   the Administrator of the National Aeronautics and Space Administration.
   All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.geom.*;

import java.util.*;

/**
 * A class to bundle the GeoTiff structures found in a GeoTiff file, and to assist in the coding/decoding of those
 * structures.
 *
 * @author brownrigg
 * @version $Id: GeoCodec.java 6602 2008-09-12 13:40:37Z jparsons $
 */
public class GeoCodec
{
    /*
     * Symbolic constants for georeferencing keys found in Geotiff "GeoKey Directory" tags.
     * Taken straight from the GeoTiff 1.0 specification.
     */

    public static final int GTModelTypeGeoKey = 1024;            /*  Section 6.3.1.1 Codes  */
    public static final int GTRasterTypeGeoKey = 1025;           /*  Section 6.3.1.2 Codes  */
    public static final int GTCitationGeoKey = 1026;             /* documentation           */

    /*
     * 6.2.2 Geographic CS Parameter Keys
     */
    public static final int GeographicTypeGeoKey = 2048;          /*  Section 6.3.2.1 Codes   */
    public static final int GeogCitationGeoKey = 2049;            /* documentation            */
    public static final int GeogGeodeticDatumGeoKey = 2050;       /*  Section 6.3.2.2 Codes   */
    public static final int GeogPrimeMeridianGeoKey = 2051;       /*  Section 6.3.2.4 codes   */
    public static final int GeogLinearUnitsGeoKey = 2052;         /*  Section 6.3.1.3 Codes   */
    public static final int GeogLinearUnitSizeGeoKey = 2053;      /* meters                   */
    public static final int GeogAngularUnitsGeoKey = 2054;        /*  Section 6.3.1.4 Codes   */
    public static final int GeogAngularUnitSizeGeoKey = 2055;     /* radians                  */
    public static final int GeogEllipsoidGeoKey = 2056;           /*  Section 6.3.2.3 Codes   */
    public static final int GeogSemiMajorAxisGeoKey = 2057;       /* GeogLinearUnits          */
    public static final int GeogSemiMinorAxisGeoKey = 2058;       /* GeogLinearUnits          */
    public static final int GeogInvFlatteningGeoKey = 2059;       /* ratio                    */
    public static final int GeogAzimuthUnitsGeoKey = 2060;        /*  Section 6.3.1.4 Codes   */
    public static final int GeogPrimeMeridianLongGeoKey = 2061;   /* GeogAngularUnit          */

    /*
     * 6.2.3 Projected CS Parameter Keys
     */
    public static final int ProjectedCSTypeGeoKey = 3072;         /*  Section 6.3.3.1 codes  */
    public static final int PCSCitationGeoKey = 3073;             /* documentation           */
    public static final int ProjectionGeoKey = 3074;              /*  Section 6.3.3.2 codes  */
    public static final int ProjCoordTransGeoKey = 3075;          /*  Section 6.3.3.3 codes  */
    public static final int ProjLinearUnitsGeoKey = 3076;         /*  Section 6.3.1.3 codes  */
    public static final int ProjLinearUnitSizeGeoKey = 3077;      /* meters                  */
    public static final int ProjStdParallel1GeoKey = 3078;        /* GeogAngularUnit */
    public static final int ProjStdParallel2GeoKey = 3079;        /* GeogAngularUnit */
    public static final int ProjNatOriginLongGeoKey = 3080;       /* GeogAngularUnit */
    public static final int ProjNatOriginLatGeoKey = 3081;        /* GeogAngularUnit */
    public static final int ProjFalseEastingGeoKey = 3082;        /* ProjLinearUnits */
    public static final int ProjFalseNorthingGeoKey = 3083;       /* ProjLinearUnits */
    public static final int ProjFalseOriginLongGeoKey = 3084;     /* GeogAngularUnit */
    public static final int ProjFalseOriginLatGeoKey = 3085;      /* GeogAngularUnit */
    public static final int ProjFalseOriginEastingGeoKey = 3086;  /* ProjLinearUnits */
    public static final int ProjFalseOriginNorthingGeoKey = 3087; /* ProjLinearUnits */
    public static final int ProjCenterLongGeoKey = 3088;          /* GeogAngularUnit */
    public static final int ProjCenterLatGeoKey = 3089;           /* GeogAngularUnit */
    public static final int ProjCenterEastingGeoKey = 3090;       /* ProjLinearUnits */
    public static final int ProjCenterNorthingGeoKey = 3091;      /* ProjLinearUnits */
    public static final int ProjScaleAtNatOriginGeoKey = 3092;    /* ratio           */
    public static final int ProjScaleAtCenterGeoKey = 3093;       /* ratio           */
    public static final int ProjAzimuthAngleGeoKey = 3094;        /* GeogAzimuthUnit */
    public static final int ProjStraightVertPoleLongGeoKey = 3095;/* GeogAngularUnit */

    /*
     * 6.2.4 Vertical CS Keys
     */
    public static final int VerticalCSTypeGeoKey = 4096;          /*  Section 6.3.4.1 codes  */
    public static final int VerticalCitationGeoKey = 4097;        /* documentation           */
    public static final int VerticalDatumGeoKey = 4098;           /*  Section 6.3.4.2 codes  */
    public static final int VerticalUnitsGeoKey = 4099;           /*  Section 6.3.1.3 codes  */

    /*
     * Aliases
     */
    public static final int ProjStdParallelGeoKey = ProjStdParallel1GeoKey;
    public static final int ProjOriginLongGeoKey = ProjNatOriginLongGeoKey;
    public static final int ProjOriginLatGeoKey = ProjNatOriginLatGeoKey;
    public static final int ProjScaleAtOriginGeoKey = ProjScaleAtNatOriginGeoKey;

    public GeoCodec()
    {

    }

    /**
     * Adds ModelTiePoints from an array. Recall that by definition, a tie point is a 6-tuple of <i,j,k><x,y,z> values.
     *
     * @param values A 6-tuple representing a Geotiff ModelTiePoint.
     * @throws IllegalArgumentException if values not a multiple of 6.
     */
    public void addModelTiePoints(double[] values) throws IllegalArgumentException
    {
        if (values == null || values.length == 0 || (values.length % 6) != 0)
        {
            String message = Logging.getMessage("GeoCodec.BadTiePoints");
            Logging.logger().severe(message);
            throw new UnsupportedOperationException(message);
        }

        for (int i = 0; i < values.length; i += 6)
        {
            addModelTiePoint(values[i], values[i + 1], values[i + 2], values[i + 3], values[i + 4], values[i + 5]);
        }
    }

    public void addModelTiePoint(double i, double j, double k, double x, double y, double z)
    {
        ModelTiePoint t = new ModelTiePoint(i, j, k, x, y, z);
        this.tiePoints.add(t);
    }

    public void addModelTiePoint(ModelTiePoint t)
    {
        if (t != null)
            this.tiePoints.add(t);
    }

    public ModelTiePoint[] getTiePoints()
    {
        ModelTiePoint[] tiePoints = new ModelTiePoint[this.tiePoints.size()];
        return this.tiePoints.toArray(tiePoints);
    }

    public void clearModelTiePoints()
    {
        this.tiePoints.clear();
    }

    /**
     * Sets the 3 ModelPixelScale values.
     *
     * @param values The ModelPixelScale values.
     * @throws IllegalArgumentException if values is not of length 3.
     */
    public void setModelPixelScale(double[] values)
    {
        if (values == null || values.length != 3)
        {
            String message = Logging.getMessage("GeoCodec.BadPixelValues");
            Logging.logger().severe(message);
            throw new UnsupportedOperationException(message);
        }

        this.xScale = values[0];
        this.yScale = values[1];
        this.zScale = values[2];
    }

    public void setModelPixelScale(double xScale, double yScale, double zScale)
    {
        this.xScale = xScale;
        this.yScale = yScale;
        this.zScale = zScale;
    }

    public double[] getModelPixelScales()
    {
        double[] scales = new double[3];
        scales[0] = this.xScale;
        scales[1] = this.yScale;
        scales[2] = this.zScale;
        return scales;
    }

    public double getModelPixelScaleX()
    {
        return this.xScale;
    }

    public double getModelPixelScaleY()
    {
        return this.yScale;
    }

    public double getModelPixelScaleZ()
    {
        return this.zScale;
    }

    /**
     * Sets the ModelTransformation matrix. This is logically a 4x4 matrix of doubles by definition.
     *
     * @param matrix A logical 4x4 matrix, in row-major order.
     * @throws IllegalArgumentException if matrix is not of length 16.
     */
    public void setModelTransformation(double[] matrix) throws IllegalArgumentException
    {
        if (matrix == null || matrix.length != 16)
        {
            String message = Logging.getMessage("GeoCodec.BadMatrix");
            Logging.logger().severe(message);
            throw new UnsupportedOperationException(message);
        }

        this.modelTransform = Matrix.fromArray(matrix, 0, true);
    }

    public void setModelTransformation(Matrix matrix)
    {
        this.modelTransform = matrix;
    }

    public Matrix getMOdelTransformation()
    {
        return this.modelTransform;
    }

    /**
     * Returns the bounding box of an image that is width X height pixels, as determined by this GeoCodec. Returns
     * UnsupportedOperationException if the transformation can not be determined (see getXYZAtPixel()). The bounding Box
     * is returned as an array of double of length 4: [0] is x coordinate of upper-left corner [1] is y coordinate of
     * upper-left corner [2] is x coordinate of lower-right corner [3] is y coordinate of lower-right corner Note that
     * coordinate units are those of the underlying modeling transformation, and are not guaranteed to be in lon/lat.
     *
     * @param width  Width of a hypothetical image.
     * @param height Height of a hypothetical image.
     * @return Returns xUL, yUL, xLR, yLR of bounding box.
     * @throws UnsupportedOperationException if georeferencing can not be computed.
     */
    public double[] getBoundingBox(int width, int height) throws UnsupportedOperationException
    {
        double[] bbox = new double[4];
        double[] pnt = getXYAtPixel(0, 0);
        bbox[0] = pnt[0];
        bbox[1] = pnt[1];
        pnt = getXYAtPixel( height, width);
        bbox[2] = pnt[0];
        bbox[3] = pnt[1];
        return bbox;
    }

    /**
     * Returns the geocoordinates for a given pixel, as determined by the modeling coordinate tranformation embodied in
     * the GeoCodec.
     * <p/>
     * TODO: Also throws UnsupportedOperationException if this is anything other than a "simple" georeferenced mapping,
     * meaning that there's a single tie-point known about the point 0,0, we know the inter-pixel spacing, and there's
     * no rotation of the image required.  Geo referencing may also be specified via a general 4x4 matrix, or by a list
     * if tie-points, implying a rubbersheeting transformation. These two cases remain to be implemented.
     * <p/>
     *
     * @param row pixel-row index
     * @param col pixel-column index
     * @return double[2] containing x,y coordinate of pixel in modelling coordinate units.
     * @throws IllegalArgumentException      if row or column outside image bounds.
     * @throws UnsupportedOperationException if georeferencing can not be determined.
     */
    public double[] getXYAtPixel(int row, int col) throws UnsupportedOperationException
    {
        if (this.tiePoints.size() != 1)
        {
            String message = Logging.getMessage("GeotiffReader.NotSimpleGeotiff");
            Logging.logger().severe(message);
            throw new UnsupportedOperationException(message);
        }

        double[] xy = new double[2];
        ModelTiePoint t = this.tiePoints.get(0);
        xy[0] = t.x + col * this.xScale;
        xy[1] = t.y - row * this.yScale;
        return xy;
    }

    /**
     * Gets the values of the given GeoKey as an array of ints.
     * <p/>
     * While this method handles the general case of multiple ints associated with a key, typically there will be only a
     * single value.
     *
     * @param key GeoKey value
     * @return Array of int values associated with the key, or null if the key was not found.
     * @throws IllegalArgumentException Thrown if the key does not embody integer values.
     */
    public int[] getGeoKeyAsInts(int key) throws IllegalArgumentException
    {
        int[] vals = null;
        GeoKeyEntry entry;
        if (this.geoKeys != null && (entry = this.geoKeys.get(key)) != null)
        {
            if (entry.array != this.shortParams)
            {
                String message = Logging.getMessage("GeoCodec.NotIntegerKey", key);
                Logging.logger().severe(message);
                throw new UnsupportedOperationException(message);
            }

            vals = new int[entry.count];
            for (int i = 0; i < vals.length; i++)
            {
                vals[i] = 0xffff & (int) this.shortParams[entry.offset + i];
            }
        }
        return vals;
    }

    /**
     * Gets the values of the given GeoKey as an array of doubles.
     * <p/>
     * While this method handles the general case of multiple doubles associated with a key, typically there will be
     * only a single value.
     *
     * @param key GeoKey value.
     * @return Array of double values associated with the key, or null if the key was not found.
     * @throws IllegalArgumentException Thrown if the key does not embody double values.
     */
    public double[] getGeoKeyAsDoubles(int key) throws IllegalArgumentException
    {
        double[] vals = null;
        GeoKeyEntry entry;
        if (this.geoKeys != null && (entry = this.geoKeys.get(key)) != null)
        {
            if (entry.array != this.doubleParams)
            {
                String message = Logging.getMessage("GeoCodec.NotDoubleKey", key);
                Logging.logger().severe(message);
                throw new UnsupportedOperationException(message);
            }

            vals = new double[entry.count];
            System.arraycopy(this.doubleParams, entry.offset, vals, 0, entry.count);
        }
        return vals;
    }

    /**
     * Gets the values of the given GeoKey as a String.
     *
     * @param key GeoKey value.
     * @return String associated with the key, or null of key was not found.
     * @throws IllegalArgumentException Thrown if the key does not embody ASCII characters.
     */
    public String getGeoKeyAsString(int key) throws IllegalArgumentException
    {
        String str = null;
        GeoKeyEntry entry;
        if (this.geoKeys != null && (entry = this.geoKeys.get(key)) != null)
        {
            if (entry.array != this.asciiParams)
            {
                String message = Logging.getMessage("GeoCodec.NotAsciiKey", key);
                Logging.logger().severe(message);
                throw new UnsupportedOperationException(message);
            }

            str = new String(this.asciiParams, entry.offset, entry.count);
        }

        return str;
    }

    /*
     * Gets an array of the raw GeoKey tags. Returns null if no keys were decoded.
     *
     */
    public int[] getGeoKeys()
    {
        if (this.geoKeys == null || this.geoKeys.size() == 0)
            return null;

        Set<Integer> keys = this.geoKeys.keySet();
        int[] keyVals = new int[keys.size()];
        int i = 0;
        for (Integer key : keys)
        {
            keyVals[i++] = key;
        }

        return keyVals;
    }

    /*
     * Returns true if the given key is a GeoKey in this file; false otherwise.
     */
    public boolean hasGeoKey(int key)
    {
        return (this.geoKeys != null && this.geoKeys.get(key) != null);
    }

    //
    // Package visibility. Not generally intended for use by end users.
    //
    void setGeokeys(short[] keys)
    {
        // Decode the geokey entries into our internal management structure. Recall that the keys are organized as
        // entries of 4 shorts, where the first 4-tuple contains versioning and the number of geokeys to follow.
        // The remaining entries look very much like regular Tiff tags.

        if (keys != null && keys.length > 4)
        {
            this.shortParams = new short[keys.length];
            System.arraycopy(keys, 0, this.shortParams, 0, keys.length);

            int numKeys = keys[3];
            this.geoKeys = new HashMap<Integer, GeoKeyEntry>();
            for (int i = 4; i < numKeys * 4; i += 4)
            {
                int tag = 0x0000ffff & keys[i];
                int tagLoc = 0x0000ffff & keys[i + 1];
                if (tagLoc == 0)
                {
                    // value is in the 4th field of this entry...
                    this.geoKeys.put(tag, new GeoKeyEntry(tag, 1, i + 3, this.shortParams));
                }
                else
                {
                    // in this case, one or more values are given relative to one of the params arrays...
                    Object sourceArray = null;
                    if (tagLoc == TiffTags.GEO_KEY_DIRECTORY)
                        sourceArray = this.shortParams;
                    else if (tagLoc == TiffTags.GEO_DOUBLE_PARAMS)
                        sourceArray = this.doubleParams;
                    else if (tagLoc == TiffTags.GEO_ASCII_PARAMS)
                        sourceArray = this.asciiParams;

                    if (sourceArray != null)
                        this.geoKeys.put(tag, new GeoKeyEntry(tag, 0x0000ffff & keys[i + 2],
                            0x0000ffff & keys[i + 3], sourceArray));
                }
            }
        }
    }

    //
    // Package visibility. Not generally intended for use by end users.
    //
    void setDoubleParams(double[] params)
    {
        this.doubleParams = new double[params.length];
        System.arraycopy(params, 0, this.doubleParams, 0, params.length);
    }

    //
    // Package visibility. Not generally intended for use by end users.
    //
    void setAsciiParams(byte[] params)
    {
        this.asciiParams = new byte[params.length];
        System.arraycopy(params, 0, this.asciiParams, 0, params.length);
    }

    private HashMap<Integer, GeoKeyEntry> geoKeys = null;

    // Collection of ModelTiePoints.
    private Vector<ModelTiePoint> tiePoints = new Vector<ModelTiePoint>(1);

    // ModelPixelScale values...
    private double xScale;
    private double yScale;
    private double zScale;

    private Matrix modelTransform;  // the ModelTransformation matrix

    private short[] shortParams;    // raw short parameters array
    private double[] doubleParams;  // raw double parameters array
    private byte[] asciiParams;     // raw ascii parameters array

    /*
    * A class to bundle up ModelTiePoints. From the Geotiff spec, a ModelTiePoint is a 6-tuple that is an
    * association of the pixel <i,j,k> to the model coordinate <x,y,z>.
    *
    */
    public class ModelTiePoint
    {
        public double i, j, k, x, y, z;

        public ModelTiePoint(double i, double j, double k, double x, double y, double z)
        {
            this.i = i;
            this.j = j;
            this.k = k;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getRow()
        {
            return this.j;
        }

        public double getColumn()
        {
            return this.i;
        }

        public double getX()
        {
            return this.x;
        }

        public double getY()
        {
            return this.y;
        }
    }

    /*
     * A little class that we use to manage GeoKeys.
     */
    private class GeoKeyEntry
    {
        int tag;
        int count;
        int offset;
        Object array;  // a reference to one of the short/double/asciiParams arrays

        GeoKeyEntry(int tag, int count, int offset, Object array)
        {
            this.tag = tag;
            this.count = count;
            this.offset = offset;
            this.array = array;
        }
    }
}
