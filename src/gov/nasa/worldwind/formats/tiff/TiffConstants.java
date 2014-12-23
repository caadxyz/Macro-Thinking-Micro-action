/* Copyright (C) 2001, 2008 United States Government as represented by
   the Administrator of the National Aeronautics and Space Administration.
   All Rights Reserved.
 */

package gov.nasa.worldwind.formats.tiff;

/**
 * @author brownrigg
 * @version $Id: TiffConstants.java 6539 2008-09-10 16:39:49Z rick $
 */
public interface TiffConstants
{
    // compression...
    public static final int COMPRESSION_NONE = 1;
    public static final int COMPRESSION_LZW = 5;
    public static final int COMPRESSION_PACKBITS = 32773;

    // photometric interpretation...
    public static final int PHOTOINTERP_WHITEISZERO = 0;
    public static final int PHOTOINTERP_BLACKISZERO = 1;
    public static final int PHOTOINTERP_RGB = 2;
    public static final int PHOTOINTERP_PALETTE = 3;

    // planar configuration...
    public static final int PLANARCONFIG_CHUNKY = 1;
    public static final int PLANARCONFIG_PLANAR = 2;

    // resolution unit...
    public static final int RESUNIT_NONE = 1;
    public static final int RESUNIT_INCH = 2;
    public static final int RESUNIT_CENTIMETER = 3;

    // sample format...
    public static final int SAMPLEFORMAT_UNSIGNED = 1;
    public static final int SAMPLEFORMAT_SIGNED = 2;
    public static final int SAMPLEFORMAT_IEEEFLOAT = 3;
    public static final int SAMPLEFORMAT_UNDEFINED = 4;
}
