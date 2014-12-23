/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.util.ServiceRegistry;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: DataIORegistry.java 8330 2009-01-05 21:31:20Z dcollins $
 */
public class DataIORegistry
{
    static
    {
        DataIORegistry.getInstance().registerDataDescriptorReader(BasicDataDescriptorReader.class);
        DataIORegistry.getInstance().registerDataDescriptorReader(WWDotNetLayerSetReader.class);
    }

    private static DataIORegistry instance;
    private ServiceRegistry registry;
 
    public DataIORegistry()
    {
        this.registry = new ServiceRegistry();
        this.registry.setName(Logging.getMessage("ServiceRegistry.DataIO.Name"));
        this.registry.addService(DataDescriptorReader.class);
    }

    public static DataIORegistry getInstance()
    {
        if (instance == null)
            instance = new DataIORegistry();
        return instance;
    }

    public Iterable<Class<? extends DataDescriptorReader>> getDataDescriptorReaders()
    {
        return this.registry.getServiceProviders(DataDescriptorReader.class);
    }

    public Iterable<? extends DataDescriptorReader> createDataDescriptorReaders()
    {
        return this.registry.createServiceProviders(DataDescriptorReader.class);
    }

    public boolean hasDataDescriptorReader(Class<? extends DataDescriptorReader> reader)
    {
        if (reader == null)
        {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return this.registry.hasServiceProvider(reader, DataDescriptorReader.class);
    }

    public void registerDataDescriptorReader(Class<? extends DataDescriptorReader> reader)
    {
        if (reader == null)
        {
            String message = Logging.getMessage("nullValue.ReaderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.registry.registerServiceProvider(reader, DataDescriptorReader.class);
    }
}
