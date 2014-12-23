/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

import java.text.MessageFormat;

/**
 * @author dcollins
 * @version $Id: ReadableDataRaster.java 11704 2009-06-17 20:28:39Z dcollins $
 */
public class ReadableDataRaster implements DataRaster
{
    private DataSource dataSource;
    private DataRasterReader rasterReader;
    private MemoryCache rasterCache;
    @SuppressWarnings({"FieldCanBeLocal"})
    private CacheListener cacheListener;

    public ReadableDataRaster(DataSource source, DataRasterReader reader, MemoryCache cache) throws java.io.IOException
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.DataSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (reader == null)
        {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.dataSource = source;
        this.rasterReader = reader;
        this.rasterCache = cache;

        if (this.rasterCache != null)
        {
            this.cacheListener = new CacheListener(this.dataSource);
            this.rasterCache.addCacheListener(this.cacheListener);
        }

        if (!this.rasterReader.canRead(this.dataSource))
        {
            String message = Logging.getMessage("DataRaster.CannotRead", this.dataSource);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        this.assembleMetadata();
    }

    public ReadableDataRaster(DataSource source, DataRasterReader reader) throws java.io.IOException
    {
        this(source, reader, null);
    }

    public static DataRasterReader findReaderFor(DataSource source, DataRasterReader[] readers)
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.DataSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (readers == null)
        {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (DataRasterReader reader : readers)
        {
            if (reader != null)
                if (reader.canRead(source))
                    return reader;
        }

        return null;
    }

    public DataSource getDataSource()
    {
        return this.dataSource;
    }

    public DataRasterReader getReader()
    {
        return this.rasterReader;
    }

    public MemoryCache getCache()
    {
        return this.rasterCache;
    }

    public int getWidth()
    {
        DataSource source = this.getDataSource();
        return (Integer) source.getValue(AVKey.WIDTH);
    }

    public int getHeight()
    {
        DataSource source = this.getDataSource();
        return (Integer) source.getValue(AVKey.HEIGHT);
    }

    public Sector getSector()
    {
        DataSource source = this.getDataSource();
        return (Sector) source.getValue(AVKey.SECTOR);
    }

    public void drawOnCanvas(DataRaster canvas, Sector clipSector)
    {
        if (canvas == null)
        {
            String message = Logging.getMessage("nullValue.CanvasIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DataRaster[] dataRasters;
        try
        {
            dataRasters = this.getDataRasters();
        }
        catch (java.io.IOException e)
        {
            String message = Logging.getMessage("DataRaster.CannotRead", e.getMessage());
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new IllegalArgumentException(message);
        }

        if (dataRasters != null)
            for (DataRaster raster : dataRasters)
            {
                raster.drawOnCanvas(canvas, clipSector);
            }
    }

    public void drawOnCanvas(DataRaster canvas)
    {
        if (canvas == null)
        {
            String message = Logging.getMessage("nullValue.CanvasIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.drawOnCanvas(canvas, null);
    }

    protected boolean isMissingMetadata()
    {
        DataSource source = this.getDataSource();

        Object o = source.getValue(AVKey.WIDTH);
        if (o == null || !(o instanceof Integer))
            return true;

        o = source.getValue(AVKey.HEIGHT);
        if (o == null || !(o instanceof Integer))
            return true;

        o = source.getValue(AVKey.SECTOR);
        //noinspection RedundantIfStatement
        if (o == null || !(o instanceof Sector))
            return true;

        return false;
    }

    protected void assembleMetadata() throws java.io.IOException
    {
        // If have all the metadata we need, then exit.
        if (!this.isMissingMetadata())
            return;

        // Attempt to read the metadata.
        DataSource source = this.getDataSource();
        this.getReader().readMetadata(source, source);

        // If any metadata is missing after the read, then throw an IOException.
        if (this.isMissingMetadata())
        {
            String message = Logging.getMessage("DataRaster.MissingMetadata", this.getDataSource());
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }
    }

    protected DataRaster[] getDataRasters() throws java.io.IOException
    {
        DataSource source = this.getDataSource();
        MemoryCache cache = this.getCache();

        DataRaster[] rasters = (DataRaster[]) cache.getObject(source);

        // If the cache entry is null, and the cache does not contain a null reference, then read the file and add
        // the raster to the cache.
        if (rasters == null && !cache.contains(source))
        {
            try
            {
                rasters = this.getReader().read(source);
            }
            finally
            {
                // Add rasters to the cache. If rasters is null we add that null reference to the cache to prevent
                // multiple failed reads.
                cache.add(source, rasters, getSizeInBytes(rasters));
            }
        }

        return rasters;
    }

    private static long getSizeInBytes(DataRaster[] rasters)
    {
        long sizeInBytes = 0L;
        if (rasters != null)
            for (DataRaster raster : rasters)
            {
                if (raster != null && raster instanceof Cacheable)
                    sizeInBytes += ((Cacheable) raster).getSizeInBytes();
            }
        return sizeInBytes;
    }

    private static class CacheListener implements MemoryCache.CacheListener
    {
        private Object key;

        private CacheListener(Object key)
        {
            this.key = key;
        }

        public void entryRemoved(Object key, Object clientObject)
        {
            if (key != this.key)
                return;

            if (clientObject == null || !(clientObject instanceof DataRaster[]))
            {
                String message = MessageFormat.format("Cannot dispose {0}", clientObject);
                Logging.logger().warning(message);
                return;
            }

            try
            {
                this.dispose((DataRaster[]) clientObject);
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("generic.ExceptionWhileDisposing", clientObject);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }

        private void dispose(DataRaster[] dataRasters)
        {
            for (DataRaster raster : dataRasters)
            {
                if (raster != null && raster instanceof Disposable)
                    ((Disposable) raster).dispose();
            }
        }
    }
}
