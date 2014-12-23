/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

/**
 * @author dcollins
 * @version $Id: WWDotNetLayerSetWriter.java 7016 2008-10-13 17:58:16Z dcollins $
 */
public class WWDotNetLayerSetWriter extends AbstractDataDescriptorWriter
{
    public static final String MIME_TYPE = "text/xml";

    public WWDotNetLayerSetWriter()
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
    
    protected void doWrite(java.io.Writer writer, DataDescriptor descriptor) throws java.io.IOException
    {
        String name = "Unnamed Data Set", fileExt = "";
        Sector sector = Sector.FULL_SPHERE;
        LatLon tileDelta = LatLon.ZERO;
        Integer numLevels = 0, tileWidth = 0, tileHeight = 0;
        java.io.File permanentDirectory = null;

        String s = descriptor.getName();
        if (s != null)
            name = s;

        s = descriptor.getStringValue(AVKey.FORMAT_SUFFIX);
        if (s != null)
            fileExt = s;

        Object o = descriptor.getValue(AVKey.SECTOR);
        if (o != null && o instanceof Sector)
            sector = (Sector) o;

        o = descriptor.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (o != null && o instanceof LatLon)
            tileDelta = (LatLon) o;

        o = descriptor.getValue(AVKey.NUM_LEVELS);
        if (o != null && o instanceof Integer)
            numLevels = (Integer) o;

        o = descriptor.getValue(AVKey.TILE_WIDTH);
        if (o != null && o instanceof Integer)
            tileWidth = (Integer) o;

        o = descriptor.getValue(AVKey.TILE_HEIGHT);
        if (o != null && o instanceof Integer)
            tileHeight = (Integer) o;

        s = this.getPathValue(descriptor, AVKey.WORLD_WIND_DOT_NET_PERMANENT_DIRECTORY);
        if (s != null)
            permanentDirectory = new java.io.File(s);
        if (permanentDirectory == null)
        {
            String path = "";

            s = this.getPathValue(descriptor, AVKey.FILE_STORE_LOCATION);
            if (s != null)
                path += s;

            s = this.getPathValue(descriptor, AVKey.DATA_CACHE_NAME);
                path += s;

            permanentDirectory = new java.io.File(path);
        }

        java.io.PrintWriter out = new java.io.PrintWriter(writer);
        out.println("<?xml version=\"1.0\"?>");
        out.print("<LayerSet Name=\"" + name + "\" ShowOnlyOneLayer=\"false\" ShowAtStartup=\"true\"");
        out.print(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println(" xsi:noNamespaceSchemaLocation=\"LayerSet.xsd\">");
        out.println("    <QuadTileSet ShowAtStartup=\"true\">");
        out.print("          <Name>");
        out.print(               name);
        out.println(        "</Name>");
        out.println("        <DistanceAboveSurface>0</DistanceAboveSurface>");
        out.println("        <BoundingBox>");
        out.print("            <North><Value>");
        out.print(                 sector.getMaxLatitude().degrees);
        out.println(          "</Value></North>");
        out.print("            <South><Value>");
        out.print(                 sector.getMinLatitude().degrees);
        out.println(          "</Value></South>");
        out.print("            <West><Value>");
        out.print(                 sector.getMinLongitude().degrees);
        out.println(          "</Value></West>");
        out.print("            <East><Value>");
        out.print(                 sector.getMaxLongitude().degrees);
        out.println(          "</Value></East>");
        out.println("        </BoundingBox>");
        out.println("        <Opacity>255</Opacity>");
        out.println("        <TerrainMapped>true</TerrainMapped>");
        out.println("        <RenderStruts>false</RenderStruts>");
        out.println("        <ImageAccessor>");
        out.print("            <LevelZeroTileSizeDegrees>");
        out.print(                 tileDelta.getLatitude().degrees);
        out.println(          "</LevelZeroTileSizeDegrees>");
        out.print("            <NumberLevels>");
        out.print(                 numLevels);
        out.println(          "</NumberLevels>");
        out.print("            <TextureSizePixels>");
        out.print(                 Math.max(tileWidth, tileHeight));
        out.println(          "</TextureSizePixels>");
        out.print("            <ImageFileExtension>");
        out.print(                 stripLeadingPeriod(fileExt));
        out.println(          "</ImageFileExtension>");
        out.print("            <PermanentDirectory>");
        out.print(                 permanentDirectory.getAbsolutePath());
        out.println(          "</PermanentDirectory>");
        out.println("        </ImageAccessor>");
        out.println("    </QuadTileSet>");
        out.println("</LayerSet>");
        out.flush();
    }

    private String getPathValue(AVList avList, String key)
    {
        String path = null;

        Object o = avList.getValue(key);
        if (o != null && o instanceof java.io.File)
            path = ((java.io.File) o).getPath();
        else if (o != null && o instanceof String)
            path = (String) o;

        return path;
    }

    private static String stripLeadingPeriod(String s)
    {
        if (s.startsWith("."))
            return s.substring(Math.min(1, s.length()), s.length());
        return s;
    }
}
