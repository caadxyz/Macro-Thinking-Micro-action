/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.*;

import java.io.*;

/**
 * @author dcollins
 * @version $Id: BasicDataDescriptorReader.java 9495 2009-03-18 19:01:38Z dcollins $
 */
public class BasicDataDescriptorReader extends AbstractDataDescriptorReader
{
    public static final String MIME_TYPE = "text/xml";
    public static final String VERSION = "1";

    private static final String DOCUMENT_ELEMENT_TAG_NAME = "dataDescriptor";
    private static final String PROPERTY_TAG_NAME = "property";
    
    public BasicDataDescriptorReader()
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

    protected boolean doCanReadStream(Reader reader) throws IOException
    {
        RestorableSupport rs;
        try
        {
            rs = this.parseDocument(reader);
        }
        catch (Exception e)
        {
            rs = null;
        }

        if (rs == null)
            return false;

        String message = this.validate(rs);
        return message == null;
    }

    protected DataDescriptor doRead(Reader reader) throws IOException
    {
        RestorableSupport rs = this.parseDocument(reader);

        String message = this.validate(rs);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DataDescriptor descriptor = new BasicDataDescriptor();

        RestorableSupport.StateObject so = rs.getStateObject("dataSet");
        if (so != null)
        {
            RestorableSupport.StateObject[] avpairs = rs.getAllStateObjects(so);
            if (avpairs != null)
            {
                for (RestorableSupport.StateObject avp : avpairs)
                {
                    if (avp != null)
                    {
                        String key = avp.getName();
                        Object value = this.parseStateValue(rs, avp);
                        key = this.parseStateKey(key);
                        descriptor.setValue(key, value);
                    }
                }
            }
        }

        return descriptor;
    }

    protected RestorableSupport parseDocument(Reader reader) throws IOException
    {
        StringBuilder sb = new StringBuilder();

        int len;
        int bufferSize = 4096; // 4k
        char[] buffer = new char[bufferSize];
        while ((len = reader.read(buffer, 0, bufferSize)) != -1)
            sb.append(buffer, 0, len);

        RestorableSupport rs = RestorableSupport.parse(sb.toString());
        rs.setStateObjectTagName(PROPERTY_TAG_NAME);
        return rs;
    }

    protected String parseStateKey(String key)
    {
        // Data descriptor files are written with the property "gov.nasa.worldwind.avkey.MissingDataValue", which
        // now corresponds to the key MISSING_DATA_REPLACEMENT. Translate that key here to MISSING_DATA_SIGNAL, so it
        // will be properly understood by the World Wind API (esp. BasicElevationModel).
        if (AVKey.MISSING_DATA_REPLACEMENT.equals(key))
        {
            return AVKey.MISSING_DATA_SIGNAL;
        }

        return key;
    }

    protected Object parseStateValue(RestorableSupport rs, RestorableSupport.StateObject so)
    {
        String name = so.getName();

        // State value is an Integer.
        if (AVKey.NUM_EMPTY_LEVELS.equals(name)
            || AVKey.NUM_LEVELS.equals(name)
            || AVKey.TILE_WIDTH.equals(name)
            || AVKey.TILE_HEIGHT.equals(name))
        {
            return rs.getStateObjectAsInteger(so);
        }
        // State value is a Double.
        else if (AVKey.MISSING_DATA_REPLACEMENT.equals(name))
        {
            return rs.getStateObjectAsDouble(so);
        }
        // State value is a LatLon.
        else if (AVKey.LEVEL_ZERO_TILE_DELTA.equals(name)
            || AVKey.TILE_DELTA.equals(name)
            || AVKey.TILE_ORIGIN.equals(name))
        {
            return rs.getStateObjectAsLatLon(so);
        }
        // State value is a Sector.
        else if (AVKey.SECTOR.equals(name))
        {
            return rs.getStateObjectAsSector(so);
        }

        return so.getValue();
    }

    protected String validate(RestorableSupport rs)
    {
        String s = rs.getDocumentElementTagName();
        if (!DOCUMENT_ELEMENT_TAG_NAME.equals(s))
            return Logging.getMessage("generic.CannotParseInputStream");

        s = rs.getStateValueAsString(AVKey.VERSION);
        if (!VERSION.equals(s))
            return Logging.getMessage("generic.CannotParseInputStream");

        RestorableSupport.StateObject so = rs.getStateObject("dataSet");
        if (so == null)
            return Logging.getMessage("generic.CannotParseInputStream");

        s = rs.getStateValueAsString(so, AVKey.DATASET_NAME);
        if (s == null)
            return Logging.getMessage("generic.CannotParseInputStream");

        s = rs.getStateValueAsString(so, AVKey.DATA_TYPE);
        if (s == null)
            return Logging.getMessage("generic.CannotParseInputStream");

        return null;
    }
}
