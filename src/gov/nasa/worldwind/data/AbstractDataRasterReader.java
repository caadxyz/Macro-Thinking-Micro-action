/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.*;

/**
 * @author dcollins
 * @version $Id: AbstractDataRasterReader.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public abstract class AbstractDataRasterReader implements DataRasterReader
{
    private final String description;
    private final String[] mimeTypes;
    private final String[] suffixes;

    public AbstractDataRasterReader(String description, String[] mimeTypes, String[] suffixes)
    {
        this.description = description;
        this.mimeTypes = copyOf(mimeTypes);
        this.suffixes = copyOf(suffixes);
    }

    public AbstractDataRasterReader(String[] mimeTypes, String[] suffixes)
    {
        this.description = descriptionFromSuffixes(suffixes);
        this.mimeTypes = copyOf(mimeTypes);
        this.suffixes = copyOf(suffixes);
    }

    protected AbstractDataRasterReader(String description)
    {
        this.description = description;
        this.mimeTypes = new String[0];
        this.suffixes = new String[0];
    }

    public String getDescription()
    {
        return this.description;
    }

    public String[] getMimeTypes()
    {
        String[] copy = new String[mimeTypes.length];
        System.arraycopy(mimeTypes, 0, copy, 0, mimeTypes.length);
        return copy;
    }

    public String[] getSuffixes()
    {
        String[] copy = new String[suffixes.length];
        System.arraycopy(suffixes, 0, copy, 0, suffixes.length);
        return copy;
    }

    public boolean canRead(DataSource source)
    {
        if (source == null || source.getSource() == null)
            return false;

        //noinspection SimplifiableIfStatement
        if (!this.canReadSuffix(source))
            return false;

        return this.doCanRead(source);
    }

    protected boolean canReadSuffix(DataSource source)
    {
        // If the source has no path, we cannot return failure, so return that the test passed.
        String path = pathFor(source);
        if (path == null)
            return true;

        // If the source has a suffix, then we return success if this reader supports the suffix.
        String pathSuffix = WWIO.getSuffix(path);
        boolean matchesAny = false;
        for (String suffix : suffixes)
        {
            if (suffix.equalsIgnoreCase(pathSuffix))
            {
                matchesAny = true;
                break;
            }
        }
        return matchesAny;
    }

    public DataRaster[] read(DataSource source) throws java.io.IOException
    {
        if (!this.canRead(source))
        {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        return this.doRead(source);
    }

    public void readMetadata(DataSource source, AVList values) throws java.io.IOException
    {
        if (!this.canRead(source))
        {
            String message = Logging.getMessage("DataRaster.CannotRead", source);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }
        if (values == null)
        {
            String message = Logging.getMessage("nullValue.AVListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.doReadMetadata(source, values);

        String message = this.validateMetadata(source, values);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }
    }

    protected String validateMetadata(DataSource source, AVList values)
    {
        StringBuilder sb = new StringBuilder();

        Object o = values.getValue(AVKey.WIDTH);
        if (o == null || !(o instanceof Integer))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoSizeSpecified", source));

        o = values.getValue(AVKey.HEIGHT);
        if (o == null || !(o instanceof Integer))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoSizeSpecified", source));

        o = values.getValue(AVKey.SECTOR);
        if (o == null || !(o instanceof Sector))
            sb.append(sb.length() > 0 ? ", " : "").append(Logging.getMessage("WorldFile.NoSectorSpecified", source));

        if (sb.length() == 0)
            return null;

        return sb.toString();
    }

    protected abstract boolean doCanRead(DataSource source);

    protected abstract DataRaster[] doRead(DataSource source) throws java.io.IOException;

    protected abstract void doReadMetadata(DataSource source, AVList values) throws java.io.IOException;

    //**************************************************************//
    //********************  Utilities  *****************************//
    //**************************************************************//

    protected static String pathFor(DataSource source)
    {
        if (source.getSource() instanceof String)
        {
            return (String) source.getSource();
        }
        else if (source.getSource() instanceof java.io.File)
        {
            return ((java.io.File) source.getSource()).getPath();
        }
        else if (source.getSource() instanceof java.net.URI)
        {
            return source.getSource().toString();
        }
        else if (source.getSource() instanceof java.net.URL)
        {
            return source.getSource().toString();
        }

        return null;
    }

    private static String[] copyOf(String[] array)
    {
        String[] copy = new String[array.length];
        for (int i = 0; i < array.length; i++)
            copy[i] = array[i].toLowerCase();
        return copy;
    }

    private static String descriptionFromSuffixes(String[] suffixes)
    {
        StringBuilder sb = new StringBuilder();
        for (String suffix : suffixes)
        {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("*.").append(suffix.toLowerCase());
        }
        return sb.toString();
    }
}
