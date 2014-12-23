/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import java.util.*;

/**
 * @author dcollins
 * @version $Id: ServiceRegistry.java 7008 2008-10-11 00:58:57Z dcollins $
 */
public class ServiceRegistry
{
    private String name = "";
    private Map<Class<?>, ProviderRegistry<?>> serviceMap = new HashMap<Class<?>, ProviderRegistry<?>>();

    public ServiceRegistry()
    {
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = (name != null ? name : "");
    }

    public Iterable<Class<?>> getServices()
    {
        return this.serviceMap.keySet();
    }

    public <T> boolean hasService(Class<T> service)
    {
        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        return reg != null;
    }

    public <T> void addService(Class<T> service)
    {
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = new ProviderRegistry<T>(service);
        this.serviceMap.put(service, reg);

        String message = Logging.getMessage("ServiceRegistry.RegisterServiceCategory", this.getName(), service);
        Logging.logger().finer(message);
    }

    public <T> Iterable<Class<? extends T>> getServiceProviders(Class<T> service)
    {
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        return reg.getProviders();    
    }

    public <T> Iterable<? extends T> createServiceProviders(Class<T> service)
    {
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        return this.instantiateProviders(reg.getProviders());
    }

    public <T> boolean hasServiceProvider(Class<? extends T> provider, Class<T> service)
    {
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        return reg.hasProvider(provider);
    }

    public <T> void registerServiceProvider(Class<? extends T> provider, Class<T> service)
    {
        if (provider == null)
        {
            String message = Logging.getMessage("nullValue.ServiceProviderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateProvider(provider, service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        reg.registerProvider(provider);

        message = Logging.getMessage("ServiceRegistry.RegisterServiceProvider",
            this.getName(), provider, reg.getService());
        Logging.logger().finer(message);
    }

    public <T> void deregisterServiceProvider(Class<? extends T> provider, Class<T> service)
    {
        if (provider == null)
        {
            String message = Logging.getMessage("nullValue.ServiceProviderIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateProvider(provider, service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        reg.deregisterProvider(provider);

        message = Logging.getMessage("ServiceRegistry.DeregisterServiceProvider",
            this.getName(), provider, reg.getService());
        Logging.logger().finer(message);
    }

    public <T> void deregisterAll(Class<T> service)
    {
        if (service == null)
        {
            String message = Logging.getMessage("nullValue.ServiceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String message = this.validateService(service);
        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ProviderRegistry<T> reg = this.getProviderRegistry(service); 
        reg.deregisterAll();

        message = Logging.getMessage("ServiceRegistry.DeregisterAllServiceProviders",
            this.getName(), reg.getService());
        Logging.logger().finer(message);
    }

    public void deregisterAll()
    {
        for (ProviderRegistry reg : this.serviceMap.values())
        {
            reg.deregisterAll();

            String message = Logging.getMessage("ServiceRegistry.DeregisterAllServiceProviders",
                this.getName(), reg.getService());
            Logging.logger().finer(message);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected <T> ProviderRegistry<T> getProviderRegistry(Class<T> service)
    {
        return (ProviderRegistry<T>) this.serviceMap.get(service);
    }

    protected <T> Iterable<? extends T> instantiateProviders(Iterable<Class<? extends T>> providers)
    {
        List<T> instances = new ArrayList<T>();

        for (Class<? extends T> cls : providers)
        {
            try
            {
                T newInstance = cls.newInstance();
                instances.add(newInstance);
            }
            catch (IllegalAccessException e)
            {
                String message = Logging.getMessage("ServiceRegistry.NoDefaultConstructor");
                Logging.logger().severe(message);
            }
            catch (InstantiationException e)
            {
                String message = Logging.getMessage("ServiceRegistry.NonInstantiableServiceProvider");
                Logging.logger().severe(message);
            }
        }

        return instances;
    }

    protected <T> String validateService(Class<T> service)
    {
        StringBuilder sb = new StringBuilder();

        ProviderRegistry<T> reg = this.getProviderRegistry(service);
        if (reg == null)
            sb.append(sb.length() > 0 ? ", " : "")
                    .append(Logging.getMessage("ServiceRegistry.UnknownService", service));

        if (sb.length() == 0)
            return null;

        return sb.toString();
    }

    protected <T> String validateProvider(Class<? extends T> provider, Class<T> service)
    {
        StringBuilder sb = new StringBuilder();

        if (!service.isAssignableFrom(provider))
            sb.append(sb.length() > 0 ? ", " : "")
                    .append(Logging.getMessage("ServiceRegistry.InvalidServiceProviderType", provider));

        if (sb.length() == 0)
            return null;

        return sb.toString();
    }

    protected static class ProviderRegistry<T>
    {
        private Class<T> service;
        private List<Class<? extends T>> providerList;

        public ProviderRegistry(Class<T> service)
        {
            this.service = service;
            this.providerList = new ArrayList<Class<? extends T>>();
        }

        public Class<T> getService()
        {
            return this.service;
        }

        public Iterable<Class<? extends T>> getProviders()
        {
            return Collections.unmodifiableList(this.providerList);
        }

        public boolean hasProvider(Class<? extends T> provider)
        {
            Object o = this.providerList.contains(provider);
            return o != null;
        }

        public void registerProvider(Class<? extends T> provider)
        {
            if (!this.providerList.contains(provider))
                this.providerList.add(provider);
        }

        public void deregisterProvider(Class<? extends T> provider)
        {
            if (!this.providerList.contains(provider))
                this.providerList.remove(provider);
        }

        public void deregisterAll()
        {
            this.providerList.clear();
        }
    }
}
