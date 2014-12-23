/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVList;

/**
 * @author dcollins
 * @version $Id: DataRasterReader.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public interface DataRasterReader
{
    String getDescription();

    String[] getMimeTypes();

    String[] getSuffixes();

    boolean canRead(DataSource source);

    DataRaster[] read(DataSource source) throws java.io.IOException;

    void readMetadata(DataSource source, AVList values) throws java.io.IOException;
}