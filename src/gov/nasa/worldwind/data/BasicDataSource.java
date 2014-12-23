/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: BasicDataSource.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public class BasicDataSource extends WWObjectImpl implements DataSource
{
    private Object source;

    public BasicDataSource(Object source)
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.source = source;
    }

    public Object getSource()
    {
        return this.source;
    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        BasicDataSource that = (BasicDataSource) o;
        return this.source.equals(that.source);
    }

    public int hashCode()
    {
        return this.source.hashCode();
    }

    public String toString()
    {
        return this.source.toString();
    }
}
