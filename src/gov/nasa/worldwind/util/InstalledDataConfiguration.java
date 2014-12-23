/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.LayerConfiguration;
import gov.nasa.worldwind.terrain.ElevationModelConfiguration;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;

/**
 * An utility class for transforming an installed data "data descriptor" configuration document to a standard Layer or
 * ElevationModel configuration document.
 *
 * @author dcollins
 * @version $Id: InstalledDataConfiguration.java 11382 2009-06-01 18:37:43Z dcollins $
 */
public class InstalledDataConfiguration
{
    /**
     * Returns true if a specified DOM document should be accepted as a"data descriptor" configuration document, and
     * false otherwise.
     *
     * @param domElement the DOM document in question.
     *
     * @return true if the document is a "data descriptor" configuration document; false otherwise.
     *
     * @throws IllegalArgumentException if document is null.
     */
    public static boolean isInstalledDataDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Element[] els = WWXML.getElements(domElement, "/dataDescriptor", null);

        return els != null && els.length > 0;
    }

    /**
     * Creates a standard layer or elevation model configuration backed by a specified "data descriptor" document.
     *
     * @param domElement backing DOM document.
     *
     * @return Layer or ElevationModel document, or null if the "data descriptor" document cannot be transformed to a
     *         standard document.
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

        Document newDoc = transformInstalledDataDocument(domElement);
        if (newDoc == null || newDoc.getDocumentElement() == null)
            return null;

        Element newDomElement = newDoc.getDocumentElement();

        if (LayerConfiguration.isLayerDocument(newDomElement))
        {
            return new LayerConfiguration(newDomElement);
        }
        else if (ElevationModelConfiguration.isElevationModelDocument(newDomElement))
        {
            return new ElevationModelConfiguration(newDomElement);
        }
        else
        {
            return new BasicDataConfiguration(newDomElement);
        }
    }

    /**
     * Transforms a "data descriptor" document to a standard layer or elevation model configuration document, depending
     * on the contents of the {@link org.w3c.dom.Element}.
     *
     * @param domElement DataDescriptor document to transform.
     *
     * @return standard Layer or ElevationModel dsocument, or null if the DataDescriptor cannot be transformed to a
     *         standard document.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static Document transformInstalledDataDocument(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        Element[] els = WWXML.getElements(domElement, "/dataDescriptor/property[@name=\"dataSet\"]", xpath);
        if (els == null || els.length == 0)
            return null;

        // Ignore all but the first dataSet element.
        Document outDoc = WWXML.createDocumentBuilder(true).newDocument();
        transformDataSet(els[0], outDoc, xpath);

        return outDoc;
    }

    protected static void transformDataSet(Element context, Document outDoc, XPath xpath)
    {
        String s = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.DataType\"]", xpath);

        // ElevationModel output.
        if (s != null && s.equals("gov.nasa.worldwind.avkey.TiledElevations"))
        {
            Element el = outDoc.createElement("ElevationModel");
            el.setAttribute("version", "1");
            outDoc.appendChild(el);
            transformCommonElements(context, el, xpath);
            transformElevationModelElements(context, el, xpath);
        }
        // Default to Layer output.
        else
        {
            Element el = outDoc.createElement("Layer");
            el.setAttribute("version", "1");
            el.setAttribute("layerType", "TiledImageLayer");
            outDoc.appendChild(el);
            transformCommonElements(context, el, xpath);
            transformLayerElements(context, el, xpath);
        }
    }

    protected static void transformCommonElements(Element context, Element outElem, XPath xpath)
    {
        // Title and cache name properties.
        String s = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.DatasetNameKey\"]", xpath);
        if (s != null && s.length() != 0)
            WWXML.appendText(outElem, "DisplayName", s);

        // DataDescriptor documents always describe an offline pyramid of tiled imagery or elevations in the file
        // store.
        Element el = WWXML.appendElementPath(outElem, "Service");
        el.setAttribute("serviceName", "Offline");

        s = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.DatasetNameKey\"]", xpath);
        if (s != null && s.length() != 0)
            WWXML.appendText(outElem, "DatasetName", s);

        // Image format properties.
        s = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.FormatSuffixKey\"]", xpath);
        if (s != null && s.length() != 0)
            WWXML.appendText(outElem, "FormatSuffix", s);

        // Tile structure properties.
        Integer numLevels = WWXML.getInteger(context, "property[@name=\"gov.nasa.worldwind.avkey.NumLevels\"]", xpath);
        Integer numEmptyLevels = WWXML.getInteger(context,
            "property[@name=\"gov.nasa.worldwind.avkey.NumEmptyLevels\"]", xpath);
        if (numLevels != null)
        {
            el = WWXML.appendElementPath(outElem, "NumLevels");
            el.setAttribute("count", Integer.toString(numLevels));
            el.setAttribute("numEmpty", Integer.toString((numEmptyLevels != null) ? numEmptyLevels : 0));
        }

        // Note the upper case K in "avKey". This was a typo in AVKey.SECTOR, and is intentionally reproduced here.
        Sector sector = getDataDescriptorSector(context, "property[@name=\"gov.nasa.worldwind.avKey.Sector\"]", xpath);
        if (sector != null)
            WWXML.appendSector(outElem, "Sector", sector);

        LatLon ll = getDataDescriptorLatLon(context, "property[@name=\"gov.nasa.worldwind.avkey.TileOrigin\"]", xpath);
        if (ll != null)
            WWXML.appendLatLon(outElem, "TileOrigin/LatLon", ll);

        ll = getDataDescriptorLatLon(context, "property[@name=\"gov.nasa.worldwind.avkey.LevelZeroTileDelta\"]", xpath);
        if (ll != null)
            WWXML.appendLatLon(outElem, "LevelZeroTileDelta/LatLon", ll);

        Integer tileWidth = WWXML.getInteger(context, "property[@name=\"gov.nasa.worldwind.avkey.TileWidthKey\"]",
            xpath);
        Integer tileHeight = WWXML.getInteger(context, "property[@name=\"gov.nasa.worldwind.avkey.TileHeightKey\"]",
            xpath);
        if (tileWidth != null && tileHeight != null)
        {
            el = WWXML.appendElementPath(outElem, "TileSize/Dimension");
            el.setAttribute("width", Integer.toString(tileWidth));
            el.setAttribute("height", Integer.toString(tileHeight));
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected static void transformLayerElements(Element context, Element outElem, XPath xpath)
    {
        // DataDescriptor documents always describe an offline pyramid of tiled imagery or elevations in the file
        // store. Therefore we can safely assume that network retrieval should be disabled. Also, because we know
        // nothing about the nature of the imagery, it's best to enable mipmapping and transparent texture by default.
        // This will almost always result in the correct visual effect. In those edge cases that it does not, the
        // caller may always interpose the desired property changes.

        WWXML.appendBoolean(outElem, "NetworkRetrievalEnabled", false);
        WWXML.appendBoolean(outElem, "UseMipMaps", true);
        WWXML.appendBoolean(outElem, "UseTransparentTextures", true);
    }

    protected static void transformElevationModelElements(Element context, Element outElem,
        XPath xpath)
    {
        // Image format properties.
        Element el = WWXML.appendElementPath(outElem, "DataType");

        String pixelType = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.PixelType\"]", xpath);
        if (pixelType != null && pixelType.length() != 0)
            el.setAttribute("type", WWXML.dataTypeAsText(pixelType));

        String byteOrder = WWXML.getText(context, "property[@name=\"gov.nasa.worldwind.avkey.ByteOrder\"]", xpath);
        if (byteOrder != null && byteOrder.length() != 0)
            el.setAttribute("byteOrder", WWXML.byteOrderAsText(byteOrder));

        // Data descriptor files are written with the property "gov.nasa.worldwind.avkey.MissingDataValue". But it
        // means the value that denotes a missing data point, and not the value that replaces missing values.
        // Translate that key here to MissingDataSignal, so it is properly understood by the World Wind API
        // (esp. BasicElevationModel).
        Double d = WWXML.getDouble(context, "property[@name=\"gov.nasa.worldwind.avkey.MissingDataValue\"]", xpath);
        if (d != null)
        {
            el = WWXML.appendElementPath(outElem, "MissingData");
            el.setAttribute("signal", Double.toString(d));
        }

        // DataDescriptor documents always describe an offline pyramid of tiled imagery or elevations in the file
        // store. Therefore we can safely assume that network retrieval should be disabled.

        // Optional boolean properties.
        WWXML.appendBoolean(outElem, "NetworkRetrievalEnabled", false);
    }

    protected static LatLon getDataDescriptorLatLon(Element context, String path, XPath xpath)
    {
        Element el = (path == null) ? context : WWXML.getElement(context, path, xpath);
        if (el == null)
            return null;

        Double latDegrees = WWXML.getDouble(el, "property[@name=\"latitudeDegrees\"]", xpath);
        Double lonDegrees = WWXML.getDouble(el, "property[@name=\"longitudeDegrees\"]", xpath);
        if (latDegrees == null || lonDegrees == null)
            return null;

        return LatLon.fromDegrees(latDegrees, lonDegrees);
    }

    protected static Sector getDataDescriptorSector(Element context, String path, XPath xpath)
    {
        Element el = (path == null) ? context : WWXML.getElement(context, path, xpath);
        if (el == null)
            return null;

        Double minLatDegrees = WWXML.getDouble(el, "property[@name=\"minLatitudeDegrees\"]", xpath);
        Double maxLatDegrees = WWXML.getDouble(el, "property[@name=\"maxLatitudeDegrees\"]", xpath);
        Double minLonDegrees = WWXML.getDouble(el, "property[@name=\"minLongitudeDegrees\"]", xpath);
        Double maxLonDegrees = WWXML.getDouble(el, "property[@name=\"maxLongitudeDegrees\"]", xpath);

        if (minLatDegrees == null || maxLatDegrees == null || minLonDegrees == null || maxLonDegrees == null)
            return null;

        return Sector.fromDegrees(minLatDegrees, maxLatDegrees, minLonDegrees, maxLonDegrees);
    }
}
