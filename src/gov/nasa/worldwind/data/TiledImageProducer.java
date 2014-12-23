/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.cache.MemoryCache;

/**
 * @author dcollins
 * @version $Id: TiledImageProducer.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public class TiledImageProducer extends TiledRasterProducer
{
    private static final String DEFAULT_FORMAT_SUFFIX = ".dds";
    // Statically reference the readers used to for unknown data sources. This drastically improves the performance of
    // reading large quantities of sources. Since the readers are invoked from a single thread, they can be
    // safely re-used.
    private static DataRasterReader[] readers = new DataRasterReader[]
    {
        new ImageIORasterReader(),
        new GeotiffRasterReader(),
        new RPFRasterReader()
    };

    public TiledImageProducer(MemoryCache cache, int writeThreadPoolSize)
    {
        super(cache, writeThreadPoolSize);
    }

    public TiledImageProducer()
    {
    }

    public String getDataSourceDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("DataStoreProducer.TiledImagery.Description"));
        sb.append(" (").append(super.getDataSourceDescription()).append(")");
        return sb.toString();
    }

    protected DataRaster createDataRaster(int width, int height, Sector sector, AVList params)
    {
        int transparency = java.awt.image.BufferedImage.TRANSLUCENT; // TODO: make configurable
        //noinspection UnnecessaryLocalVariable
        BufferedImageRaster raster = new BufferedImageRaster(width, height, transparency, sector);
        return raster;
    }

    protected DataRasterReader[] getDataRasterReaders()
    {
        return readers;
    }

    protected DataRasterWriter[] getDataRasterWriters()
    {
        return new DataRasterWriter[]
        {
            new ImageIORasterWriter(),
            new DDSRasterWriter()
        };
    }
    
    protected String validateDataSource(DataSource dataSource)
    {
        if (dataSource.getSource() instanceof DataRaster)
            if (!(dataSource.getSource() instanceof BufferedImageRaster))
                return Logging.getMessage("DataStoreProducer.InvalidDataSource", dataSource);

        return super.validateDataSource(dataSource);
    }

    protected void initProductionParameters(AVList params)
    {
        super.initProductionParameters(params);

        Object o = params.getValue(AVKey.FORMAT_SUFFIX);
        if (o == null || !(o instanceof String) || ((String) o).length() == 0)
            params.setValue(AVKey.FORMAT_SUFFIX, DEFAULT_FORMAT_SUFFIX);

        o = params.getValue(AVKey.DATA_TYPE);
        if (o == null)
            params.setValue(AVKey.DATA_TYPE, AVKey.TILED_IMAGERY);
    }
}
