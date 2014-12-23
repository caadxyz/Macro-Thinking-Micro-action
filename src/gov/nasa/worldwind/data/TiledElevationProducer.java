/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: TiledElevationProducer.java 9495 2009-03-18 19:01:38Z dcollins $
 */
public class TiledElevationProducer extends TiledRasterProducer
{
    private static final String DEFAULT_FORMAT_SUFFIX = ".bil";
    // Statically reference the readers used to for unknown data sources. This drastically improves the performance of
    // reading large quantities of sources. Since the readers are invoked from a single thread, they can be
    // safely re-used.
    private static DataRasterReader[] readers = new DataRasterReader[]
    {
        new BILRasterReader()
    };

    public TiledElevationProducer(MemoryCache cache, int writeThreadPoolSize)
    {
        super(cache, writeThreadPoolSize);
    }

    public TiledElevationProducer()
    {
    }

    public String getDataSourceDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("DataStoreProducer.TiledElevation.Description"));
        sb.append(" (").append(super.getDataSourceDescription()).append(")");
        return sb.toString();
    }

    protected DataRaster createDataRaster(int width, int height, Sector sector, AVList params)
    {
        // Create a BIL elevation raster to hold the tile's data.
        AVList bufferParams = new AVListImpl();

        // ByteBufferRaster is expected buffer type in "DATA_TYPE" key,
        // but WorldFile specifies data type in the "PIXEL_TYPE" key.
        bufferParams.setValue(AVKey.DATA_TYPE, params.getValue(AVKey.PIXEL_TYPE));
        bufferParams.setValue(AVKey.BYTE_ORDER, params.getValue(AVKey.BYTE_ORDER));
        ByteBufferRaster bufferRaster = new ByteBufferRaster(width, height, sector, bufferParams);

        // Clear the raster with the missing data replacment.
        // This code expects the string "gov.nasa.worldwind.avkey.MissingDataValue", which now corresponds to the key 
        // MISSING_DATA_REPLACEMENT.
        Object o = params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
        if (o != null && o instanceof Double)
        {
            Double missingDataValue = (Double) o;
            bufferRaster.fill(missingDataValue);
            bufferRaster.setTransparentValue(missingDataValue);
        }

        return bufferRaster;
    }

    protected DataRasterReader[] getDataRasterReaders()
    {
        return readers;
    }

    protected DataRasterWriter[] getDataRasterWriters()
    {
        return new DataRasterWriter[]
        {
            new BILRasterWriter()
        };
    }
    
    protected String validateDataSource(DataSource dataSource)
    {
        if (dataSource.getSource() instanceof DataRaster)
            if (!(dataSource.getSource() instanceof BufferedDataRaster))
                return Logging.getMessage("DataStoreProducer.InvalidDataSource", dataSource);

        return super.validateDataSource(dataSource);
    }

    protected void initProductionParameters(AVList params)
    {
        super.initProductionParameters(params);

        Object o = params.getValue(AVKey.FORMAT_SUFFIX);
        if (o == null || !(o instanceof String) || ((String) o).length() == 0)
            params.setValue(AVKey.FORMAT_SUFFIX, DEFAULT_FORMAT_SUFFIX);

        o = params.getValue(AVKey.BYTE_ORDER);
        if (o == null)
            params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);

        o = params.getValue(AVKey.PIXEL_TYPE);
        if (o == null)
            params.setValue(AVKey.PIXEL_TYPE, AVKey.FLOAT32);

        // This code expects the string "gov.nasa.worldwind.avkey.MissingDataValue", which now corresponds to the key
        // MISSING_DATA_REPLACEMENT.
        o = params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
        if (o == null || !(o instanceof Double))
            params.setValue(AVKey.MISSING_DATA_REPLACEMENT, -9999.0);

        o = params.getValue(AVKey.DATA_TYPE);
        if (o == null)
            params.setValue(AVKey.DATA_TYPE, AVKey.TILED_ELEVATIONS);
    }

    protected LatLon computeRasterTileDelta(int tileWidth, int tileHeight, Iterable<? extends DataRaster> rasters)
    {
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        // Compute the tile size in latitude and longitude, given a raster's sector and dimension, and the tile
        // dimensions. In this computation a pixel is assumed to have no dimension. We measure the distance between
        // pixels rather than some pixel dimension.
        double latDelta = (tileHeight - 1) * pixelSize.getLatitude().degrees;
        double lonDelta = (tileWidth  - 1) * pixelSize.getLongitude().degrees;
        return LatLon.fromDegrees(latDelta, lonDelta);
    }
}
