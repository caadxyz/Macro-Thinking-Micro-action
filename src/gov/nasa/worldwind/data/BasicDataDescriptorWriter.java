/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.avlist.AVKey;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * @author dcollins
 * @version $Id: BasicDataDescriptorWriter.java 7136 2008-10-22 21:46:07Z dcollins $
 */
public class BasicDataDescriptorWriter extends AbstractDataDescriptorWriter
{
    public static final String MIME_TYPE = "text/xml";
    public static final String VERSION = "1";

    private static final String DOCUMENT_ELEMENT_TAG_NAME = "dataDescriptor";
    private static final String PROPERTY_TAG_NAME = "property";

    public BasicDataDescriptorWriter()
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
        sb.append(Logging.getMessage("DataDescriptor.BasicDataDescriptor.Name"));
        sb.append("(*").append(WWIO.makeSuffixForMimeType(this.getMimeType())).append(")");
        return sb.toString();
    }
    
    protected void doWrite(Writer writer, DataDescriptor descriptor) throws IOException
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport(DOCUMENT_ELEMENT_TAG_NAME);
        rs.setStateObjectTagName(PROPERTY_TAG_NAME);

        rs.addStateValueAsString(AVKey.VERSION, VERSION);

        RestorableSupport.StateObject so = rs.addStateObject("dataSet");

        for (Map.Entry<String, Object> entry : descriptor.getEntries())
        {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Do not write the file-store-location. This is a runtime property, and is therefore not persistant.
            if (AVKey.FILE_STORE_LOCATION.equals(key))
                continue;

            // Do not write the data-cache-name. This is a runtime property, and is therefore not persistant.
            if (AVKey.DATA_CACHE_NAME.equals(key))
                continue;

            if (value instanceof Integer)
            {
                rs.addStateValueAsInteger(so, key, (Integer) value);
            }
            else if (value instanceof Double)
            {
                rs.addStateValueAsDouble(so, key, (Double) value);
            }
            else if (value instanceof Boolean)
            {
                rs.addStateValueAsBoolean(so, key, (Boolean) value);
            }
            else if (value instanceof Position)
            {
                rs.addStateValueAsPosition(so, key, (Position) value);
            }
            else if (value instanceof LatLon)
            {
                rs.addStateValueAsLatLon(so, key, (LatLon) value);
            }
            else if (value instanceof Sector)
            {
                rs.addStateValueAsSector(so, key, (Sector) value);
            }
            else if (value instanceof java.awt.Color)
            {
                rs.addStateValueAsColor(so, key, (java.awt.Color) value);
            }
            else
            {
                rs.addStateValueAsString(so, key, value.toString());
            }
        }

        String descriptorXml = rs.getStateAsXml();
        writer.write(descriptorXml);
        writer.flush();
    }
}
