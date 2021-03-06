/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.wms;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

/**
 * @author tag
 * @version $Id: WMSTiledImageLayer.java 12963 2009-12-24 09:54:34Z tgaskins $
 */
public class WMSTiledImageLayer extends BasicTiledImageLayer
{
    private static final String[] formatOrderPreference = new String[]
        {
            "image/dds", "image/png", "image/jpeg"
        };

    public WMSTiledImageLayer(AVList params)
    {
        super(params);
    }

    public WMSTiledImageLayer(Document dom, AVList params)
    {
        this(dom.getDocumentElement(), params);
    }

    public WMSTiledImageLayer(Element domElement, AVList params)
    {
        super(domElement, wmsGetParamsFromDocument(domElement, params));
    }

    public WMSTiledImageLayer(Capabilities caps, AVList params)
    {
        super(wmsGetParamsFromCapsDoc(caps, params));
    }

    public WMSTiledImageLayer(String stateInXml)
    {
        this(wmsRestorableStateToParams(stateInXml));

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

        this.doRestoreState(rs, null);
    }

    protected static AVList wmsGetParamsFromDocument(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        LayerConfiguration.getWMSTiledImageLayerParams(domElement, params);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params));

        return params;
    }

    protected static AVList wmsGetParamsFromCapsDoc(Capabilities caps, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WMSCapabilities");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.LayerConfigParams");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            LayerConfiguration.getWMSTiledImageLayerParams(caps, formatOrderPreference, params);
        }
        catch (IllegalArgumentException e)
        {
            String message = Logging.getMessage("WMS.MissingLayerParameters");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }
        catch (WWRuntimeException e)
        {
            String message = Logging.getMessage("WMS.MissingCapabilityValues");
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }

        setFallbacks(params);

        // Setup WMS URL builder.
        params.setValue(AVKey.WMS_VERSION, caps.getVersion());
        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params));
        // Setup default WMS tiled image layer behaviors.
        params.setValue(AVKey.USE_TRANSPARENT_TEXTURES, true);

        return params;
    }

    // TODO: consolidate common code in WMSTiledImageLayer.URLBuilder and WMSBasicElevationModel.URLBuilder
    public static class URLBuilder implements TileUrlBuilder
    {
        private static final String MAX_VERSION = "1.3.0";

        private final String layerNames;
        private final String styleNames;
        private final String imageFormat;
        private final String wmsVersion;
        private final String crs;
        private final String backgroundColor;
        public String URLTemplate = null;

        public URLBuilder(AVList params)
        {
            this.layerNames = params.getStringValue(AVKey.LAYER_NAMES);
            this.styleNames = params.getStringValue(AVKey.STYLE_NAMES);
            this.imageFormat = params.getStringValue(AVKey.IMAGE_FORMAT);
            this.backgroundColor = params.getStringValue(AVKey.WMS_BACKGROUND_COLOR);
            String version = params.getStringValue(AVKey.WMS_VERSION);

            if (version == null || version.compareTo(MAX_VERSION) >= 0)
            {
                this.wmsVersion = MAX_VERSION;
                this.crs = "&crs=CRS:84";
            }
            else
            {
                this.wmsVersion = version;
                this.crs = "&srs=EPSG:4326";
            }
        }

        public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException
        {
            StringBuffer sb;
            if (this.URLTemplate == null)
            {
                sb = new StringBuffer(WWXML.fixGetMapString(tile.getLevel().getService()));

                if (!sb.toString().toLowerCase().contains("service=wms"))
                    sb.append("service=WMS");
                sb.append("&request=GetMap");
                sb.append("&version=");
                sb.append(this.wmsVersion);
                sb.append(this.crs);
                sb.append("&layers=");
                sb.append(this.layerNames);
                sb.append("&styles=");
                sb.append(this.styleNames != null ? this.styleNames : "");
                sb.append("&format=");
                if (altImageFormat == null)
                    sb.append(this.imageFormat);
                else
                    sb.append(altImageFormat);
                sb.append("&transparent=TRUE");
                if (this.backgroundColor != null)
                    sb.append("&bgcolor=").append(this.backgroundColor);

                this.URLTemplate = sb.toString();
            }
            else
            {
                sb = new StringBuffer(this.URLTemplate);
            }

            sb.append("&width=");
            sb.append(tile.getWidth());
            sb.append("&height=");
            sb.append(tile.getHeight());

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());
//            sb.append("&"); // terminate the query string

            return new java.net.URL(sb.toString().replace(" ", "%20"));
        }

    }

    private static class ComposeImageTile extends TextureTile
    {
        private int width;
        private int height;
        private File file;

        public ComposeImageTile(Sector sector, String mimeType, Level level, int width, int height)
            throws IOException
        {
            super(sector, level, -1, -1); // row and column aren't used and need to signal that

            this.width = width;
            this.height = height;

            this.file = File.createTempFile(WWIO.DELETE_ON_EXIT_PREFIX, WWIO.makeSuffixForMimeType(mimeType));
        }

        @Override
        public int getWidth()
        {
            return this.width;
        }

        @Override
        public int getHeight()
        {
            return this.height;
        }

        @Override
        public String getPath()
        {
            return this.file.getPath();
        }

        public File getFile()
        {
            return this.file;
        }
    }

    @Override
    public BufferedImage composeImageForSector(Sector sector, int canvasWidth, int canvasHeight, double aspectRatio,
        int levelNumber, String mimeType, boolean abortOnError, BufferedImage image, int timeout) throws Exception
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ComposeImageTile tile =
            new ComposeImageTile(sector, mimeType, this.getLevels().getLastLevel(), canvasWidth, canvasHeight);
        try
        {
            if (image == null)
                image = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);

            downloadImage(tile, mimeType, timeout);
            Thread.sleep(1); // generates InterruptedException if thread has been interupted

            BufferedImage tileImage = ImageIO.read(tile.getFile());
            Thread.sleep(1); // generates InterruptedException if thread has been interupted

            ImageUtil.mergeImage(sector, tile.getSector(), aspectRatio, tileImage, image);
            Thread.sleep(1); // generates InterruptedException if thread has been interupted

            this.firePropertyChange(AVKey.PROGRESS, 0d, 1d);
        }
        catch (InterruptedIOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            if (abortOnError)
                throw e;

            String message = Logging.getMessage("generic.ExceptionWhileRequestingImage", tile.getPath());
            Logging.logger().log(java.util.logging.Level.WARNING, message, e);
        }

        return image;
    }

    //**************************************************************//
    //********************  Configuration File  ********************//
    //**************************************************************//

    /**
     * Appends WMS tiled image layer configuration elements to the superclass configuration document.
     *
     * @param params configuration parameters describing this WMS tiled image layer.
     *
     * @return a WMS tiled image layer configuration document.
     */
    protected Document createConfigurationDocument(AVList params)
    {

        Document doc = super.createConfigurationDocument(params);
        if (doc == null || doc.getDocumentElement() == null)
            return doc;

        LayerConfiguration.createWMSTiledImageLayerElements(params, doc.getDocumentElement());

        return doc;
    }

    //**************************************************************//
    //********************  Restorable Support  ********************//
    //**************************************************************//

    protected void doGetRestorableStateForAVPair(String key, Object value,
        RestorableSupport rs, RestorableSupport.StateObject context)
    {
        if (value instanceof URLBuilder)
        {
            rs.addStateValueAsString(context, "wms.Version", ((URLBuilder) value).wmsVersion);
            rs.addStateValueAsString(context, "wms.Crs", ((URLBuilder) value).crs);
        }
        else
        {
            super.doGetRestorableStateForAVPair(key, value, rs, context);
        }
    }

    protected static AVList wmsRestorableStateToParams(String stateInXml)
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
        wmsRestoreStateToParams(rs, null, params);
        return params;
    }

    protected static void wmsRestoreStateToParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params)
    {
        // Invoke the BasicTiledImageLayer functionality.
        restoreStateForParams(rs, context, params);
        // Parse any legacy WMSTiledImageLayer state values.
        legacyWmsRestoreStateToParams(rs, context, params);

        String s = rs.getStateValueAsString(context, AVKey.IMAGE_FORMAT);
        if (s != null)
            params.setValue(AVKey.IMAGE_FORMAT, s);

        s = rs.getStateValueAsString(context, AVKey.TITLE);
        if (s != null)
            params.setValue(AVKey.TITLE, s);

        s = rs.getStateValueAsString(context, AVKey.DISPLAY_NAME);
        if (s != null)
            params.setValue(AVKey.DISPLAY_NAME, s);

        RestorableSupport.adjustTitleAndDisplayName(params);

        s = rs.getStateValueAsString(context, AVKey.LAYER_NAMES);
        if (s != null)
            params.setValue(AVKey.LAYER_NAMES, s);

        s = rs.getStateValueAsString(context, AVKey.STYLE_NAMES);
        if (s != null)
            params.setValue(AVKey.STYLE_NAMES, s);

        s = rs.getStateValueAsString(context, "wms.Version");
        if (s != null)
            params.setValue(AVKey.WMS_VERSION, s);
        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params));
    }

    protected static void legacyWmsRestoreStateToParams(RestorableSupport rs, RestorableSupport.StateObject context,
        AVList params)
    {
        // WMSTiledImageLayer has historically used a different format for storing LatLon and Sector properties
        // in the restorable state XML documents. Although WMSTiledImageLayer no longer writes these properties,
        // we must provide support for reading them here.
        Double lat = rs.getStateValueAsDouble(context, AVKey.LEVEL_ZERO_TILE_DELTA + ".Latitude");
        Double lon = rs.getStateValueAsDouble(context, AVKey.LEVEL_ZERO_TILE_DELTA + ".Longitude");
        if (lat != null && lon != null)
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(lat, lon));

        Double minLat = rs.getStateValueAsDouble(context, AVKey.SECTOR + ".MinLatitude");
        Double minLon = rs.getStateValueAsDouble(context, AVKey.SECTOR + ".MinLongitude");
        Double maxLat = rs.getStateValueAsDouble(context, AVKey.SECTOR + ".MaxLatitude");
        Double maxLon = rs.getStateValueAsDouble(context, AVKey.SECTOR + ".MaxLongitude");
        if (minLat != null && minLon != null && maxLat != null && maxLon != null)
            params.setValue(AVKey.SECTOR, Sector.fromDegrees(minLat, maxLat, minLon, maxLon));
    }
}
