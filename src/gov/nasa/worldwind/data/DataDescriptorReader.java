/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

/**
 * @author dcollins
 * @version $Id: DataDescriptorReader.java 7008 2008-10-11 00:58:57Z dcollins $
 */
public interface DataDescriptorReader
{
    Object getSource();

    void setSource(Object source);

    String getMimeType();

    boolean matchesMimeType(String mimeType);

    boolean canRead() throws java.io.IOException;

    DataDescriptor read() throws java.io.IOException;
}
