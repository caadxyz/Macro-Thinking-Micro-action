/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.*;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Provides tracking of per-host network availability. Host computers that fail network requests can be logged to this
 * class' tracking list. When a host has been logged a specified number of times, it is marked as unreachable. Users can
 * query instances of this class to determine whether a host has been marked as unreachable.
 * <p/>
 * Users are expected to invoke this class' {@link #logUnavailableHost(java.net.URL)} method when an attempt to contact
 * a host fails. Each invocation increments the failure count by one. When the count exceeds the attempt limit, the host
 * is marked as unreachable. When attempts to contact the host <em>are</em> successful, users should invoke this class'
 * {@link #logAvailableHost(java.net.URL)} method to clear its status.
 * <p/>
 * A host may become reachable at a time subsequent to its being logged. To detect this, this class will mark a host as
 * not unreachable after a specifiable interval of time. If the host is once more logged as unavailable, its entry will
 * return to the unavailable state. This cycle continues indefinitely.
 * <p/>
 * Methods are also provided to determine whether the public network can be reached and whether the NASA World Wind
 * servers cab be reached.
 *
 * @author tag
 * @version $Id: BasicNetworkStatus.java 12551 2009-09-03 19:26:19Z tgaskins $
 */
public class BasicNetworkStatus extends AVListImpl implements NetworkStatus
{
    protected static final long DEFAULT_TRY_AGAIN_INTERVAL = (long) 60e3; // seconds
    protected static final int DEFAULT_ATTEMPT_LIMIT = 10; // number of unavailable events to declare host unavailable
    protected static final long NETWORK_STATUS_REPORT_INTERVAL = (long) 60e3;
    protected static final String[] DEFAULT_NETWORK_TEST_SITES = new String[]
        {"www.nasa.gov", "worldwind.arc.nasa.gov", "google.com", "microsoft.com", "yahoo.com"};

    protected static class HostInfo
    {
        protected final long tryAgainInterval;
        protected final int attemptLimit;
        protected AtomicInteger logCount = new AtomicInteger();
        protected AtomicLong lastLogTime = new AtomicLong();

        protected HostInfo(int attemptLimit, long tryAgainInterval)
        {
            this.lastLogTime.set(System.currentTimeMillis());
            this.logCount.set(1);
            this.tryAgainInterval = tryAgainInterval;
            this.attemptLimit = attemptLimit;
        }

        protected boolean isUnavailable()
        {
            return this.logCount.get() >= this.attemptLimit;
        }

        protected boolean isTimeToTryAgain()
        {
            return System.currentTimeMillis() - this.lastLogTime.get() >= this.tryAgainInterval;
        }
    }

    // Values exposed to the application.
    private CopyOnWriteArrayList<String> networkTestSites = new CopyOnWriteArrayList<String>();
    private AtomicLong tryAgainInterval = new AtomicLong(DEFAULT_TRY_AGAIN_INTERVAL);
    private AtomicInteger attemptLimit = new AtomicInteger(DEFAULT_ATTEMPT_LIMIT);
    private boolean offlineMode;

    // Fields for determining and remembering overall network status.
    protected ConcurrentHashMap<String, HostInfo> hostMap = new ConcurrentHashMap<String, HostInfo>();
    protected AtomicLong lastUnavailableLogTime = new AtomicLong(System.currentTimeMillis());
    protected AtomicLong lastAvailableLogTime = new AtomicLong(System.currentTimeMillis() + 1);
    protected AtomicLong lastNetworkCheckTime = new AtomicLong(System.currentTimeMillis());
    protected AtomicLong lastNetworkStatusReportTime = new AtomicLong(0);
    protected AtomicBoolean lastNetworkUnavailableResult = new AtomicBoolean(false);

    public BasicNetworkStatus()
    {
        String oms = Configuration.getStringValue(AVKey.OFFLINE_MODE, "false");
        this.offlineMode = oms.startsWith("t") || oms.startsWith("T");

        this.networkTestSites.addAll(Arrays.asList(DEFAULT_NETWORK_TEST_SITES));
    }

    /**
     * Indicates whether World Wind will attempt to connect to the network to retrieve data or for other reasons.
     *
     * @return <code>true</code> if World Wind is in off-line mode, <code>false</code> if not.
     */
    public boolean isOfflineMode()
    {
        return offlineMode;
    }

    /**
     * Indicate whether World Wind should attempt to connect to the network to retrieve data or for other reasons.
     * The default value for this attribute is <code>false</code>, indicating that the network should be used.
     *
     * @param offlineMode <code>true</code> if World Wind should use the network, <code>false</code> otherwise
     */
    public void setOfflineMode(boolean offlineMode)
    {
        this.offlineMode = offlineMode;
    }

    /**
     * Set the number of times a host must be logged as unavailable before it is marked unavailable in this class.
     *
     * @param limit the number of log-unavailability invocations necessary to consider the host unreachable.
     * @throws IllegalArgumentException if the limit is less than 1.
     */
    public void setAttemptLimit(int limit)
    {
        if (limit < 1)
        {
            String message = Logging.getMessage("NetworkStatus.InvalidAttemptLimit");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.attemptLimit.set(limit);
    }

    /**
     * Set the length of time to wait until a host is marked as not unreachable subsequent to its being marked
     * unreachable.
     *
     * @param interval The length of time, in milliseconds, to wait to unmark a host as unreachable.
     * @throws IllegalArgumentException if the interval is less than 0.
     */
    public void setTryAgainInterval(long interval)
    {
        if (interval < 0)
        {
            String message = Logging.getMessage("NetworkStatus.InvalidTryAgainInterval");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.tryAgainInterval.set(interval);
    }

    /**
     * Returns the number of times a host must be logged as unavailable before it is marked unavailable in this class.
     *
     * @return the limit.
     */
    public int getAttemptLimit()
    {
        return this.attemptLimit.get();
    }

    /**
     * Returns the length of time to wait until a host is marked as not unreachable subsequent to its being marked
     * unreachable.
     *
     * @return the interval, in milliseconds.
     */
    public long getTryAgainInterval()
    {
        return this.tryAgainInterval.get();
    }

    public List<String> getNetworkTestSites()
    {
        return new ArrayList<String>(networkTestSites);
    }

    public void setNetworkTestSites(List<String> networkTestSites)
    {
        if (networkTestSites == null)
        {
            String message = Logging.getMessage("nullValue.ServerListIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.networkTestSites.clear();
        this.networkTestSites.addAll(networkTestSites);
    }

    /**
     * Log a host as unavailable. Each invocation increments the host's attempt count. When the count equals or exceeds
     * the attempt limit, the host is marked as unavailable.
     *
     * @param url a url containing the host to mark as unavailable.
     */
    public synchronized void logUnavailableHost(URL url)
    {
        if (this.offlineMode)
            return;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            if (!hi.isUnavailable())
            {
                hi.logCount.incrementAndGet();
                if (hi.isUnavailable()) // host just became unavailable
                    this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
            }
            hi.lastLogTime.set(System.currentTimeMillis());
        }
        else
        {
            hi = new HostInfo(this.attemptLimit.get(), this.tryAgainInterval.get());
            hi.logCount.set(1);
            if (hi.isUnavailable()) // the attempt limit may be as low as 1, so handle that case here
                this.firePropertyChange(NetworkStatus.HOST_UNAVAILABLE, null, url);
            this.hostMap.put(hostName, hi);
        }

        this.lastUnavailableLogTime.set(System.currentTimeMillis());
    }

    /**
     * Log a host as available. Each invocation causes the host to no longer be marked as unavailable. Its
     * unavailability count is effectively set to 0.
     *
     * @param url a url containing the host to mark as available.
     */
    public synchronized void logAvailableHost(URL url)
    {
        if (this.offlineMode)
            return;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi != null)
        {
            this.hostMap.remove(hostName); // host is available again
            firePropertyChange(NetworkStatus.HOST_AVAILABLE, null, url);
        }

        this.lastAvailableLogTime.set(System.currentTimeMillis());
    }

    /**
     * Indicates whether the host has been marked as unavailable. To be marked unavailable a host's attempt count must
     * exceed the specified attempt limit.
     *
     * @param url a url containing the host to check for availability.
     * @return true if the host is marked as unavailable, otherwise false.
     */
    public synchronized boolean isHostUnavailable(URL url)
    {
        if (this.offlineMode)
            return true;

        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String hostName = url.getHost();
        HostInfo hi = this.hostMap.get(hostName);
        if (hi == null)
            return false;

        if (hi.isTimeToTryAgain())
        {
            hi.logCount.set(0); // info removed from table in logAvailableHost
            return false;
        }

        return hi.isUnavailable();
    }

    /**
     * Indicates whether a public network can be reached or has been reached in the previous five seconds.
     *
     * @return false if the network can be reached or has been reached in the previous five seconds, otherwise true.
     */
    public boolean isNetworkUnavailable()
    {
        return this.offlineMode || this.isNetworkUnavailable(5000L);
    }

    /**
     * Indicates whether a public network can be reached or has been reached in a specified previous amount of time.
     *
     * @param checkInterval the number of milliseconds in the past used to determine whether the server was avaialble
     *                      recently.
     * @return false if the network can be reached or has been reached in a specified time, otherwise true.
     */
    public synchronized boolean isNetworkUnavailable(long checkInterval)
    {
        if (this.offlineMode)
            return true;

        // If there's been success since failure, network assumed to be reachable.
        if (this.lastAvailableLogTime.get() > this.lastUnavailableLogTime.get())
        {
            this.lastNetworkUnavailableResult.set(false);
            return this.lastNetworkUnavailableResult.get();
        }

        long now = System.currentTimeMillis();

        // If there's been success recently, network assumed to be reachable.
        if (!this.lastNetworkUnavailableResult.get() && now - this.lastAvailableLogTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        // If query comes too soon after an earlier one that addressed the network, return the earlier result.
        if (now - this.lastNetworkCheckTime.get() < checkInterval)
        {
            return this.lastNetworkUnavailableResult.get();
        }

        this.lastNetworkCheckTime.set(now);

        if (!this.isWorlWindServerUnavailable())
        {
            this.lastNetworkUnavailableResult.set(false); // network not unreachable
            return this.lastNetworkUnavailableResult.get();
        }

        for (String testHost : networkTestSites)
        {
            if (isHostReachable(testHost))
            {
                {
                    this.lastNetworkUnavailableResult.set(false); // network not unreachable
                    return this.lastNetworkUnavailableResult.get();
                }
            }
        }

        if (now - this.lastNetworkStatusReportTime.get() > NETWORK_STATUS_REPORT_INTERVAL)
        {
            this.lastNetworkStatusReportTime.set(now);
            String message = Logging.getMessage("NetworkStatus.NetworkUnreachable");
            Logging.logger().info(message);
        }

        this.lastNetworkUnavailableResult.set(true); // if no successful contact then network is unreachable
        return this.lastNetworkUnavailableResult.get();
    }

    /**
     * Indicates whether the NASA World Wind servers can be reached.
     *
     * @return false if the servers can be reached, otherwise true.
     */
    public boolean isWorlWindServerUnavailable()
    {
        return this.offlineMode || !isHostReachable("worldwind.arc.nasa.gov");
    }

    protected static boolean isHostReachable(String hostName)
    {
        try
        {
            // Assume host is unreachable if we can't get its dns entry without getting an exception
            //noinspection ResultOfMethodCallIgnored
            InetAddress.getByName(hostName);
        }
        catch (UnknownHostException e)
        {
            String message = Logging.getMessage("NetworkStatus.UnreachableTestHost", hostName);
            Logging.logger().fine(message);
            return false;
        }
        catch (Exception e)
        {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            Logging.logger().info(message);
            return false;
        }

        // Was able to get internet address, but host still might not be reachable because the address might have been
        // cached earlier when it was available. So need to try something else.

        URLConnection connection = null;
        try
        {
            URL url = new URL("http://" + hostName);
            Proxy proxy = WWIO.configureProxy();
            if (proxy != null)
                connection = url.openConnection(proxy);
            else
                connection = url.openConnection();

            connection.setConnectTimeout(2000);
            String ct = connection.getContentType();
            if (ct != null)
                return true;
        }
        catch (IOException e)
        {
            String message = Logging.getMessage("NetworkStatus.ExceptionTestingHost", hostName);
            Logging.logger().info(message);
        }
        finally
        {
            if (connection != null && connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
        }

        return false;
    }
//
//    public static void main(String[] args)
//    {
//        try
//        {
//            NetworkStatus ns = new BasicNetworkStatus();
//            boolean tf = ns.isWorlWindServerUnavailable();
//            tf = ns.isNetworkUnavailable();
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//    }
}
