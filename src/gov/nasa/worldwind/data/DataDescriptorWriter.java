/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

/**
 * @author dcollins
 * @version $Id: DataDescriptorWriter.java 7008 2008-10-11 00:58:57Z dcollins $
 */
public interface DataDescriptorWriter
{
    Object getDestination();

    String getMimeType();

    boolean matchesMimeType(String mimeType);

    void setDestination(Object destination);

    void write(DataDescriptor descriptor) throws java.io.IOException;
}
