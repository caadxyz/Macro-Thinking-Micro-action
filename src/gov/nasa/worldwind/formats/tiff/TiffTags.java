/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * Symbolic constants for some of the more common TAGS encountered in a (Geo)Tiff file.
 *
 * @author brownrigg
 * @version $Id: TiffTags.java 11575 2009-06-11 14:23:57Z jparsons $
 */
public interface TiffTags
{
    // Baseline Tiff 6.0 tags...
    public static final int IMAGE_WIDTH = 256;
    public static final int IMAGE_LENGTH = 257;
    public static final int BITS_PER_SAMPLE = 258;
    public static final int COMPRESSION = 259;
    public static final int PHOTO_INTERPRETATION = 262;
    public static final int STRIP_OFFSETS = 273;
    public static final int SAMPLES_PER_PIXEL = 277;
    public static final int ROWS_PER_STRIP = 278;
    public static final int STRIP_BYTE_COUNTS = 279;
    public static final int X_RESOLUTION = 282;
    public static final int Y_RESOLUTION = 283;
    public static final int PLANAR_CONFIGURATION = 284;
    public static final int RESOLUTION_UNIT = 296;
    public static final int TIFF_PREDICTOR = 317;
    public static final int COLORMAP = 320;
    public static final int TILE_WIDTH = 322;
    public static final int TILE_LENGTH = 323;
    public static final int TILE_OFFSETS = 324;
    public static final int TILE_COUNTS = 325;

    // Tiff extensions...
    public static final int SAMPLE_FORMAT = 339;

    // Geotiff extensions...
    public static final int MODEL_PIXELSCALE = 33550;
    public static final int MODEL_TIEPOINT = 33922;
    public static final int MODEL_TRANSFORMATION = 34264;
    public static final int GEO_KEY_DIRECTORY = 34735;
    public static final int GEO_DOUBLE_PARAMS = 34736;
    public static final int GEO_ASCII_PARAMS = 34737;
}
