/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.formats.tiff.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;

import java.io.IOException;

/**
 * @author dcollins
 * @version $Id: GeotiffRasterReader.java 12432 2009-08-10 16:44:42Z tgaskins $
 */
public class GeotiffRasterReader extends AbstractDataRasterReader
{
    private static final String[] geotiffMimeTypes = {"image/tiff", "image/geotiff"};
    private static final String[] geotiffSuffixes = {"tif", "tiff", "gtif"};

    public GeotiffRasterReader()
    {
        super(geotiffMimeTypes, geotiffSuffixes);
    }

    protected boolean doCanRead(DataSource source)
    {
        String path = pathFor(source);
        if (path == null)
            return false;

        GeotiffReader reader = null;
        try
        {
            reader = new GeotiffReader(path);
            return reader.isGeotiff();
        }
        catch (Exception e)
        {
            // Intentionally ignoring exceptions.
            return false;
        }
        finally
        {
            if (reader != null)
                reader.close();
        }
    }

    protected DataRaster[] doRead(DataSource source) throws java.io.IOException
    {
        String path = pathFor(source);
        if (path == null)
        {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        GeotiffReader reader = null;
        try
        {
            reader = new GeotiffReader(path);
            java.awt.image.BufferedImage image = reader.read();
            image = ImageUtil.toCompatibleImage(image);

            // If the data source doesn't already have all the necessary metadata, then we attempt to read the metadata.
            Object o = source.getValue(AVKey.SECTOR);
            if (o == null || !(o instanceof Sector))
            {
                AVList values = new AVListImpl();
                this.readGeotiffSector(reader, values);
                o = values.getValue(AVKey.SECTOR);
            }

            return new DataRaster[] {new BufferedImageRaster((Sector) o, image)};
        }
        finally
        {
            if (reader != null)
                reader.close();
        }
    }

    protected void doReadMetadata(DataSource source, AVList values) throws java.io.IOException
    {
        String path = pathFor(source);
        if (path == null)
        {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }
        
        GeotiffReader reader = null;
        try
        {
            Object width = values.getValue(AVKey.WIDTH);
            Object height = values.getValue(AVKey.HEIGHT);
            if (width == null || height == null || !(width instanceof Integer) || !(height instanceof Integer))
            {
                reader = new GeotiffReader(path);
                this.readGeotiffDimension(reader, values);
            }

            Object sector = values.getValue(AVKey.SECTOR);
            if (sector == null || !(sector instanceof Sector))
            {
                this.readGeotiffSector(reader, values);
            }
        }
        finally
        {
            if (reader != null)
                reader.close();
        }
    }

    private void readGeotiffDimension(GeotiffReader reader, AVList values) throws java.io.IOException
    {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        values.setValue(AVKey.WIDTH, width);
        values.setValue(AVKey.HEIGHT, height);
    }

    private void readGeotiffSector(GeotiffReader reader, AVList values) throws java.io.IOException
    {
        if (reader.getGeoCodec().hasGeoKey(GeoCodec.GeographicTypeGeoKey) && !reader.getGeoCodec().hasGeoKey(GeoCodec.ProjectedCSTypeGeoKey))
        {
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            double[] bbox = reader.getGeoCodec().getBoundingBox(width, height);
            Sector sector = new Sector(
                Angle.fromDegreesLatitude(bbox[3]), Angle.fromDegreesLatitude(bbox[1]), 
                Angle.fromDegreesLongitude(bbox[0]), Angle.fromDegreesLongitude(bbox[2]));
            values.setValue(AVKey.SECTOR, sector);
        }
        else
        {
            String message = Logging.getMessage("generic.ProjectionUnsupported");
            Logging.logger().severe(message);
            throw new IOException(message);
        }
    }
}
