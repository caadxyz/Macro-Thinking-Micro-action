/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.WWObject;

/**
 * @author dcollins
 * @version $Id: DataSource.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public interface DataSource extends WWObject
{
    Object getSource();
}
