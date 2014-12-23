/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.AVList;

import java.net.URL;
import java.util.List;

/**
 * @author tag
 * @version $Id: NetworkStatus.java 12551 2009-09-03 19:26:19Z tgaskins $
 */
public interface NetworkStatus extends AVList
{
    public static final String HOST_UNAVAILABLE = "gov.nasa.worldwind.util.NetworkStatus.HostUnavailable";
    public static final String HOST_AVAILABLE = "gov.nasa.worldwind.util.NetworkStatus.HostAvailable";

    void logUnavailableHost(URL url);

    void logAvailableHost(URL url);

    boolean isHostUnavailable(URL url);

    boolean isNetworkUnavailable();

    boolean isWorlWindServerUnavailable();

    int getAttemptLimit();

    long getTryAgainInterval();

    /**
     * Indicates whether World Wind will attempt to connect to the network to retrieve data or for other reasons.
     *
     * @return <code>true</code> if World Wind is in off-line mode, <code>false</code> if not.
     */
    boolean isOfflineMode();

    /**
     * Indicates whether World Wind should attempt to connect to the network to retrieve data or for other reasons. The
     * default value for this attribute is <code>false</code>, indicating that the network should be used.
     *
     * @param offlineMode <code>true</code> if World Wind should use the network, <code>false</code> otherwise
     */
    void setOfflineMode(boolean offlineMode);

    /**
     * Sets the number of times a host must be logged as unavailable before it is marked unavailable in this class.
     *
     * @param limit the number of log-unavailability invocations necessary to consider the host unreachable.
     *
     * @throws IllegalArgumentException if the limit is less than 1.
     */
    void setAttemptLimit(int limit);

    /**
     * Sets the length of time to wait until a host is marked as not unreachable subsequent to its being marked
     * unreachable.
     *
     * @param interval The length of time, in milliseconds, to wait to unmark a host as unreachable.
     *
     * @throws IllegalArgumentException if the interval is less than 0.
     */
    void setTryAgainInterval(long interval);

    /**
     * Returns the server domain names of the sites used to test public network availability.
     *
     * @return the list of sites used to check network status. The list is a copy of the internal list, so changes to it
     *         do not affect instances of this class.
     */
    List<String> getNetworkTestSites();

    /**
     * Sets the domain names, e.g., worldwind.arc.nasa.gov, of sites used to determine public network availability.
     *
     * @param networkTestSites the list of desired test sites. The list is copied internally, so changes made to
     *                         the submitted list do not affect instances of this class.
     *
     * @throws IllegalArgumentException if the test site list is null.
     */
    void setNetworkTestSites(List<String> networkTestSites);
}
