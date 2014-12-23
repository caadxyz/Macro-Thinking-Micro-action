/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Sector;

/**
 * @author tag
 * @version $Id: DecisionTree.java 5895 2008-08-09 03:35:44Z tgaskins $
 */
public class DecisionTree<T, C> // T = type being managed. C = traversal context
{
    public interface Controller<T, C>
    {
        public boolean isVisible(T o, C context);
        public boolean isTerminal(T o, C context);
        public T[] split(T o, C context);
    }

    protected Controller<T, C> controller;

    public DecisionTree(Controller<T, C> controller)
    {
        this.controller = controller;
    }

    public void traverse(T o, C context)
    {
        if (!this.controller.isVisible(o, context))
            return;

        if (this.controller.isTerminal(o, context))
            return;

        for (T child : this.controller.split(o, context))
            this.traverse(child, context);
    }

    public static void main(String[] args)
    {
        DecisionTree<Sector, Sector> tree = new DecisionTree<Sector, Sector>(new Controller<Sector, Sector>()
        {
            public boolean isVisible(Sector s, Sector context)
            {
                return s.intersects(context);
            }

            public boolean isTerminal(Sector s, Sector context)
            {
                return s.getDeltaLat().degrees < 1d;
            }

            public Sector[] split(Sector s, Sector context)
            {
                return s.subdivide();
            }
        });

        int N = 10000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++)
            tree.traverse(Sector.FULL_SPHERE, Sector.fromDegrees(0, 40, 0, 40));
        System.out.println((System.currentTimeMillis() - start) / (double) N + " ms");
    }
}
