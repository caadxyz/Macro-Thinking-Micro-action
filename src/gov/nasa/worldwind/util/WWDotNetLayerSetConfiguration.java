/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.DataConfiguration;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;

/**
 * A utility class for transforming a World Wind .NET LayerSet document to a standard Layer configuration document.
 *
 * @author dcollins
 * @version $Id: WWDotNetLayerSetConfiguration.java 11390 2009-06-01 21:23:21Z dcollins $
 */
public class WWDotNetLayerSetConfiguration
{
    /**
     * Returns true if a specified document should be accepted as a World Wind .NET LayerSet configuration document, 
     * and false otherwise.
     *
     * @param domElement the document in question.
     *
     * @return true if the document is a LayerSet configuration document; false otherwise.
     *
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isLayerSetDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();
        Element[] elements = WWXML.getElements(domElement, "/LayerSet", xpath);

        return elements != null && elements.length > 0;
    }

    /**
     * Creates a standard layer configuration from a World Wind .NET LayerSet document.
     *
     * @param domElement backing document.
     *
     * @return Layer document, or null if the LayerSet document cannot be transformed to a standard document.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static DataConfiguration createDataConfig(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Document newDoc = transformLayerSetDocument(domElement);
        if (newDoc == null || newDoc.getDocumentElement() == null)
            return null;

        Element newDomElement = newDoc.getDocumentElement();
        return new LayerConfiguration(newDomElement);
    }

    /**
     * Transforms a World Wind .NET LayerSet document to a standard layer configuration document.
     *
     * @param domElement LayerSet document to transform.
     *
     * @return standard Layer document, or null if the LayerSet document cannot be transformed to a standard document.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static Document transformLayerSetDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        Element[] els = WWXML.getElements(domElement, "/LayerSet/QuadTileSet", xpath);
        if (els == null || els.length == 0)
            return null;

        // Ignore all but the first QuadTileSet element.
        Document outDoc = WWXML.createDocumentBuilder(true).newDocument();
        transformLayerSet(els[0], outDoc, xpath);

        return outDoc;
    }

    protected static void transformLayerSet(Element context, Document outDoc, XPath xpath)
    {
        Element el = outDoc.createElement("Layer");
        el.setAttribute("version", "1");
        el.setAttribute("layerType", "TiledImageLayer");
        outDoc.appendChild(el);

        transformQuadTileSet(context, el, xpath);
    }

    protected static void transformQuadTileSet(Element context, Element outElem, XPath xpath)
    {
        // Title and cache name properties.
        String s = WWXML.getText(context, "Name", xpath);
        if (s != null && s.length() != 0)
            WWXML.appendText(outElem, "DisplayName", s);

        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store.
        Element el = WWXML.appendElementPath(outElem, "Service");
        el.setAttribute("serviceName", "Offline");

        s = WWXML.getText(context, "Name", xpath);
        if (s != null && s.length() != 0)
            WWXML.appendText(outElem, "DatasetName", s);

        // Image format properties.
        s = WWXML.getText(context, "ImageAccessor/ImageFileExtension", xpath);
        if (s != null && s.length() != 0)
        {
            if (!s.startsWith("."))
                s = "." + s;
            WWXML.appendText(outElem, "FormatSuffix", s);
        }

        // Tile structure properties.
        Integer numLevels  = WWXML.getInteger(context, "ImageAccessor/NumberLevels", xpath);
        if (numLevels != null)
        {
            el = WWXML.appendElementPath(outElem, "NumLevels");
            el.setAttribute("count", Integer.toString(numLevels));
            el.setAttribute("numEmpty", Integer.toString(0));
        }

        Sector sector = getLayerSetSector(context, "BoundingBox", xpath);
        if (sector != null)
            WWXML.appendSector(outElem, "Sector", sector);

        WWXML.appendLatLon(outElem, "TileOrigin/LatLon", new LatLon(Angle.NEG90, Angle.NEG180));

        LatLon ll = getLayerSetLatLon(context, "ImageAccessor/LevelZeroTileSizeDegrees", xpath);
        if (ll != null)
            WWXML.appendLatLon(outElem, "LevelZeroTileDelta/LatLon", ll);

        Integer tileDimension = WWXML.getInteger(context, "ImageAccessor/TextureSizePixels", xpath);
        if (tileDimension != null)
        {
            el = WWXML.appendElementPath(outElem, "TileSize/Dimension");
            el.setAttribute("width", Integer.toString(tileDimension));
            el.setAttribute("height", Integer.toString(tileDimension));
        }

        // LayerSet documents always describe an offline pyramid of tiled imagery in the file store. Therefore we can
        // safely assume that network retrieval should be disabled. Also, because we know nothing about the nature of
        // the imagery, it's best to enable mipmapping and transparent texture by default. This will almost always
        // result in the correct visual effect. In those edge cases that it does not, the caller may always interpose
        // the desired property changes.
        
        WWXML.appendBoolean(outElem, "NetworkRetrievalEnabled", false);
        WWXML.appendBoolean(outElem, "UseMipMaps", true);
        WWXML.appendBoolean(outElem, "UseTransparentTextures", true);
    }

    protected static LatLon getLayerSetLatLon(Element context, String path, XPath xpath)
    {
        Double degrees = WWXML.getDouble(context, path, xpath);
        if (degrees == null)
            return null;

        return LatLon.fromDegrees(degrees, degrees);
    }

    protected static Sector getLayerSetSector(Element context, String path, XPath xpath)
    {
        Element el = (path == null) ? context : WWXML.getElement(context, path, xpath);
        if (el == null)
            return null;

        Double minLatDegrees = WWXML.getDouble(el, "South/Value", xpath);
        Double maxLatDegrees = WWXML.getDouble(el, "North/Value", xpath);
        Double minLonDegrees = WWXML.getDouble(el, "West/Value", xpath);
        Double maxLonDegrees = WWXML.getDouble(el, "East/Value", xpath);

        if (minLatDegrees == null || maxLatDegrees == null || minLonDegrees == null || maxLonDegrees == null)
            return null;

        return Sector.fromDegrees(minLatDegrees, maxLatDegrees, minLonDegrees, maxLonDegrees);
    }
}
