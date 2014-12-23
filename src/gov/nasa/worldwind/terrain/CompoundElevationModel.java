/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.util.Logging;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author tag
 * @version $Id: CompoundElevationModel.java 13006 2010-01-14 01:39:59Z tgaskins $
 */
public class CompoundElevationModel extends AbstractElevationModel
{
    protected CopyOnWriteArrayList<ElevationModel> elevationModels = new CopyOnWriteArrayList<ElevationModel>();

    public void addElevationModel(ElevationModel em)
    {
        if (em == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationModelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elevationModels.add(em);
    }

    public void addElevationModel(int index, ElevationModel em)
    {
        if (em == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationModelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elevationModels.add(index, em); // the list's add method will throw exception for invalid index
    }

    public void removeElevationModel(ElevationModel em)
    {
        if (em == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationModelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elevationModels.remove(em);
    }

    public void removeElevationModel(int index)
    {
        if (index < 0 || index >= this.elevationModels.size())
        {
            String msg = Logging.getMessage("generic.indexOutOfRange", index);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elevationModels.remove(index);
    }

    public void setElevationModel(int index, ElevationModel em)
    {
        if (em == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationModelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (index < 0 || index >= this.elevationModels.size())
        {
            String msg = Logging.getMessage("generic.indexOutOfRange", index);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.elevationModels.set(index, em);
    }

    public List<ElevationModel> getElevationModels()
    {
        return new ArrayList<ElevationModel>(this.elevationModels);
    }

    /**
     * Recursively specifies the time of the most recent dataset update for this compound model, and for each elevation
     * model contained within this compound model. Any cached data older than this time is invalid.
     *
     * @param expiryTime the expiry time of any cached data, expressed as a number of milliseconds beyond the epoch.
     *
     * @see System#currentTimeMillis() for a description of milliseconds beyond the epoch.
     */
    public void setExpiryTime(long expiryTime)
    {
        super.setExpiryTime(expiryTime);

        for (ElevationModel em : this.elevationModels)
        {
            em.setExpiryTime(expiryTime);
        }
    }

    public double getMaxElevation() // TODO: probably want to cache the min and max rather than always compute them
    {
        double max = -Double.MAX_VALUE;

        for (ElevationModel em : this.elevationModels)
        {
            double m = em.getMaxElevation();
            if (m > max)
                max = m;
        }

        return max == -Double.MAX_VALUE ? 0 : max;
    }

    public double getMinElevation()
    {
        double min = Double.MAX_VALUE;

        for (ElevationModel em : this.elevationModels)
        {
            double m = em.getMinElevation();
            if (m < min)
                min = m;
        }

        return min == Double.MAX_VALUE ? 0 : min;
    }

    public double[] getExtremeElevations(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double[] retVal = null;

        for (ElevationModel em : this.elevationModels)
        {
            double[] minmax = em.getExtremeElevations(latitude, longitude);
            if (retVal == null)
            {
                retVal = new double[] {minmax[0], minmax[1]};
            }
            else
            {
                if (minmax[0] < retVal[0])
                    retVal[0] = minmax[0];
                if (minmax[1] > retVal[1])
                    retVal[1] = minmax[1];
            }
        }

        return retVal == null ? new double[] {0, 0} : retVal;
    }

    public double[] getExtremeElevations(Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        double[] retVal = null;

        for (ElevationModel em : this.elevationModels)
        {
            int c = em.intersects(sector);
            if (c == 0) // elevation model fully contains the sector
                return em.getExtremeElevations(sector);

            if (c == 1) // elevation model intersects the sector
            {
                double[] minmax = em.getExtremeElevations(sector);
                if (retVal == null)
                {
                    retVal = new double[] {minmax[0], minmax[1]};
                }
                else
                {
                    if (minmax[0] < retVal[0])
                        retVal[0] = minmax[0];
                    if (minmax[1] > retVal[1])
                        retVal[1] = minmax[1];
                }
            }
        }

        return retVal == null ? new double[] {0, 0} : retVal;
    }

    public double getBestResolution(Sector sector)
    {
        double res = 0;

        for (ElevationModel em : this.elevationModels)
        {
            if (em.intersects(sector) < 0) // sector does not intersect elevation model
                continue;

            double r = em.getBestResolution(sector);
            if (r < res || res == 0)
                res = r;
        }

        return res != 0 ? res : Double.MAX_VALUE;
    }

    public double getDetailHint(Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Find the first elevation model intersecting the sector, starting with the hightest resolution. Return the
        // detail hint for that elevation model.
        for (int i = this.elevationModels.size() - 1; i >= 0; i--) // iterate from highest resolution to lowest
        {
            ElevationModel em = this.elevationModels.get(i);

            int c = em.intersects(sector);
            if (c != -1)
                return em.getDetailHint(sector);
        }

        // No elevation model intersects the sector. Return a default detail hint.
        return 0.0;
    }

    public int intersects(Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        boolean intersects = false;

        for (ElevationModel em : this.elevationModels)
        {
            int c = em.intersects(sector);
            if (c == 0) // sector fully contained in the elevation model. no need to test further
                return 0;

            if (c == 1)
                intersects = true;
        }

        return intersects ? 1 : -1;
    }

    public boolean contains(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (ElevationModel em : this.elevationModels)
        {
            if (em.contains(latitude, longitude))
                return true;
        }

        return false;
    }

    public double getUnmappedElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatLonIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Find the best elevation available at the specified (latitude, longitude) coordinates.
        Double value = this.missingDataFlag;
        for (int i = this.elevationModels.size() - 1; i >= 0; i--) // iterate from highest resolution to lowest
        {
            ElevationModel em = this.elevationModels.get(i);

            if (!em.contains(latitude, longitude))
                continue;

            double emValue = em.getUnmappedElevation(latitude, longitude);

            // Since we're working from highest resolution to lowest, we return the first value that's not a missing
            // data flag. Check this against the current ElevationModel's missing data flag, which might be different
            // from our own.
            if (emValue != em.getMissingDataSignal())
            {
                value = emValue;
                break;
            }
        }

        return value;
    }

    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (latlons == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer.length < latlons.size())
        {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Fill the buffer with ElevationModel contents from lowest resolution to highest, potentially overwriting
        // values at each step. ElevationModels are expected to leave the buffer untouched for locations outside its
        // coverage area.
        double resolutionAchieved = 0;
        for (ElevationModel em : this.elevationModels)
        {
            int c = em.intersects(sector);
            if (c < 0) // no intersection
                continue;

            double r = em.getElevations(sector, latlons, targetResolution, buffer);

            if (r < resolutionAchieved || resolutionAchieved == 0)
                resolutionAchieved = r;
        }

        return resolutionAchieved;
    }

    public void composeElevations(Sector sector, List<? extends LatLon> latlons, int tileWidth,
        double[] buffer) throws Exception
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (latlons == null)
        {
            String msg = Logging.getMessage("nullValue.LatLonListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.ElevationsBufferIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (buffer.length < latlons.size())
        {
            String msg = Logging.getMessage("ElevationModel.ElevationsBufferTooSmall", latlons.size());
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Fill the buffer with ElevationModel contents from back to front, potentially overwriting values at each step.
        // ElevationModels are expected to leave the buffer untouched when data is missing at a location.
        for (ElevationModel em : this.elevationModels)
        {
            int c = em.intersects(sector);
            if (c < 0) // no intersection
                continue;

            em.composeElevations(sector, latlons, tileWidth, buffer);
        }
    }

    public void setNetworkRetrievalEnabled(boolean networkRetrievalEnabled)
    {
        super.setNetworkRetrievalEnabled(networkRetrievalEnabled);

        for (ElevationModel em : this.elevationModels)
        {
            em.setNetworkRetrievalEnabled(networkRetrievalEnabled);
        }
    }
}
