/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.WWObject;

/**
 * @author dcollins
 * @version $Id: DataDescriptor.java 6817 2008-09-25 04:27:23Z dcollins $
 */
public interface DataDescriptor extends WWObject
{
    java.io.File getFileStoreLocation();

    void setFileStoreLocation(java.io.File location);

    String getFileStorePath();

    void setFileStorePath(String path);

    boolean isInstalled();

    void setInstalled(boolean isInstalled);

    String getName();

    void setName(String name);
    
    String getType();

    void setType(String type);
}
