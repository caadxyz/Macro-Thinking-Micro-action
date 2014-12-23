/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.markers.*;

import java.util.*;

/**
 * @author tag
 * @version $Id: SectorTree.java 12471 2009-08-17 23:40:14Z tgaskins $
 */
abstract public class SectorTree<T>
{
    protected int depth;
    protected int maxDepth;
    protected boolean addToAllLevels;
    protected Sector coverage;
    protected List<Sector> subCoverage;
    protected ArrayList<SectorTree<T>> children;
    protected List<T> items = new ArrayList<T>();

    protected abstract boolean intersects(Sector extent, T item);

    protected abstract SectorTree<T> createInstance(Sector extent, int depth, int maxDepth, boolean addToAllLevels);

    public SectorTree(Sector coverage, int depth, int maxDepth, boolean addToAllLevels) // TODO: arg check
    {
        this(coverage, depth, maxDepth, addToAllLevels, 2, 2);
    }

    public SectorTree(Sector coverage, int depth, int maxDepth, boolean addToAllLevels, int rows,
        int cols) // TODO: arg check
    {
        this.coverage = coverage;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.addToAllLevels = addToAllLevels;

        if (depth < maxDepth)
        {
            this.subCoverage = this.subdivide(this.coverage, rows, cols);
            children = new ArrayList<SectorTree<T>>(rows * cols);
            for (int i = 0; i < rows * cols; i++)
            {
                children.add(null);
            }
        }
    }

    synchronized public boolean hasItems()
    {
        if (this.items.size() > 0)
            return true;

        if (this.children == null || this.children.size() == 0)
            return false;

        for (SectorTree<T> child : this.children)
        {
            if (child != null && child.hasItems())
                return true;
        }

        return false;
    }

    synchronized public boolean add(T item)
    {
        boolean success = false;

        if (depth == maxDepth || addToAllLevels)
        {
            success = maxDepthAdd(item);
        }

        if (depth < maxDepth)
        {
            success = normalAdd(item) || success;
        }

        return success;
    }

    protected boolean normalAdd(T item)
    {
        boolean success = false;

        for (int i = 0; i < subCoverage.size() && !success; i++)
        {
            if (this.intersects(subCoverage.get(i), item))
            {
                if (children.get(i) == null)
                {
                    children.set(i, this.createInstance(subCoverage.get(i), depth + 1, maxDepth, addToAllLevels));
                }
                success = children.get(i).add(item);
            }
        }

        return success;
    }

    protected boolean maxDepthAdd(T item)
    {
        return this.intersects(coverage, item) && items.add(item);
    }

    synchronized public void remove(T item)
    {
        if (!this.intersects(this.coverage, item))
            return;
        
        this.items.remove(item);

        if (this.subCoverage == null || this.subCoverage.size() == 0)
            return;

        for (int i = 0; i < subCoverage.size(); i++)
        {
            if (this.intersects(subCoverage.get(i), item))
            {
                if (children.get(i) != null)
                    children.get(i).remove(item);
            }
        }
    }

    synchronized public Collection<T> getItems(LatLon location, int depth, Collection<T> itemsOut) // arg check
    {
        if (!this.contains(this.coverage, location))
        {
            return Collections.emptyList();
        }

        if (this.depth == depth)
        {
            return getAll(itemsOut);
        }
        else if (this.depth < depth)
        {
            for (int i = 0; i < subCoverage.size(); i++)
            {
                if (this.contains(subCoverage.get(i), location))
                {
                    SectorTree<T> child = this.children.get(i);
                    if (child != null)
                    {
                        return child.getItems(location, depth, itemsOut);
                    }
                    return Collections.emptyList();
                }
            }
        }
        return Collections.emptyList();
    }

    synchronized public Collection<T> getItems(Sector extent, Collection<T> itemsOut) // TODO: arg check
    {
        // No interaction with this grid
        if (!this.intersectsCoverage(extent, coverage))
        {
            return Collections.emptyList();
        }

        if (itemsOut == null)
            itemsOut = new ArrayList<T>();

        // Check the case where the input rect equals the coverage area
        if (extent.equals(coverage))
        {
            return getAll(itemsOut);
        }

        // Max Depth Check
        if (this.depth == this.maxDepth)
        {
            for (T item : items)
            {
                if (this.intersects(extent, item))
                    itemsOut.add(item);
            }
        }
        else if (this.depth < this.maxDepth)
        {
            for (int i = 0; i < subCoverage.size(); i++)
            {
                if (intersectsCoverage(extent, subCoverage.get(i)) && children.get(i) != null)
                    children.get(i).getItems(extent, itemsOut);
            }
        }

        return itemsOut;
    }

    synchronized public Collection<T> getAll(Collection<T> itemsOut)
    {
        if (addToAllLevels)
        {
            itemsOut.addAll(this.items);
        }
        else if (this.children == null)
        {
            itemsOut.addAll(this.items);
        }
        else
        {
            for (SectorTree<T> quad : children)
            {
                if (quad != null)
                {
                    return quad.getAll(itemsOut);
                }
            }
        }

        return itemsOut;
    }

    public int getDepth()
    {
        return this.depth;
    }

    public int getMaxDepth()
    {
        return this.maxDepth;
    }

    public Sector getCoverage()
    {
        return this.coverage;
    }

    protected boolean intersectsCoverage(Sector sector1, Sector sector2)
    {
        return sector1.intersects(sector2);
    }

    protected boolean contains(Sector extent, LatLon location)
    {
        return extent.contains(location);
    }

    protected ArrayList<Sector> subdivide(Sector extent, int rows, int cols)
    {
        ArrayList<Sector> children = new ArrayList<Sector>(rows * cols);

        double width = extent.getDeltaLon().degrees / cols;
        double height = extent.getDeltaLat().degrees / rows;

        for (int row = 0; row < rows; row++)
        {
            for (int col = 0; col < cols; col++)
            {
                children.add(Sector.fromDegrees(
                    extent.getMinLatitude().degrees + row * height,
                    extent.getMinLatitude().degrees + (row + 1) * height,
                    extent.getMinLongitude().degrees + col * width,
                    extent.getMinLongitude().degrees + (col + 1) * width));
            }
        }

        return children;
    }

    public static class MarkerTree extends SectorTree<Marker>
    {
        public MarkerTree(int maxDepth) // TODO: arg check
        {
            this(Sector.FULL_SPHERE, 0, maxDepth, false);
        }

        public MarkerTree(Sector coverage, int maxDepth)
        {
            super(coverage, 0, maxDepth, false);
        }

        protected MarkerTree(Sector coverage, int depth, int maxDepth, boolean addToAllLevels)
        {
            super(coverage, depth, maxDepth, addToAllLevels);
        }

        protected boolean intersects(Sector extent, Marker item)
        {
            return extent.contains(item.getPosition());
        }

        protected SectorTree<Marker> createInstance(Sector extent, int depth, int maxDepth,
            boolean addToAllLevels)
        {
            return new MarkerTree(extent, depth, maxDepth, addToAllLevels);
        }
    }
}
