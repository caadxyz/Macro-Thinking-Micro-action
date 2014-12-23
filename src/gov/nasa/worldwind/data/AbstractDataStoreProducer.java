/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.WWObjectImpl;

/**
 * @author dcollins
 * @version $Id: AbstractDataStoreProducer.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public abstract class AbstractDataStoreProducer extends WWObjectImpl implements DataStoreProducer
{
    private AVList params;
    private java.util.List<DataSource> dataSourceList = new java.util.ArrayList<DataSource>();
    private java.util.List<Object> productionResults = new java.util.ArrayList<Object>();
    private boolean isStopped = false;

    public AbstractDataStoreProducer()
    {
    }

    public AVList getStoreParameters()
    {
        return this.params;
    }

    public void setStoreParameters(AVList parameters)
    {
        if (parameters == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateProductionParameters(parameters);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.params = parameters;
    }

    public Iterable<DataSource> getDataSources()
    {
        return java.util.Collections.unmodifiableList(this.dataSourceList);
    }

    public boolean acceptsDataSource(DataSource dataSource)
    {
        if (dataSource == null)
            return false;

        String message = this.validateDataSource(dataSource);
        //noinspection RedundantIfStatement
        if (message != null)
            return false;

        return true;
    }

    public boolean containsDataSource(DataSource dataSource)
    {
        return this.dataSourceList.contains(dataSource);
    }

    public void offerDataSource(DataSource dataSource)
    {
        if (dataSource == null)
        {
            String message = Logging.getMessage("nullValue.DataSourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateDataSource(dataSource);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.dataSourceList.add(dataSource);
    }

    public void offerAllDataSources(java.util.Collection<? extends DataSource> dataSources)
    {
        if (dataSources == null)
        {
            String message = Logging.getMessage("nullValue.CollectionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (DataSource source : dataSources)
            this.offerDataSource(source);
    }

    public void removeDataSource(DataSource dataSource)
    {
        if (dataSource == null)
        {
            String message = Logging.getMessage("nullValue.DataSourceIsNull");
            Logging.logger().warning(message);
            return; // Warn but don't throw an exception.
        }

        if (this.dataSourceList.contains(dataSource))
            this.dataSourceList.remove(dataSource);
    }

    public void removeAllDataSources()
    {
        this.dataSourceList.clear();
    }

    public void startProduction() throws Exception
    {
        if (this.isStopped())
        {
            String message = Logging.getMessage("DataStoreProducer.Stopped");
            Logging.logger().warning(message);
            return;
        }

        String message = this.validateProductionParameters(this.params);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.doStartProduction(this.params);
    }

    public synchronized void stopProduction()
    {
        this.isStopped = true;
    }

    protected synchronized boolean isStopped()
    {
        return this.isStopped;
    }

    public Iterable<?> getProductionResults()
    {
        return java.util.Collections.unmodifiableList(this.productionResults);
    }

    public void removeProductionState()
    {
        // Left as an optional operation for subclasses to define.
    }

    protected java.util.List<DataSource> getDataSourceList()
    {
        return this.dataSourceList;
    }

    protected java.util.List<Object> getProductionResultsList()
    {
        return this.productionResults;
    }

    protected abstract void doStartProduction(AVList parameters) throws Exception;

    protected abstract String validateProductionParameters(AVList parameters);

    protected abstract String validateDataSource(DataSource dataSource);
}
