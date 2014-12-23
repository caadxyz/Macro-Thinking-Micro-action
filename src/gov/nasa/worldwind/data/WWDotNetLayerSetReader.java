/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

// TODO: support for LayerSet.xsd format nested descriptors
/**
 * @author dcollins
 * @version $Id: WWDotNetLayerSetReader.java 7142 2008-10-22 22:52:17Z dcollins $
 */
public class WWDotNetLayerSetReader extends AbstractDataDescriptorReader
{
    public static final String MIME_TYPE = "text/xml";

    public WWDotNetLayerSetReader()
    {
    }

    public String getMimeType()
    {
        return MIME_TYPE;
    }

    public boolean matchesMimeType(String mimeType)
    {
        //noinspection SimplifiableIfStatement
        if (mimeType == null)
            return false;

        return mimeType.toLowerCase().contains(this.getMimeType());
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("DataDescriptor.WWDotNetLayerSet.Name"));
        sb.append("(*").append(WWIO.makeSuffixForMimeType(this.getMimeType())).append(")");
        return sb.toString();
    }

    protected boolean doCanRead(Object src) throws java.io.IOException
    {
        // We short circuit the process of probing the file for contents by requiring that the file name
        // or URL string end in ".xml".
        String path = null;
        if (src instanceof java.io.File)
            path = ((java.io.File) src).getPath();
        else if (src instanceof java.net.URL)
            path = ((java.net.URL) src).toExternalForm();

        String suffix = WWIO.makeSuffixForMimeType(this.getMimeType());
        //noinspection SimplifiableIfStatement
        if (path != null && !path.toLowerCase().endsWith(suffix))
            return false;

        return super.doCanRead(src);
    }

    protected boolean doCanReadStream(java.io.Reader reader) throws java.io.IOException
    {
        javax.xml.parsers.DocumentBuilderFactory docBuilderFactory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.xpath.XPathFactory pathFactory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath pathFinder = pathFactory.newXPath();

        try
        {
            javax.xml.parsers.DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(new org.xml.sax.InputSource(reader));

            Object o = pathFinder.evaluate("/LayerSet", doc.getDocumentElement(), javax.xml.xpath.XPathConstants.NODE);
            if (o != null && o instanceof org.w3c.dom.Node)
                return true;
        }
        catch (Exception e)
        {
            return false;
        }
        
        return false;
    }

    protected DataDescriptor doRead(java.io.Reader reader) throws java.io.IOException
    {
        DataDescriptor descriptor = new BasicDataDescriptor();
        this.readInputStream(reader, descriptor);
        return descriptor;
    }

    protected void readInputStream(java.io.Reader reader, DataDescriptor descriptor) throws java.io.IOException
    {
        javax.xml.parsers.DocumentBuilderFactory docBuilderFactory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();

        try
        {
            javax.xml.parsers.DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(new org.xml.sax.InputSource(reader));

            this.parseLayerSet(doc.getDocumentElement(), descriptor);
        }
        catch (javax.xml.parsers.ParserConfigurationException e)
        {
            String message = Logging.getMessage("generic.CannotParseInputStream", reader);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
        catch (org.xml.sax.SAXException e)
        {
            String message = Logging.getMessage("generic.CannotParseInputStream", reader);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected void parseLayerSet(org.w3c.dom.Node layerSetNode, DataDescriptor descriptor) throws java.io.IOException
    {
        javax.xml.xpath.XPathFactory pathFactory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath pathFinder = pathFactory.newXPath();

        try
        {
            Object layerSet = pathFinder.evaluate("/LayerSet", layerSetNode, javax.xml.xpath.XPathConstants.NODE);
            if (layerSet == null)
            {
                String message = Logging.getMessage("generic.CannotParseInputStream", layerSetNode);
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }

            // Mark this descriptor as a World Wind .NET LayerSet, and mark it as generic tiled imagery.
            descriptor.setValue(AVKey.WORLD_WIND_DOT_NET_LAYER_SET, Boolean.TRUE);
            descriptor.setType(AVKey.TILED_IMAGERY);

            String s = pathFinder.evaluate("/LayerSet/QuadTileSet/Name", layerSetNode);
            if (s != null && s.length() != 0)
            {
                descriptor.setValue(AVKey.DATASET_NAME, s);
            }

            Double south = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/BoundingBox/South/Value", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            Double north = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/BoundingBox/North/Value", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            Double west = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/BoundingBox/West/Value", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            Double east = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/BoundingBox/East/Value", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            if (south != null && north != null && west != null && east != null &&
                !Double.isNaN(south) && !Double.isNaN(north) && !Double.isNaN(west) && !Double.isNaN(east))
            {
                descriptor.setValue(AVKey.SECTOR, Sector.fromDegrees(south, north, west, east));
            }

            Double d = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/ImageAccessor/LevelZeroTileSizeDegrees",
                layerSetNode, javax.xml.xpath.XPathConstants.NUMBER);
            if (d != null && !Double.isNaN(d))
            {
                LatLon ll = LatLon.fromDegrees(d, d);
                descriptor.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);
            }

            d = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/ImageAccessor/NumberLevels", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            if (d != null && !Double.isNaN(d))
            {
                int i = d.intValue();
                descriptor.setValue(AVKey.NUM_LEVELS, i);
            }

            descriptor.setValue(AVKey.NUM_EMPTY_LEVELS, 0);

            d = (Double) pathFinder.evaluate("/LayerSet/QuadTileSet/ImageAccessor/TextureSizePixels", layerSetNode,
                javax.xml.xpath.XPathConstants.NUMBER);
            if (d != null && !Double.isNaN(d))
            {
                int i = d.intValue();
                descriptor.setValue(AVKey.TILE_WIDTH, i);
                descriptor.setValue(AVKey.TILE_HEIGHT, i);
            }

            s = pathFinder.evaluate("/LayerSet/QuadTileSet/ImageAccessor/ImageFileExtension", layerSetNode);
            if (s != null && s.length() != 0)
            {
                s = addLeadingPeriod(s);
                descriptor.setValue(AVKey.FORMAT_SUFFIX, s);
            }

            s = pathFinder.evaluate("/LayerSet/QuadTileSet/ImageAccessor/PermanentDirectory", layerSetNode);
            if (s != null && s.length() != 0)
            {
                descriptor.setValue(AVKey.WORLD_WIND_DOT_NET_PERMANENT_DIRECTORY, s);
            }
        }
        catch (javax.xml.xpath.XPathExpressionException e)
        {
            String message = Logging.getMessage("generic.CannotParseInputStream", layerSetNode);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    private static String addLeadingPeriod(String s)
    {
        if (!s.startsWith("."))
            s = "." + s;
        return s;
    }
}
