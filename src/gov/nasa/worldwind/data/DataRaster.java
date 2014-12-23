/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.geom.Sector;

/**
 * @author dcollins
 * @version $Id: DataRaster.java 11704 2009-06-17 20:28:39Z dcollins $
 */
public interface DataRaster
{
    int getWidth();

    int getHeight();

    Sector getSector();

    void drawOnCanvas(DataRaster canvas, Sector clipSector);

    void drawOnCanvas(DataRaster canvas);
}
