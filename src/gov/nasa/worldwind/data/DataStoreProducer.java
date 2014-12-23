/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.WWObject;

/**
 * @author dcollins
 * @version $Id: DataStoreProducer.java 8321 2009-01-05 17:06:14Z dcollins $
 */
public interface DataStoreProducer extends WWObject
{
    AVList getStoreParameters();

    void setStoreParameters(AVList parameters);

    String getDataSourceDescription();
    
    Iterable<DataSource> getDataSources();

    boolean acceptsDataSource(DataSource dataSource);

    boolean containsDataSource(DataSource dataSource);

    void offerDataSource(DataSource dataSource);

    void offerAllDataSources(java.util.Collection<? extends DataSource> dataSources);

    void removeDataSource(DataSource dataSource);

    void removeAllDataSources();

    void startProduction() throws Exception;

    void stopProduction();

    Iterable<?> getProductionResults();

    void removeProductionState();
}
