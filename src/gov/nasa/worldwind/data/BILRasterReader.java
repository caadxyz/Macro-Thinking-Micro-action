/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.formats.worldfile.WorldFile;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.io.File;

/**
 * @author dcollins
 * @version $Id: BILRasterReader.java 12497 2009-08-20 23:30:00Z garakl $
 */
public class BILRasterReader extends AbstractDataRasterReader
{
    private static final String[] bilMimeTypes = new String[] {"image/bil"};
    private static final String[] bilSuffixes = new String[] {"bil"};

    private boolean mapLargeFiles = false;
    private long largeFileThreshold = 16777216L; // 16 megabytes

    public BILRasterReader()
    {
        super(bilMimeTypes, bilSuffixes);
    }

    public boolean isMapLargeFiles()
    {
        return this.mapLargeFiles;
    }

    public void setMapLargeFiles(boolean mapLargeFiles)
    {
        this.mapLargeFiles = mapLargeFiles;
    }

    public long getLargeFileThreshold()
    {
        return this.largeFileThreshold;
    }

    public void setLargeFileThreshold(long largeFileThreshold)
    {
        if (largeFileThreshold < 0L)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "largeFileThreshold < 0");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.largeFileThreshold = largeFileThreshold;
    }

    protected boolean doCanRead(DataSource source)
    {
        if (!(source.getSource() instanceof java.io.File) && !(source.getSource() instanceof java.net.URL))
            return false;

        // If the data source doesn't already have all the necessary metadata, then we determine whether or not
        // the missing metadata can be read.
        if (validateMetadata(source, source) != null)
            if (!this.canReadWorldFiles(source))
                return false;

        return true;
    }

    protected DataRaster[] doRead(DataSource source) throws java.io.IOException
    {
        java.nio.ByteBuffer byteBuffer = this.readElevations(source);

        // If the data source doesn't already have all the necessary metadata, then we attempt to read the metadata.        
        AVList values = source;
        if (this.validateMetadata(source, values) != null)
        {
            values = new AVListImpl();
            values.setValue(AVKey.FILE_SIZE, byteBuffer.capacity() );
            this.readWorldFiles(source, values);
        }



        int width = (Integer) values.getValue(AVKey.WIDTH);
        int height = (Integer) values.getValue(AVKey.HEIGHT);
        Sector sector = (Sector) values.getValue(AVKey.SECTOR);
        if( null == sector && null != source && source.hasKey(AVKey.SECTOR))
        {
            // last resort if source has a SECTOR object
            sector = (Sector)source.getValue(AVKey.SECTOR);
            values.setValue(AVKey.SECTOR, sector );
        }

        // Translate the property PIXEL_TYPE to the property DATA_TYPE.
        if (values.getValue(AVKey.DATA_TYPE) == null)
            values.setValue(AVKey.DATA_TYPE, values.getValue(AVKey.PIXEL_TYPE));

        ByteBufferRaster raster = new ByteBufferRaster(width, height, sector, byteBuffer, values);

        // This code expects the string "gov.nasa.worldwind.avkey.MissingDataValue", which now corresponds to the
        // key MISSING_DATA_REPLACEMENT.
        Double missingDataValue = (Double) values.getValue(AVKey.MISSING_DATA_REPLACEMENT);
        if (missingDataValue != null)
            raster.setTransparentValue(missingDataValue);

        return new DataRaster[] {raster};
    }

    protected void doReadMetadata(DataSource source, AVList values) throws java.io.IOException
    {
        if (validateMetadata(source, values) != null)
            this.readWorldFiles(source, values);
    }

    protected String validateMetadata(DataSource source, AVList values)
    {
        StringBuilder sb = new StringBuilder();

        String message = super.validateMetadata(source, values);
        if (message != null)
            sb.append(message);

        Object o = values.getValue(AVKey.BYTE_ORDER);
        if (o == null || !(o instanceof String))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoByteOrderSpecified", source));

        o = values.getValue(AVKey.PIXEL_FORMAT);
        if (o == null)
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoPixelFormatSpecified", source));
        else if (!AVKey.ELEVATION.equals(o))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.InvalidPixelFormat", source));

        o = values.getValue(AVKey.PIXEL_TYPE);
        if (o == null)
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoPixelTypeSpecified", source));
        
        if (sb.length() == 0)
            return null;

        return sb.toString();
    }

    private boolean canReadWorldFiles(DataSource source)
    {
        Object src = source.getSource();
        if (!(src instanceof java.io.File))
            return false;

        try
        {
            java.io.File[] worldFiles = WorldFile.getWorldFiles((java.io.File) src);
            if (worldFiles == null || worldFiles.length == 0)
                return false;
        }
        catch (java.io.IOException e)
        {
            // Not interested in logging the exception, we only want to report the failure to read.
            return false;
        }

        return true;
    }

    private java.nio.ByteBuffer readElevations(DataSource source) throws java.io.IOException
    {
        if (!(source.getSource() instanceof java.io.File) && !(source.getSource() instanceof java.net.URL))
        {
            String message = Logging.getMessage("DataRaster.CannotRead", source.getSource());
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        if (source.getSource() instanceof java.io.File)
        {
            java.io.File file = (java.io.File) source.getSource();

            if (!this.isMapLargeFiles() || (this.getLargeFileThreshold() > file.length()))
            {
                return WWIO.readFileToBuffer(file);
            }
            else
            {
                return WWIO.mapFile(file);
            }
        }
        else if (source.getSource() instanceof java.net.URL)
        {
            java.net.URL url = (java.net.URL) source.getSource();
            return WWIO.readURLContentToBuffer(url);
        }
        else
        {
            return null;
        }
    }

    private void readWorldFiles(DataSource source, AVList values) throws java.io.IOException
    {
        Object src = source.getSource();

        if (!(src instanceof java.io.File))
        {
            String message = Logging.getMessage("DataRaster.CannotRead", src);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        java.io.File[] worldFiles = WorldFile.getWorldFiles((java.io.File) src);
        WorldFile.decodeWorldFiles(worldFiles, values);

        // Translate the property WORLD_FILE_IMAGE_SIZE to separate properties WIDTH and HEIGHT.
        Object o = values.getValue(WorldFile.WORLD_FILE_IMAGE_SIZE);
        if (o != null && o instanceof int[])
        {
            int[] size = (int[]) o;
            values.setValue(AVKey.WIDTH, size[0]);
            values.setValue(AVKey.HEIGHT, size[1]);
        }
    }


    public static void main(String[] args)
    {
        try
        {
            BILRasterReader bilReader = new BILRasterReader();
            File bilFile = new File( "/Users/lado/Desktop/Current Tasks/NED_10m/dem7442.bil" );
            DataSource bilSource = new BasicDataSource( bilFile );

            // USGS NED 10m tiles come with .HDR files that do not contain NBITS, NBANDS, and PIXELTYPE properties
            // to avoid reading .HDR and .PRJ files, and because the tile parameters are well known,
            // we populate all known properties
//            bilSource.setValue( AVKey.WIDTH, 10812 );
//            bilSource.setValue( AVKey.HEIGHT, 10812 );
//            bilSource.setValue( AVKey.SECTOR, Sector.fromDegrees( 41d, 42d, -74d, -73d ) );
//            bilSource.setValue( AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN );
//            bilSource.setValue( AVKey.PIXEL_FORMAT, AVKey.ELEVATION );
//            bilSource.setValue( AVKey.PIXEL_TYPE, AVKey.FLOAT32 );
//            bilSource.setValue( AVKey.MISSING_DATA_REPLACEMENT, Double.valueOf(-9999d) );

            DataRaster[] rasters = bilReader.read( bilSource );
            ByteBufferRaster raster = (ByteBufferRaster)rasters[0];
            Sector sec = raster.getSector();
            int width = raster.getWidth();
            int height = raster.getHeight();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
