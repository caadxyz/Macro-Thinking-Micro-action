/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.texture.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.formats.dds.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.wms.*;
import org.w3c.dom.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: BasicTiledImageLayer.java 12979 2010-01-06 03:44:18Z dcollins $
 */
public class BasicTiledImageLayer extends TiledImageLayer implements BulkRetrievable
{
    private final Object fileLock = new Object();

    // Layer resource properties.
    private boolean serviceInitialized = false;
    private boolean serviceInitFailed = false;
    private AbsentResourceList absentResources = new AbsentResourceList();
    protected static final int SERVICE_CAPABILITIES_RESOURCE_ID = 1;

    public BasicTiledImageLayer(LevelSet levelSet)
    {
        super(levelSet);
    }

    public BasicTiledImageLayer(AVList params)
    {
        this(new LevelSet(params));

        String s = params.getStringValue(AVKey.DISPLAY_NAME);
        if (s != null)
            this.setName(s);

        String[] strings = (String[]) params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS);
        if (strings != null && strings.length > 0)
            this.setAvailableImageFormats(strings);

        Double d = (Double) params.getValue(AVKey.OPACITY);
        if (d != null)
            this.setOpacity(d);

        d = (Double) params.getValue(AVKey.MAX_ACTIVE_ALTITUDE);
        if (d != null)
            this.setMaxActiveAltitude(d);

        d = (Double) params.getValue(AVKey.MIN_ACTIVE_ALTITUDE);
        if (d != null)
            this.setMinActiveAltitude(d);

        d = (Double) params.getValue(AVKey.MAP_SCALE);
        if (d != null)
            this.setValue(AVKey.MAP_SCALE, d);

        d = (Double) params.getValue(AVKey.SPLIT_SCALE);
        if (d != null)
            this.setSplitScale(d);

        Boolean b = (Boolean) params.getValue(AVKey.FORCE_LEVEL_ZERO_LOADS);
        if (b != null)
            this.setForceLevelZeroLoads(b);

        b = (Boolean) params.getValue(AVKey.RETAIN_LEVEL_ZERO_TILES);
        if (b != null)
            this.setRetainLevelZeroTiles(b);

        b = (Boolean) params.getValue(AVKey.NETWORK_RETRIEVAL_ENABLED);
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        b = (Boolean) params.getValue(AVKey.USE_MIP_MAPS);
        if (b != null)
            this.setUseMipMaps(b);

        b = (Boolean) params.getValue(AVKey.USE_TRANSPARENT_TEXTURES);
        if (b != null)
            this.setUseTransparentTextures(b);

        Object o = params.getValue(AVKey.URL_CONNECT_TIMEOUT);
        if (o != null)
            this.setValue(AVKey.URL_CONNECT_TIMEOUT, o);

        o = params.getValue(AVKey.URL_READ_TIMEOUT);
        if (o != null)
            this.setValue(AVKey.URL_READ_TIMEOUT, o);

        o = params.getValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (o != null)
            this.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, o);

        ScreenCredit sc = (ScreenCredit) params.getValue(AVKey.SCREEN_CREDIT);
        if (sc != null)
            this.setScreenCredit(sc);

        if (params.getValue(AVKey.TRANSPARENCY_COLORS) != null)
            this.setValue(AVKey.TRANSPARENCY_COLORS, params.getValue(AVKey.TRANSPARENCY_COLORS));

        this.setValue(AVKey.CONSTRUCTION_PARAMETERS, params.copy());
    }

    public BasicTiledImageLayer(Document dom, AVList params)
    {
        this(dom.getDocumentElement(), params);
    }

    public BasicTiledImageLayer(Element domElement, AVList params)
    {
        this(getParamsFromDocument(domElement, params));
    }

    public BasicTiledImageLayer(String restorableStateInXml)
    {
        this(restorableStateToParams(restorableStateInXml));

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(restorableStateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", restorableStateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        this.doRestoreState(rs, null);
    }

    protected static AVList getParamsFromDocument(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        LayerConfiguration.getLayerParams(domElement, params);
        LayerConfiguration.getTiledImageLayerParams(domElement, params);
        setFallbacks(params);

        return params;
    }

    protected static void setFallbacks(AVList params)
    {
        if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null)
        {
            Angle delta = Angle.fromDegrees(36);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.getValue(AVKey.TILE_WIDTH) == null)
            params.setValue(AVKey.TILE_WIDTH, 512);

        if (params.getValue(AVKey.TILE_HEIGHT) == null)
            params.setValue(AVKey.TILE_HEIGHT, 512);

        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
            params.setValue(AVKey.FORMAT_SUFFIX, ".dds");

        if (params.getValue(AVKey.NUM_LEVELS) == null)
            params.setValue(AVKey.NUM_LEVELS, 19); // approximately 0.1 meters per pixel

        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
    }

    protected void forceTextureLoad(TextureTile tile)
    {
        final URL textureURL = this.getDataFileStore().findFile(tile.getPath(), true);

        if (textureURL != null && !this.isTextureFileExpired(tile, textureURL, this.getDataFileStore()))
        {
            this.loadTexture(tile, textureURL);
        }
    }

    protected void requestTexture(DrawContext dc, TextureTile tile)
    {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());
        Vec4 referencePoint = this.getReferencePoint(dc);
        if (referencePoint != null)
            tile.setPriority(centroid.distanceTo3(referencePoint));

        RequestTask task = new RequestTask(tile, this);
        this.getRequestQ().add(task);
    }

    private static class RequestTask implements Runnable, Comparable<RequestTask>
    {
        private final BasicTiledImageLayer layer;
        private final TextureTile tile;

        private RequestTask(TextureTile tile, BasicTiledImageLayer layer)
        {
            this.layer = layer;
            this.tile = tile;
        }

        public void run()
        {
            // TODO: check to ensure load is still needed

            final java.net.URL textureURL = this.layer.getDataFileStore().findFile(tile.getPath(), false);
            if (textureURL != null && !this.layer.isTextureFileExpired(tile, textureURL, this.layer.getDataFileStore()))
            {
                if (this.layer.loadTexture(tile, textureURL))
                {
                    layer.getLevels().unmarkResourceAbsent(tile);
                    this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    return;
                }
                else
                {
                    // Assume that something's wrong with the file and delete it.
                    this.layer.getDataFileStore().removeFile(textureURL);
                    layer.getLevels().markResourceAbsent(tile);
                    String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
                    Logging.logger().info(message);
                }
            }

            this.layer.downloadTexture(this.tile, null);
        }

        /**
         * @param that the task to compare
         *
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         *
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }
            return this.tile.getPriority() == that.tile.getPriority() ? 0 :
                this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        public String toString()
        {
            return this.tile.toString();
        }
    }

    protected boolean isTextureFileExpired(TextureTile tile, java.net.URL textureURL, FileStore fileStore)
    {
        if (!WWIO.isFileOutOfDate(textureURL, tile.getLevel().getExpiryTime()))
            return false;

        // The file has expired. Delete it.
        fileStore.removeFile(textureURL);
        String message = Logging.getMessage("generic.DataFileExpired", textureURL);
        Logging.logger().fine(message);
        return true;
    }

    private boolean loadTexture(TextureTile tile, java.net.URL textureURL)
    {
        TextureData textureData;

        synchronized (this.fileLock)
        {
            textureData = readTexture(textureURL, this.isUseMipMaps());
        }

        if (textureData == null)
            return false;

        tile.setTextureData(textureData);
        if (tile.getLevelNumber() != 0 || !this.isRetainLevelZeroTiles())
            this.addTileToCache(tile);

        return true;
    }

    private static TextureData readTexture(java.net.URL url, boolean useMipMaps)
    {
        try
        {
            return TextureIO.newTextureData(url, useMipMaps, null);
        }
        catch (Exception e)
        {
            Logging.logger().log(
                java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            return null;
        }
    }

    private void addTileToCache(TextureTile tile)
    {
        TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
    }

    // *** Bulk download ***
    // *** Bulk download ***
    // *** Bulk download ***

    /**
     * Start a new {@link BulkRetrievalThread} that downloads all imagery for a given sector and resolution to the
     * current World Wind file cache, without downloading imagery that is already in the cache.
     * <p/>
     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
     * a downloader that has not been started, construct a {@link BasicTiledImageLayerBulkDownloader}.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to download imagery for.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param listener   an optional retrieval listener. May be null.
     *
     * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector
     * does not intersect the layer bounding sector.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     * @see BasicTiledImageLayerBulkDownloader
     */
    public BulkRetrievalThread makeLocal(Sector sector, double resolution, BulkRetrievalListener listener)
    {
        return makeLocal(sector, resolution, null, listener);
    }

    /**
     * Start a new {@link BulkRetrievalThread} that downloads all imagery for a given sector and resolution to a
     * specified {@link FileStore}, without downloading imagery tht is already in the file store.
     * <p/>
     * This method creates and starts a thread to perform the download. A reference to the thread is returned. To create
     * a downloader that has not been started, construct a {@link BasicTiledImageLayerBulkDownloader}.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to download data for.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param fileStore  the file store in which to place the downloaded imagery. If null the current World Wind file
     *                   cache is used.
     * @param listener   an optional retrieval listener. May be null.
     *
     * @return the {@link BulkRetrievalThread} executing the retrieval or <code>null</code> if the specified sector
     * does not intersect the layer bounding sector.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     * @see BasicTiledImageLayerBulkDownloader
     */
    public BulkRetrievalThread makeLocal(Sector sector, double resolution, FileStore fileStore,
        BulkRetrievalListener listener)
    {
        Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
        if (targetSector == null)
            return null;

        BasicTiledImageLayerBulkDownloader thread = new BasicTiledImageLayerBulkDownloader(this, targetSector,
            resolution, fileStore != null ? fileStore : this.getDataFileStore(), listener);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Get the estimated size in bytes of the imagery not in the World Wind file cache for the given sector and
     * resolution.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     *
     * @return the estimated size in bytes of the missing imagery.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution)
    {
        return this.getEstimatedMissingDataSize(sector, resolution, null);
    }

    /**
     * Get the estimated size in bytes of the imagery not in a specified file store for a specified sector and
     * resolution.
     * <p/>
     * Note that the target resolution must be provided in radians of latitude per texel, which is the resolution in
     * meters divided by the globe radius.
     *
     * @param sector     the sector to estimate.
     * @param resolution the target resolution, provided in radians of latitude per texel.
     * @param fileStore  the file store to examine. If null the current World Wind file cache is used.
     *
     * @return the estimated size in byte of the missing imagery.
     *
     * @throws IllegalArgumentException if the sector is null or the resolution is less than zero.
     */
    public long getEstimatedMissingDataSize(Sector sector, double resolution, FileStore fileStore)
    {
        Sector targetSector = sector != null ? getLevels().getSector().intersection(sector) : null;
        if (targetSector == null)
            return 0;

        BasicTiledImageLayerBulkDownloader downloader = new BasicTiledImageLayerBulkDownloader(this, sector, resolution,
            fileStore != null ? fileStore : this.getDataFileStore(), null);

        return downloader.getEstimatedMissingDataSize();
    }

    // *** Tile download ***
    // *** Tile download ***
    // *** Tile download ***

    protected void downloadTexture(final TextureTile tile, DownloadPostProcessor postProcessor)
    {
        if (!this.isNetworkRetrievalEnabled())
        {
            this.getLevels().markResourceAbsent(tile);
            return;
        }

        if (!WorldWind.getRetrievalService().isAvailable())
            return;

        java.net.URL url;
        try
        {
            url = tile.getResourceURL();
            if (url == null)
                return;

            if (WorldWind.getNetworkStatus().isHostUnavailable(url))
            {
                this.getLevels().markResourceAbsent(tile);
                return;
            }
        }
        catch (java.net.MalformedURLException e)
        {
            Logging.logger().log(java.util.logging.Level.SEVERE,
                Logging.getMessage("layers.TextureLayer.ExceptionCreatingTextureUrl", tile), e);
            return;
        }

        Retriever retriever;

        if ("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()))
        {
            if (postProcessor == null)
                postProcessor = new DownloadPostProcessor(tile, this);
            retriever = new HTTPRetriever(url, postProcessor);
        }
        else
        {
            Logging.logger().severe(
                Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", url.toString()));
            return;
        }

        // Apply any overridden timeouts.
        Integer cto = AVListImpl.getIntegerValue(this, AVKey.URL_CONNECT_TIMEOUT);
        if (cto != null && cto > 0)
            retriever.setConnectTimeout(cto);
        Integer cro = AVListImpl.getIntegerValue(this, AVKey.URL_READ_TIMEOUT);
        if (cro != null && cro > 0)
            retriever.setReadTimeout(cro);
        Integer srl = AVListImpl.getIntegerValue(this, AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);
        if (srl != null && srl > 0)
            retriever.setStaleRequestLimit(srl);

        WorldWind.getRetrievalService().runRetriever(retriever, tile.getPriority());
    }

    private void saveBuffer(java.nio.ByteBuffer buffer, java.io.File outFile) throws java.io.IOException
    {
        synchronized (this.fileLock) // sychronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    protected static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        // TODO: Rewrite this inner class, factoring out the generic parts.
        protected final TextureTile tile;
        protected final BasicTiledImageLayer layer;
        protected final FileStore fileStore;

        public DownloadPostProcessor(TextureTile tile, BasicTiledImageLayer layer)
        {
            this.tile = tile;
            this.layer = layer;
            this.fileStore = layer.getDataFileStore();
        }

        public DownloadPostProcessor(TextureTile tile, BasicTiledImageLayer layer, FileStore fileStore)
        {
            this.tile = tile;
            this.layer = layer;
            this.fileStore = fileStore;
        }

        public ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            try
            {
                if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                {
                    if (retriever.getState().equals(Retriever.RETRIEVER_STATE_ERROR))
                        this.layer.getLevels().markResourceAbsent(this.tile);

                    return null;
                }

                URLRetriever r = (URLRetriever) retriever;
                ByteBuffer buffer = r.getBuffer();

                if (retriever instanceof HTTPRetriever)
                {
                    HTTPRetriever htr = (HTTPRetriever) retriever;
                    if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
                    {
                        // Mark tile as missing to avoid excessive attempts
                        this.layer.getLevels().markResourceAbsent(this.tile);
                        return null;
                    }
                    else if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
                    {
                        // Also mark tile as missing, but for an unknown reason.
                        this.layer.getLevels().markResourceAbsent(this.tile);
                        return null;
                    }
                }

                final File outFile = this.fileStore.newFile(this.tile.getPath());
                if (outFile == null)
                    return null;

                if (outFile.exists())
                    return buffer;

                if (this.layer.getValue(AVKey.DELETE_CACHE_ON_EXIT) != null)
                    outFile.deleteOnExit();

                // TODO: Better, more generic and flexible handling of file-format type
                if (buffer != null)
                {
                    String contentType = r.getContentType();
                    if (contentType == null)
                    {
                        // TODO: logger message
                        return null;
                    }

                    if (contentType.contains("xml") || contentType.contains("html") || contentType.contains("text"))
                    {
                        this.layer.getLevels().markResourceAbsent(this.tile);

                        StringBuffer sb = new StringBuffer();
                        while (buffer.hasRemaining())
                        {
                            sb.append((char) buffer.get());
                        }
                        // TODO: parse out the message if the content is xml or html.
                        Logging.logger().severe(sb.toString());

                        return null;
                    }
                    else if (contentType.contains("dds"))
                    {
                        this.layer.saveBuffer(buffer, outFile);
                    }
                    else if (contentType.contains("zip"))
                    {
                        // Assume it's zipped DDS, which the retriever would have unzipped into the buffer.
                        this.layer.saveBuffer(buffer, outFile);
                    }
                    else if (outFile.getName().endsWith(".dds"))
                    {
                        // Convert to DDS and save the result.

                        int[] colors = (int[]) layer.getValue(AVKey.TRANSPARENCY_COLORS);
                        if (colors != null)
                        {
                            // Map the specified colors to transparent.
                            BufferedImage image = ImageUtil.mapTransparencyColors(buffer, colors);
                            if (image != null)
                            {
                                buffer = DDSCompressor.compressImage(image);
                                if (buffer != null)
                                    this.layer.saveBuffer(buffer, outFile);
                            }
                        }
                        else
                        {
                            buffer = DDSCompressor.compressImageBuffer(buffer);
                            if (buffer != null)
                                this.layer.saveBuffer(buffer, outFile);
                        }
                    }
                    else if (contentType.contains("image"))
                    {
                        // Just save whatever it is to the cache.

                        int[] colors = (int[]) layer.getValue(AVKey.TRANSPARENCY_COLORS);
                        if (colors != null)
                        {
                            // Map the specified colors to transparent.
                            BufferedImage image = ImageUtil.mapTransparencyColors(buffer, colors);
                            if (image != null)
                                ImageIO.write(image, contentType.split("/")[1], outFile);
                        }
                        else
                        {
                            this.layer.saveBuffer(buffer, outFile);
                        }
                    }

                    if (buffer != null)
                    {
                        // We've successfully written data to the cache. Check if there's a configuration file for this
                        // layer in the cache, and create one if there isn't.
                        this.layer.writeConfigurationFile(this.fileStore);
                        // Fire a property change to denote that the layer's backing data has changed.
                        this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    }
                    return buffer;
                }
            }
            catch (ClosedByInterruptException e)
            {
                Logging.logger().log(java.util.logging.Level.FINE,
                    Logging.getMessage("generic.OperationCancelled", "tiled image retrieval"), e);
            }
            catch (java.io.IOException e)
            {
                this.layer.getLevels().markResourceAbsent(this.tile);
                Logging.logger().log(java.util.logging.Level.SEVERE,
                    Logging.getMessage("layers.TextureLayer.ExceptionSavingRetrievedTextureFile", tile.getPath()), e);
            }

            return null;
        }
    }

    //**************************************************************//
    //********************  Resources  *****************************//
    //**************************************************************//

    protected void checkResources()
    {
        AVList params = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (params == null)
            return;

        this.initializeResources(params);
    }

    protected void initializeResources(AVList params)
    {
        Boolean b = (Boolean) params.getValue(AVKey.RETRIEVE_PROPERTIES_FROM_SERVICE);
        if (b != null && b)
            this.initPropertiesFromService(params);
    }

    protected void initPropertiesFromService(AVList params)
    {
        // Return immediately if the layer has been sucessfully initialized from its service's online resource, or if
        // a previous attempt at initialization failed. We do this to avoid any additional overhead incurred by invoking
        // this method.
        if (this.serviceInitialized || this.serviceInitFailed)
            return;

        // Return immediately if the service's WMS Capabilities resource is absent. We do this to avoid the additional
        // overhead incurred by constructing the WMS Capabilities URL, performing a cache lookup, and potentially
        // initiating a retrieval of the Capabilities document. This check is critical to avoiding overhead when World
        // Wind is running in offline mode, since the service initialization never fails or succeeds. Instead, we keep
        // checking the absent resource list in case the service becomes available at a later time.
        if (this.absentResources.isResourceAbsent(SERVICE_CAPABILITIES_RESOURCE_ID))
            return;

        // Mark the initialization as failed and return if the layer's AVList does not contain the necessary parameters.
        // Marking the initialization as failed avoids any additional overhead incurred by subsequent attempts.
        URL url = DataConfigurationUtils.getOGCGetCapabilitiesURL(params);
        if (url == null)
        {
            this.serviceInitFailed = true;
            return;
        }

        // Get the service's WMS Capabilities resource from the session cache, or initiate a retrieval to fetch it in
        // a non-EDT thread. SessionCacheUtils.getOrRetrieveSessionCapabilities() returns null if it initiated a
        // retrieval, or if the WMS Capabilities URL is unavailable.
        //
        // Note that we use the URL's String representation as the cache key. We cannot use the URL itself, because
        // the cache invokes the methods Object.hashCode() and Object.equals() on the cache key. URL's implementations
        // of hashCode() and equals() perform blocking IO calls. World Wind does not perform blocking calls during
        // rendering, and this method is likely to be called from the rendering thread.
        Object cacheKey = url.toString();
        Capabilities caps = SessionCacheUtils.getOrRetrieveSessionCapabilities(url, WorldWind.getRetrievalService(),
            WorldWind.getSessionCache(), cacheKey, this.absentResources, SERVICE_CAPABILITIES_RESOURCE_ID, this,
            AVKey.LAYER);
        if (caps == null)
            return;

        // We have sucessfully retrieved the service's WMS Capabilities resource. Intialize any layer properties from
        // the Capaibilities document, and mark the initialization as successful. Marking the initialization as
        // successful avoids any additional overhead incurred by subsequent attempts.
        this.initPropertiesFromCapabilities(caps, params);
        this.serviceInitialized = true;
    }

    protected void initPropertiesFromCapabilities(Capabilities caps, AVList params)
    {
        String[] names = DataConfigurationUtils.getOGCLayerNames(params);
        if (names == null || names.length == 0)
            return;

        Long expiryTime = caps.getLayerLatestLastUpdateTime(caps, names);
        if (expiryTime != null)
            this.setExpiryTime(expiryTime);
    }

    //**************************************************************//
    //********************  Configuration File  ********************//
    //**************************************************************//

    protected void writeConfigurationFile(FileStore fileStore)
    {
        // TODO: configurable max attempts for creating a configuration file.

        try
        {
            AVList configParams = this.getConfigurationParams(null);
            this.writeConfigurationParams(fileStore, configParams);
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("generic.ExceptionAttemptingToWriteConfigurationFile");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void writeConfigurationParams(FileStore fileStore, AVList params)
    {
        // Determine what the configuration file name should be based on the configuration parameters. Assume an XML
        // configuration document type, and append the XML file suffix.
        String fileName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        // Check if this component needs to write a configuration file. This happens outside of the synchronized block
        // to improve multithreaded performance for the common case: the configuration file already exists, this just
        // need to check that it's there and return. If the file exists but is expired, do not remove it -  this
        // removes the file inside the synchronized block below.
        if (!this.needsConfigurationFile(fileStore, fileName, params, false))
            return;

        synchronized (this.fileLock)
        {
            // Check again if the component needs to write a configuration file, potentially removing any existing file
            // which has expired. This additional check is necessary because the file could have been created by
            // another thread while we were waiting for the lock.
            if (!this.needsConfigurationFile(fileStore, fileName, params, true))
                return;

            this.doWriteConfigurationParams(fileStore, fileName, params);
        }
    }

    protected void doWriteConfigurationParams(FileStore fileStore, String fileName, AVList params)
    {
        Document doc = this.createConfigurationDocument(params);

        DataConfigurationUtils.saveDataConfigDocument(doc, fileStore, fileName);
    }

    protected boolean needsConfigurationFile(FileStore fileStore, String fileName, AVList params,
        boolean removeIfExpired)
    {
        long expiryTime = this.getExpiryTime();
        if (expiryTime <= 0)
            expiryTime = AVListImpl.getLongValue(params, AVKey.EXPIRY_TIME, 0L);

        return !DataConfigurationUtils.hasDataConfigFile(fileStore, fileName, removeIfExpired, expiryTime);
    }

    protected AVList getConfigurationParams(AVList params)
    {
        if (params == null)
            params = new AVListImpl();

        // Gather all the construction parameters if they are available.
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null)
            params.setValues(constructionParams);

        // Gather any missing LevelSet parameters from the LevelSet itself.
        DataConfigurationUtils.getLevelSetParams(this.getLevels(), params);

        return params;
    }

    protected Document createConfigurationDocument(AVList params)
    {
        return LayerConfiguration.createTiledImageLayerDocument(params);
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    public String getRestorableState()
    {
        // We only create a restorable state XML if this elevation model was constructed with an AVList.
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams == null)
            return null;

        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (rs == null)
            return null;

        this.doGetRestorableState(rs, null);
        return rs.getStateAsXml();
    }

    public void restoreState(String stateInXml)
    {
        String message = Logging.getMessage("RestorableSupport.RestoreRequiresConstructor");
        Logging.logger().severe(message);
        throw new UnsupportedOperationException(message);
    }

    private void doGetRestorableState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        AVList constructionParams = (AVList) this.getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (constructionParams != null)
        {
            for (Map.Entry<String, Object> avp : constructionParams.getEntries())
            {
                this.doGetRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, context);
            }
        }

        rs.addStateValueAsBoolean(context, "Layer.Enabled", this.isEnabled());
        rs.addStateValueAsDouble(context, "Layer.Opacity", this.getOpacity());
        rs.addStateValueAsDouble(context, "Layer.MinActiveAltitude", this.getMinActiveAltitude());
        rs.addStateValueAsDouble(context, "Layer.MaxActiveAltitude", this.getMaxActiveAltitude());
        rs.addStateValueAsBoolean(context, "Layer.NetworkRetrievalEnabled", this.isNetworkRetrievalEnabled());
        rs.addStateValueAsString(context, "Layer.Name", this.getName());
        rs.addStateValueAsBoolean(context, "TiledImageLayer.UseMipMaps", this.isUseMipMaps());
        rs.addStateValueAsBoolean(context, "TiledImageLayer.UseTransparentTextures", this.isUseTransparentTextures());

        RestorableSupport.StateObject so = rs.addStateObject(context, "avlist");
        for (Map.Entry<String, Object> avp : this.getEntries())
        {
            this.doGetRestorableStateForAVPair(avp.getKey(), avp.getValue(), rs, so);
        }
    }

    protected void doGetRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (value == null)
            return;

        if (key.equals(AVKey.CONSTRUCTION_PARAMETERS))
            return;

        if (value instanceof LatLon)
        {
            rs.addStateValueAsLatLon(context, key, (LatLon) value);
        }
        else if (value instanceof Sector)
        {
            rs.addStateValueAsSector(context, key, (Sector) value);
        }
        else if (value instanceof Color)
        {
            rs.addStateValueAsColor(context, key, (Color) value);
        }
        else
        {
            rs.addStateValueAsString(context, key, value.toString());
        }
    }

    protected void doRestoreState(RestorableSupport rs, RestorableSupport.StateObject context)
    {
        Boolean b = rs.getStateValueAsBoolean(context, "Layer.Enabled");
        if (b != null)
            this.setEnabled(b);

        Double d = rs.getStateValueAsDouble(context, "Layer.Opacity");
        if (d != null)
            this.setOpacity(d);

        d = rs.getStateValueAsDouble(context, "Layer.MinActiveAltitude");
        if (d != null)
            this.setMinActiveAltitude(d);

        d = rs.getStateValueAsDouble(context, "Layer.MaxActiveAltitude");
        if (d != null)
            this.setMaxActiveAltitude(d);

        b = rs.getStateValueAsBoolean(context, "Layer.NetworkRetrievalEnabled");
        if (b != null)
            this.setNetworkRetrievalEnabled(b);

        String s = rs.getStateValueAsString(context, "Layer.Name");
        if (s != null)
            this.setName(s);

        b = rs.getStateValueAsBoolean(context, "TiledImageLayer.UseMipMaps");
        if (b != null)
            this.setUseMipMaps(b);

        b = rs.getStateValueAsBoolean(context, "TiledImageLayer.UseTransparentTextures");
        if (b != null)
            this.setUseTransparentTextures(b);

        RestorableSupport.StateObject so = rs.getStateObject(context, "avlist");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so, "");
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                        this.doRestoreStateForObject(rs, avp);
                }
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected void doRestoreStateForObject(RestorableSupport rs, RestorableSupport.StateObject so)
    {
        if (so == null)
            return;

        this.setValue(so.getName(), so.getValue());
    }

    protected static AVList restorableStateToParams(String stateInXml)
    {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport rs;
        try
        {
            rs = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }

        AVList params = new AVListImpl();
        restoreStateForParams(rs, null, params);
        return params;
    }

    protected static void restoreStateForParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params)
    {
        String s = rs.getStateValueAsString(context, AVKey.DATA_CACHE_NAME);
        if (s != null)
            params.setValue(AVKey.DATA_CACHE_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.SERVICE);
        if (s != null)
            params.setValue(AVKey.SERVICE, s);

        s = rs.getStateValueAsString(context, AVKey.DATASET_NAME);
        if (s != null)
            params.setValue(AVKey.DATASET_NAME, s);

        s = rs.getStateValueAsString(context, AVKey.FORMAT_SUFFIX);
        if (s != null)
            params.setValue(AVKey.FORMAT_SUFFIX, s);

        Integer i = rs.getStateValueAsInteger(context, AVKey.NUM_EMPTY_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.NUM_LEVELS);
        if (i != null)
            params.setValue(AVKey.NUM_LEVELS, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_WIDTH);
        if (i != null)
            params.setValue(AVKey.TILE_WIDTH, i);

        i = rs.getStateValueAsInteger(context, AVKey.TILE_HEIGHT);
        if (i != null)
            params.setValue(AVKey.TILE_HEIGHT, i);

        Long lo = rs.getStateValueAsLong(context, AVKey.EXPIRY_TIME);
        if (lo != null)
            params.setValue(AVKey.EXPIRY_TIME, lo);

        LatLon ll = rs.getStateValueAsLatLon(context, AVKey.LEVEL_ZERO_TILE_DELTA);
        if (ll != null)
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);

        ll = rs.getStateValueAsLatLon(context, AVKey.TILE_ORIGIN);
        if (ll != null)
            params.setValue(AVKey.TILE_ORIGIN, ll);

        Sector sector = rs.getStateValueAsSector(context, AVKey.SECTOR);
        if (sector != null)
            params.setValue(AVKey.SECTOR, sector);
    }
}
