/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.WWObjectImpl;

/**
 * @author dcollins
 * @version $Id: BasicDataDescriptor.java 7630 2008-11-14 21:35:01Z dcollins $
 */
public class BasicDataDescriptor extends WWObjectImpl implements DataDescriptor
{
    private java.io.File fileStoreLocation;

    public BasicDataDescriptor(AVList parameters)
    {
        if (parameters == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.setValues(parameters);
    }

    public BasicDataDescriptor()
    {
    }

    public java.io.File getFileStoreLocation()
    {
        return this.fileStoreLocation;
    }

    public void setFileStoreLocation(java.io.File location)
    {
        this.fileStoreLocation = location;
    }

    public String getFileStorePath()
    {
        return this.getStringValue(AVKey.DATA_CACHE_NAME);
    }

    public void setFileStorePath(String path)
    {
        this.setValue(AVKey.DATA_CACHE_NAME, path);
    }

    public boolean isInstalled()
    {
        Object o = this.getValue(AVKey.INSTALLED);
        return (o != null && o instanceof Boolean) && (Boolean) o;
    }

    public void setInstalled(boolean isInstalled)
    {
        this.setValue(AVKey.INSTALLED, isInstalled);
    }

    public String getName()
    {
        return this.getStringValue(AVKey.DATASET_NAME);
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DATASET_NAME, name);
    }

    public String getType()
    {
        return this.getStringValue(AVKey.DATA_TYPE);
    }

    public void setType(String dataType)
    {
        this.setValue(AVKey.DATA_TYPE, dataType);
    }
}
