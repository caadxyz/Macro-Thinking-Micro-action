/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.LayerConfiguration;
import gov.nasa.worldwind.terrain.ElevationModelConfiguration;
import gov.nasa.worldwind.wms.*;
import org.w3c.dom.*;

import javax.xml.xpath.XPath;
import java.io.InputStream;
import java.net.*;

/**
 * A collection of static methods useful for opening, reading, and otherwise working with {@link
 * gov.nasa.worldwind.DataConfiguration} resources.
 *
 * @author dcollins
 * @version $Id: DataConfigurationUtils.java 12676 2009-09-30 02:47:31Z dcollins $
 */
public class DataConfigurationUtils
{
    /**
     * Returns true if the specified {@link org.w3c.dom.Element} should be accepted as a configuration document.
     *
     * @param domElement the document in question.
     *
     * @return true if the document should be accepted; false otherwise.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static boolean isDataConfig(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (LayerConfiguration.isLayerDocument(domElement))
            return true;

        if (ElevationModelConfiguration.isElevationModelDocument(domElement))
            return true;

        if (InstalledDataConfiguration.isInstalledDataDocument(domElement))
            return true;

        //noinspection RedundantIfStatement
        if (WWDotNetLayerSetConfiguration.isLayerSetDocument(domElement))
            return true;

        return false;
    }

    /**
     * Create a DataConfiguration from a specified Document. Maps the document type to the appropriate base
     * DataConfiguration type. Document types recognized are: <ul> <li>"Layer" for {@link
     * gov.nasa.worldwind.layers.Layer} configuration documents</li> <li>"ElevationModel" for {@link
     * gov.nasa.worldwind.globes.ElevationModel} configuration documents</li> <li>"dataDescriptor" for DataDescriptor
     * documents</li> <li>"LevelSet" for World Wind .NET LayerSet documents</li> </ul> If the document type is not
     * recognized, this returns a default implementation of DataConfiguration.
     *
     * @param domElement the document to create
     *
     * @return a DataConfiguration with the document as its backing contents.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static DataConfiguration createDataConfig(Element domElement)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (LayerConfiguration.isLayerDocument(domElement))
        {
            return new LayerConfiguration(domElement);
        }
        else if (ElevationModelConfiguration.isElevationModelDocument(domElement))
        {
            return new ElevationModelConfiguration(domElement);
        }
        else if (InstalledDataConfiguration.isInstalledDataDocument(domElement))
        {
            return InstalledDataConfiguration.createDataConfig(domElement);
        }
        else if (WWDotNetLayerSetConfiguration.isLayerSetDocument(domElement))
        {
            return WWDotNetLayerSetConfiguration.createDataConfig(domElement);
        }
        else
        {
            return new BasicDataConfiguration(domElement);
        }
    }

    /**
     * Opens a configuration file given the file's location in the file system or on the classpath.
     *
     * @param filePath the path to the file. Must be an absolute path or a path relative to a location in the
     *                 classpath.
     * @param c        the class that is used to find a path relative to the classpath.
     *
     * @return a DataConfiguration for the file, or null if the file cannot be understood as a configuration file.
     *
     * @throws IllegalArgumentException if the file path is null.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if an exception or error occurs while opening and parsing the file. The causing
     *                                  exception is included in this exception's {@link Throwable#initCause(Throwable)}
     *                                  .
     */
    public static DataConfiguration openDataConfigFile(String filePath, Class c)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream inputStream = WWIO.openFileOrResourceStream(filePath, c);
        if (inputStream == null)
            return null;

        return openDataConfigStream(inputStream);
    }

    /**
     * Reads configuration information given a {@link java.net.URL}.
     *
     * @param url the configuration information .
     *
     * @return a DataConfiguration for the stream content, or null if the stream cannot be understood as configuration
     *         information.
     *
     * @throws IllegalArgumentException if the input stream is null.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if an exception or error occurs while parsing the string. The causing exception
     *                                  is included in this exception's {@link Throwable#initCause(Throwable)}
     */
    public static DataConfiguration openDataConfigURL(java.net.URL url)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            InputStream inputStream = url.openStream();

            return openDataConfigStream(inputStream);
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("generic.ExceptionWhileReading", url.toExternalForm());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
            throw new WWRuntimeException(msg, e);
        }
    }

    /**
     * Reads configuration information given an input stream.
     *
     * @param inputStream the configuration information as an input stream.
     *
     * @return a DataConfiguration for the stream content, or null if the stream cannot be understood as configuration
     *         information.
     *
     * @throws IllegalArgumentException if the input stream is null.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if an exception or error occurs while parsing the string. The causing exception
     *                                  is included in this exception's {@link Throwable#initCause(Throwable)}
     */
    public static DataConfiguration openDataConfigStream(InputStream inputStream)
    {
        if (inputStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Document doc = WWXML.openDocumentStream(inputStream);
        if (doc == null || doc.getDocumentElement() == null)
            return null;

        return createDataConfig(doc.getDocumentElement());
    }

    /**
     * Saves a specified configuration document to a specified file store, under a specified name.
     *
     * @param doc       the DOM document to save.
     * @param fileStore the file store to save the document under.
     * @param fileName  the path to the file. Must be an relative path in the file store.
     *
     * @throws IllegalArgumentException if either the document, file store, or file name are null.
     * @throws WWRuntimeException       if the file cannot be created, or if an exception or error occurs while writing
     *                                  the document. The causing exception is included in this exception's {@link
     *                                  Throwable#initCause(Throwable)}
     */
    public static void saveDataConfigDocument(Document doc, FileStore fileStore, String fileName)
    {
        if (doc == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.io.File file = fileStore.newFile(fileName);
        if (file == null)
        {
            String message = Logging.getMessage("generic.CannotCreateFile", fileName);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }

        WWXML.saveDocumentToFile(doc, file.getPath());

        String message = Logging.getMessage("generic.ConfigurationFileCreated", fileName);
        Logging.logger().fine(message);
    }

    /**
     * Returns true if a configuration file name exists in the store which has not expired. This returns false if a
     * configuration file does not exist, or it has expired. This invokes {@link #findExistingDataConfigFile(gov.nasa.worldwind.cache.FileStore,
     * String)} to determine the URL of any existing file names. If an existing file has expired, and removeIfExpired is
     * true, this removes the existing file.
     *
     * @param fileStore       the file store in which to look.
     * @param fileName        the file name to look for. If a file with this nname does not exist in the store, this
     *                        looks at the file's siblings for a match.
     * @param removeIfExpired true to remove the existing file, if it exists and is expired; false otherwise.
     * @param expiryTime      the time in milliseconds, before which a file is considered to be expired.
     *
     * @return whether a configuration file already exists which has not expired.
     *
     * @throws IllegalArgumentException if either the file store or file name are null.
     */
    public static boolean hasDataConfigFile(FileStore fileStore, String fileName, boolean removeIfExpired,
        long expiryTime)
    {
        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Look for an existing configuration file in the store. Return true if a configuration file does not exist,
        // or it has expired; otherwise return false.
        java.net.URL url = findExistingDataConfigFile(fileStore, fileName);
        if (url != null && !WWIO.isFileOutOfDate(url, expiryTime))
            return true;

        // A configuration file exists but it is expired. Remove the file and return false, indicating that there is
        // no configuration document.
        if (url != null && removeIfExpired)
        {
            fileStore.removeFile(url);

            String message = Logging.getMessage("generic.DataFileExpired", url);
            Logging.logger().fine(message);
        }

        return false;
    }

    /**
     * Returns the URL of an existing data configuration file under the specified file store, or null if no
     * configuration file exists. This first looks for a configuration file with the specified name. If that does not
     * exists, this checks the siblings of the specified file for a configuration file match.
     *
     * @param fileStore the file store in which to look.
     * @param fileName  the file name to look for. If a file with this nname does not exist in the store, this looks at
     *                  the file's siblings for a match.
     *
     * @return the URL of an existing configuration file in the store, or null if none exists.
     *
     * @throws IllegalArgumentException if either the file store or file name are null.
     */
    public static java.net.URL findExistingDataConfigFile(FileStore fileStore, String fileName)
    {
        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (fileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Attempt to find the specified file name in the store. If it exists, then we've found a match and we're done.
        java.net.URL url = fileStore.findFile(fileName, false);
        if (url != null)
            return url;

        // If the specified name did not exist, then try to find any data configuration file under the file's parent
        // path. Find only the file names which are siblings of the specified file name.
        String path = WWIO.getParentFilePath(fileName);
        if (path == null || path.length() == 0)
            return null;

        String[] names = fileStore.listFileNames(path, new DataConfigurationFilter());
        if (names == null || names.length == 0)
            return null;

        // Ignore all but the first file match.
        return fileStore.findFile(names[0], false);
    }

    /**
     * Returns a file store path name for the specified parameters list. This returns null if the parameter list does
     * not contain enough information to construct a path name.
     *
     * @param params the parameter list to extract a configuration filename from.
     * @param suffix the file suffix to append on the path name, or null to append no suffix.
     *
     * @return a file store path name with the specified suffix, or null if a path name cannot be constructed.
     *
     * @throws IllegalArgumentException if the parameter list is null.
     */
    public static String getDataConfigFilename(AVList params, String suffix)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String path = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (path == null || path.length() == 0)
            return null;

        String filename = params.getStringValue(AVKey.DATASET_NAME);

        if (filename == null || filename.length() == 0)
            filename = params.getStringValue(AVKey.DISPLAY_NAME);

        if (filename == null || filename.length() == 0)
            filename = "DataConfiguration";

        filename = WWIO.stripIllegalFileNameCharacters(filename);

        return path + java.io.File.separator + filename + (suffix != null ? suffix : "");
    }

    /**
     * Parses LevelSet parameters from a specified DOM document. This writes output as key-value pairs to params. If a
     * parameter from the XML document already exists in params, that parameter is ignored. Supported key and parameter
     * names are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td>DatasetName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td>DataCacheName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#SERVICE}</td><td>Service/URL</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>ExpiryTime</td><td>Long</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>LastUpdate</td><td>Long</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#FORMAT_SUFFIX}</td><td>FormatSuffix</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NUM_LEVELS}</td><td>NumLevels/@count</td><td>Integer</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NUM_EMPTY_LEVELS}</td><td>NumLevels/@numEmpty</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#INACTIVE_LEVELS}</td><td>NumLevels/@inactive</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR}</td><td>Sector</td><td>{@link
     * gov.nasa.worldwind.geom.Sector}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR_RESOLUTION_LIMITS}</td><td>SectorResolutionLimit</td>
     * <td>{@link LevelSet.SectorResolution}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_ORIGIN}</td><td>TileOrigin/LatLon</td><td>{@link
     * gov.nasa.worldwind.geom.LatLon}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_WIDTH}</td><td>TileSize/Dimension/@width</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_HEIGHT}</td><td>TileSize/Dimension/@height</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>LastUpdate</td><td>LatLon</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MAX_ABSENT_TILE_ATTEMPTS}</td><td>AbsentTiles/MaxAttempts</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MIN_ABSENT_TILE_CHECK_INTERVAL}</td><td>AbsentTiles/MinCheckInterval/Time</td><td>Integer
     * milliseconds</td></tr> </table>
     *
     * @param domElement the XML document root to parse for LevelSet parameters.
     * @param params     the output key-value pairs which recieve the LevelSet parametres. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getLevelSetParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        // Title and cache name properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.DATASET_NAME, "DatasetName", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.DATA_CACHE_NAME, "DataCacheName", xpath);

        // Service properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.SERVICE, "Service/URL", xpath);
        WWXML.checkAndSetLongParam(domElement, params, AVKey.EXPIRY_TIME, "ExpiryTime", xpath);
        WWXML.checkAndSetLongParam(domElement, params, AVKey.EXPIRY_TIME, "LastUpdate", xpath);

        // Image format properties.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.FORMAT_SUFFIX, "FormatSuffix", xpath);

        // Tile structure properties.
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.NUM_LEVELS, "NumLevels/@count", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.NUM_EMPTY_LEVELS, "NumLevels/@numEmpty", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.INACTIVE_LEVELS, "NumLevels/@inactive", xpath);
        WWXML.checkAndSetSectorParam(domElement, params, AVKey.SECTOR, "Sector", xpath);
        WWXML.checkAndSetSectorResolutionParam(domElement, params, AVKey.SECTOR_RESOLUTION_LIMITS,
            "SectorResolutionLimit", xpath);
        WWXML.checkAndSetLatLonParam(domElement, params, AVKey.TILE_ORIGIN, "TileOrigin/LatLon", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.TILE_WIDTH, "TileSize/Dimension/@width", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.TILE_HEIGHT, "TileSize/Dimension/@height", xpath);
        WWXML.checkAndSetLatLonParam(domElement, params, AVKey.LEVEL_ZERO_TILE_DELTA, "LevelZeroTileDelta/LatLon",
            xpath);

        // Retrieval properties.
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS,
            "AbsentTiles/MaxAttempts", xpath);
        WWXML.checkAndSetTimeParamAsInteger(domElement, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "AbsentTiles/MinCheckInterval/Time", xpath);

        return params;
    }

    /**
     * Parses LevelSet parameters from the configuration information. This writes output as key-value pairs to params.
     * If a parameter from the configuration already exists in params, that parameter is ignored. Supported key and
     * parameter names are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td>DatasetName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td>DataCacheName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#SERVICE}</td><td>Service/URL</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>ExpiryTime</td><td>Long</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>LastUpdate</td><td>Long</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#FORMAT_SUFFIX}</td><td>FormatSuffix</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NUM_LEVELS}</td><td>NumLevels/@count</td><td>Integer</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NUM_EMPTY_LEVELS}</td><td>NumLevels/@numEmpty</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#INACTIVE_LEVELS}</td><td>NumLevels/@inactive</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR}</td><td>Sector</td><td>{@link
     * gov.nasa.worldwind.geom.Sector}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR_RESOLUTION_LIMITS}</td><td>SectorResolutionLimit</td>
     * <td>{@link LevelSet.SectorResolution}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_ORIGIN}</td><td>TileOrigin/LatLon</td><td>{@link
     * gov.nasa.worldwind.geom.LatLon}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_WIDTH}</td><td>TileSize/Dimension/@width</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_HEIGHT}</td><td>TileSize/Dimension/@height</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>LastUpdate</td><td>LatLon</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MAX_ABSENT_TILE_ATTEMPTS}</td><td>AbsentTiles/MaxAttempts</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MIN_ABSENT_TILE_CHECK_INTERVAL}</td><td>AbsentTiles/MinCheckInterval/Time</td><td>Integer
     * milliseconds</td></tr> </table>
     *
     * @param dataConfig the configuration information in which to look for for LevelSet parameters.
     * @param params     the output key-value pairs which recieve the LevelSet parameters. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the configuration information is null.
     */
    public static AVList getLevelSetParams(DataConfiguration dataConfig, AVList params)
    {
        if (dataConfig == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        // Title and cache name properties.
        checkAndSetStringParam(dataConfig, params, AVKey.DATASET_NAME, "DatasetName");
        checkAndSetStringParam(dataConfig, params, AVKey.DATA_CACHE_NAME, "DataCacheName");

        // Service properties.
        checkAndSetStringParam(dataConfig, params, AVKey.SERVICE, "Service/URL");

        // Expiry time properties.
        checkAndSetLongParam(dataConfig, params, AVKey.EXPIRY_TIME, "ExpiryTime");
        checkAndSetLongParam(dataConfig, params, AVKey.EXPIRY_TIME, "LastUpdate");

        // Image format properties.
        checkAndSetStringParam(dataConfig, params, AVKey.FORMAT_SUFFIX, "FormatSuffix");

        // Tile structure properties.
        checkAndSetIntegerParam(dataConfig, params, AVKey.NUM_LEVELS, "NumLevels/@count");
        checkAndSetIntegerParam(dataConfig, params, AVKey.NUM_EMPTY_LEVELS, "NumLevels/@numEmpty");
        checkAndSetStringParam(dataConfig, params, AVKey.INACTIVE_LEVELS, "NumLevels/@inactive");
        checkAndSetSectorParam(dataConfig, params, AVKey.SECTOR, "Sector");
        checkAndSetSectorResolutionParam(dataConfig, params, AVKey.SECTOR_RESOLUTION_LIMITS, "SectorResolutionLimit");
        checkAndSetLatLonParam(dataConfig, params, AVKey.TILE_ORIGIN, "TileOrigin/LatLon");
        checkAndSetIntegerParam(dataConfig, params, AVKey.TILE_WIDTH, "TileSize/Dimension/@width");
        checkAndSetIntegerParam(dataConfig, params, AVKey.TILE_HEIGHT, "TileSize/Dimension/@height");
        checkAndSetLatLonParam(dataConfig, params, AVKey.LEVEL_ZERO_TILE_DELTA, "LevelZeroTileDelta/LatLon");

        // Retrieval properties.
        checkAndSetIntegerParam(dataConfig, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS,
            "AbsentTiles/MaxAttempts");
        checkAndSetTimeParamAsInteger(dataConfig, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "AbsentTiles/MinCheckInterval/Time");

        return params;
    }

    /**
     * Gathers LevelSet parameters from a specified LevelSet reference. This writes output as key-value pairs params. If
     * a parameter from the XML document already exists in params, that parameter is ignored. Supported key and
     * parameter names are: <table> <th><td>Key</td><td>Value</td><td>Type</td></th> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td>First Level's dataset</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td>First Level's
     * cacheName</td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SERVICE}</td><td>First Level's
     * service</td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>First
     * Level's expiryTime</td><td>Long</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#FORMAT_SUFFIX}</td><td>FirstLevel's
     * formatSuffix</td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_LEVELS}</td><td>numLevels</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_EMPTY_LEVELS}</td><td>1 + index of first non-empty
     * Level</td><td>Integer</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#INACTIVE_LEVELS}</td><td>Comma
     * delimited string of Level numbers</td><td>String</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR}</td><td>sector</td><td>{@link
     * gov.nasa.worldwind.geom.Sector}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR_RESOLUTION_LIMITS}</td><td>sectorLevelLimits</td>
     * <td>{@link LevelSet.SectorResolution}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_ORIGIN}</td><td>tileOrigin</td><td>{@link
     * gov.nasa.worldwind.geom.LatLon}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_WIDTH}</td><td>First
     * Level's tileWidth<td><td>Integer</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_HEIGHT}</td><td>First
     * Level's tileHeight</td><td>Integer</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>levelZeroTileDelta</td><td>LatLon</td></tr>
     * </table>
     *
     * @param levelSet the LevelSet reference to gather parameters from.
     * @param params   the output key-value pairs which recieve the LevelSet parameters. A null reference is permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getLevelSetParams(LevelSet levelSet, AVList params)
    {
        if (levelSet == null)
        {
            String message = Logging.getMessage("nullValue.LevelSetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        Level firstLevel = levelSet.getFirstLevel();

        // Title and cache name properties.
        String s = params.getStringValue(AVKey.DATASET_NAME);
        if (s == null || s.length() == 0)
        {
            s = firstLevel.getDataset();
            if (s != null && s.length() > 0)
                params.setValue(AVKey.DATASET_NAME, s);
        }

        s = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (s == null || s.length() == 0)
        {
            s = firstLevel.getCacheName();
            if (s != null && s.length() > 0)
                params.setValue(AVKey.DATA_CACHE_NAME, s);
        }

        // Service properties.
        s = params.getStringValue(AVKey.SERVICE);
        if (s == null || s.length() == 0)
        {
            s = firstLevel.getService();
            if (s != null && s.length() > 0)
                params.setValue(AVKey.SERVICE, s);
        }

        Object o = params.getValue(AVKey.EXPIRY_TIME);
        if (o == null)
        {
            // If the expiry time is zero or negative, then treat it as an uninitialized value.
            long l = firstLevel.getExpiryTime();
            if (l > 0)
                params.setValue(AVKey.EXPIRY_TIME, l);
        }

        // Image format properties.
        s = params.getStringValue(AVKey.FORMAT_SUFFIX);
        if (s == null || s.length() == 0)
        {
            s = firstLevel.getFormatSuffix();
            if (s != null && s.length() > 0)
                params.setValue(AVKey.FORMAT_SUFFIX, s);
        }

        // Tile structure properties.
        o = params.getValue(AVKey.NUM_LEVELS);
        if (o == null)
            params.setValue(AVKey.NUM_LEVELS, levelSet.getNumLevels());

        o = params.getValue(AVKey.NUM_EMPTY_LEVELS);
        if (o == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, getNumEmptyLevels(levelSet));

        s = params.getStringValue(AVKey.INACTIVE_LEVELS);
        if (s == null || s.length() == 0)
        {
            s = getInactiveLevels(levelSet);
            if (s != null && s.length() > 0)
                params.setValue(AVKey.INACTIVE_LEVELS, s);
        }

        o = params.getValue(AVKey.SECTOR);
        if (o == null)
        {
            Sector sector = levelSet.getSector();
            if (sector != null)
                params.setValue(AVKey.SECTOR, sector);
        }

        o = params.getValue(AVKey.SECTOR_RESOLUTION_LIMITS);
        if (o == null)
        {
            LevelSet.SectorResolution[] srs = levelSet.getSectorLevelLimits();
            if (srs != null && srs.length > 0)
                params.setValue(AVKey.SECTOR_RESOLUTION_LIMITS, srs);
        }

        o = params.getValue(AVKey.TILE_ORIGIN);
        if (o == null)
        {
            LatLon ll = levelSet.getTileOrigin();
            if (ll != null)
                params.setValue(AVKey.TILE_ORIGIN, ll);
        }

        o = params.getValue(AVKey.TILE_WIDTH);
        if (o == null)
            params.setValue(AVKey.TILE_WIDTH, firstLevel.getTileWidth());

        o = params.getValue(AVKey.TILE_HEIGHT);
        if (o == null)
            params.setValue(AVKey.TILE_HEIGHT, firstLevel.getTileHeight());

        o = params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (o == null)
        {
            LatLon ll = levelSet.getLevelZeroTileDelta();
            if (ll != null)
                params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);
        }

        // Note: retrieval properties MAX_ABSENT_TILE_ATTEMPTS and MIN_ABSENT_TILE_CHECK_INTERVAL are initialized
        // through the AVList constructor on LevelSet and Level. Rather than expose those properties in Level, we rely
        // on the caller to gather those properties via the AVList used to construct the LevelSet.

        return params;
    }

    /**
     * Parses WMS layer parameters from the XML configuration document starting at domElement. This writes output as
     * key-value pairs to params. If a parameter from the XML document already exists in params, that parameter is
     * ignored. Supported key and parameter names are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th>
     * <tr><td>{@link AVKey#WMS_VERSION}</td><td>Service/@version</td><td>String</td></tr> <tr><td>{@link
     * AVKey#LAYER_NAMES}</td><td>Service/LayerNames</td><td>String</td></tr> <tr><td>{@link
     * AVKey#STYLE_NAMES}</td><td>Service/StyleNames</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_MAP_URL}</td><td>Service/GetMapURL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_CAPABILITIES_URL}</td><td>Service/GetCapabilitiesURL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#SERVICE}</td><td>AVKey#GET_MAP_URL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#DATASET_NAME}</td><td>AVKey.LAYER_NAMES</td><td>String</td></tr> </table>
     *
     * @param domElement the XML document root to parse for WMS layer parameters.
     * @param params     the output key-value pairs which recieve the WMS layer parameters. A null reference is
     *                   permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getWMSLayerParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        // Need to determine these for URLBuilder construction.
        WWXML.checkAndSetStringParam(domElement, params, AVKey.WMS_VERSION, "Service/@version", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.LAYER_NAMES, "Service/LayerNames", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.STYLE_NAMES, "Service/StyleNames", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.GET_MAP_URL, "Service/GetMapURL", xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.GET_CAPABILITIES_URL, "Service/GetCapabilitiesURL",
            xpath);
        WWXML.checkAndSetStringParam(domElement, params, AVKey.IMAGE_FORMAT, "ImageFormat", xpath);

        params.setValue(AVKey.SERVICE, params.getValue(AVKey.GET_MAP_URL));
        String serviceURL = params.getStringValue(AVKey.SERVICE);
        if (serviceURL != null)
            params.setValue(AVKey.SERVICE, WWXML.fixGetMapString(serviceURL));

        // The dataset name is the layer-names string for WMS elevation models
        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        if (layerNames != null)
            params.setValue(AVKey.DATASET_NAME, layerNames);

        return params;
    }

    /**
     * Parses WMS layer parameters from the WMS {@link Capabilities} document starting at domElement. The specified
     * parameter list must contain a non-empty String value under the key {@link AVKey#LAYER_NAMES}, and it must match
     * one of the names layers in the specified Capabilities document. The parameter list may optionally contain a
     * String value under the key {@link AVKey#STYLE_NAMES}. This writes output as key-value pairs to params. Supported
     * key and parameter names are: <table> <th><td>Key</td><td>Value</td><td>Type</td></th> <tr><td>{@link
     * AVKey#DISPLAY_NAME}</td><td>Combination of WMS layer names and style names</td><td>String</td></tr>
     * <tr><td>{@link AVKey#DATASET_NAME}</td><td>WMS layer names</td><td>String</td></tr> <tr><td>{@link
     * AVKey#EXPIRY_TIME}</td><td>WMS layer last update time</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_MAP_URL}</td><td>WMS GetMap Get URL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_CAPABILITIES_URL}</td><td>WMS GetCapabilities Get URL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#SERVICE}</td><td>WMS GetMap Get URL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#SERVICE_NAME}</td><td>Capabilities#WMS_SERVICE_NAME</td><td>String</td></tr> <tr><td>{@link
     * AVKey#WMS_VERSION}</td><td>WMS version</td><td>String</td></tr> <tr><td>{@link
     * AVKey#DATA_CACHE_NAME}</td><td>Combination of WMS layer names and style names</td><td>String</td></tr>
     * <tr><td>{@link AVKey#IMAGE_FORMAT}</td><td>First WMS image format matching the specified
     * formats</td><td>String</td></tr> <tr><td>{@link AVKey#SECTOR}</td><td>Union of WMS layer bounding
     * boxes</td><td>Sector</td></tr> </table>
     *
     * @param caps                  the WMS Capabilities document to parse for WMS layer parameters.
     * @param formatOrderPreference an ordered array of preferred image formats, or null to use the default format.
     * @param params                the output key-value pairs which recieve the WMS layer parameters.
     *
     * @return a reference to params.
     *
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws WWRuntimeException       if the Capabilities document does not contain any of the required information.
     */
    public static AVList getWMSLayerParams(Capabilities caps, String[] formatOrderPreference, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WMSCapabilities");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = params.getStringValue(AVKey.STYLE_NAMES);
        if (layerNames == null || layerNames.length() == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] names = layerNames.split(",");
        if (names == null || names.length == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        for (String name : names)
        {
            if (caps.getLayerByName(name) == null)
            {
                String message = Logging.getMessage("WMS.LayerNameMissing", name);
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }
        }

        // Define the DISPLAY_NAME and DATASET_NAME from the WMS layer names and styles.
        params.setValue(AVKey.DISPLAY_NAME, makeTitle(caps, layerNames, styleNames));
        params.setValue(AVKey.DATASET_NAME, layerNames);

        // Get the EXPIRY_TIME from the WMS layer last update time.
        Long lastUpdate = caps.getLayerLatestLastUpdateTime(caps, names);
        if (lastUpdate != null)
            params.setValue(AVKey.EXPIRY_TIME, lastUpdate);

        // Get the GET_MAP_URL from the WMS getMapRequest URL.
        String mapRequestURIString = caps.getGetMapRequestGetURL();
        if (params.getValue(AVKey.GET_MAP_URL) == null)
            params.setValue(AVKey.GET_MAP_URL, mapRequestURIString);
        mapRequestURIString = params.getStringValue(AVKey.GET_MAP_URL);
        // Throw an exception if there's no GET_MAP_URL property, or no getMapRequest URL in the WMS Capabilities.
        if (mapRequestURIString == null || mapRequestURIString.length() == 0)
        {
            Logging.logger().severe("WMS.RequestMapURLMissing");
            throw new WWRuntimeException(Logging.getMessage("WMS.RequestMapURLMissing"));
        }

        // Get the GET_CAPABILITIES_URL from the WMS getCapabilitiesRequest URL.
        String capsRequestURIString = caps.getGetCapabilitiesRequestGetURL();
        if (params.getValue(AVKey.GET_CAPABILITIES_URL) == null)
            params.setValue(AVKey.GET_CAPABILITIES_URL, capsRequestURIString);

        // Define the SERVICE from the GET_MAP_URL property.
        params.setValue(AVKey.SERVICE, params.getValue(AVKey.GET_MAP_URL));
        String serviceURL = params.getStringValue(AVKey.SERVICE);
        if (serviceURL != null)
            params.setValue(AVKey.SERVICE, WWXML.fixGetMapString(serviceURL));

        // Define the SERVICE_NAME as the standard OGC WMS service string.
        if (params.getValue(AVKey.SERVICE_NAME) == null)
            params.setValue(AVKey.SERVICE_NAME, Capabilities.WMS_SERVICE_NAME);

        // Define the WMS VERSION as the version fetched from the Capabilities document.
        String versionString = caps.getVersion();
        if (params.getValue(AVKey.WMS_VERSION) == null)
            params.setValue(AVKey.WMS_VERSION, versionString);

        // Form the cache path DATA_CACHE_NAME from a set of unique WMS parameters.
        if (params.getValue(AVKey.DATA_CACHE_NAME) == null)
        {
            try
            {
                URI mapRequestURI = new URI(mapRequestURIString);
                String cacheName = WWIO.formPath(mapRequestURI.getAuthority(), mapRequestURI.getPath(), layerNames,
                    styleNames);
                params.setValue(AVKey.DATA_CACHE_NAME, cacheName);
            }
            catch (URISyntaxException e)
            {
                String message = Logging.getMessage("WMS.RequestMapURLBad", mapRequestURIString);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
                throw new WWRuntimeException(message);
            }
        }

        // Determine image format to request.
        if (params.getStringValue(AVKey.IMAGE_FORMAT) == null)
        {
            String imageFormat = chooseImageFormat(caps, formatOrderPreference);
            params.setValue(AVKey.IMAGE_FORMAT, imageFormat);
        }

        // Throw an exception if we cannot determine an image format to request.
        if (params.getStringValue(AVKey.IMAGE_FORMAT) == null)
        {
            Logging.logger().severe("WMS.NoImageFormats");
            throw new WWRuntimeException(Logging.getMessage("WMS.NoImageFormats"));
        }

        // Determine bounding sector.
        Sector sector = (Sector) params.getValue(AVKey.SECTOR);
        if (sector == null)
        {
            for (String name : names)
            {
                BoundingBox bb = caps.getLayerGeographicBoundingBox(caps.getLayerByName(name));
                if (bb == null)
                {
                    Logging.logger().log(java.util.logging.Level.SEVERE, "WMS.NoGeographicBoundingBoxForLayer", name);
                    continue;
                }

                sector = Sector.union(sector, Sector.fromDegrees(
                    WWMath.clamp(bb.getMiny(), -90d, 90d),
                    WWMath.clamp(bb.getMaxy(), -90d, 90d),
                    WWMath.clamp(bb.getMinx(), -180d, 180d),
                    WWMath.clamp(bb.getMaxx(), -180d, 180d)));
            }

            if (sector == null)
            {
                Logging.logger().severe("WMS.NoGeographicBoundingBox");
                throw new WWRuntimeException(Logging.getMessage("WMS.NoGeographicBoundingBox"));
            }
            params.setValue(AVKey.SECTOR, sector);
        }

        if (isNonComposableWMSLayer(caps, params))
        {
            getNonComposableWMSLayerParams(caps, params);
        }

        // TODO: adjust for subsetable, fixedimage, etc.

        return params;
    }

    /**
     * Returns true if the layer is a non-composable WMS layer, indicating that GetMap requests must be made by the
     * client against a specific tiling structure. The specified arameter list must contain a non-empty String value
     * under the key {@link AVKey#LAYER_NAMES}, and it must match one of the names layers in the specified Capabilities
     * document.
     *
     * @param caps   the WMS Capabilities document to parse for WMS layer parameters.
     * @param params the output key-value pairs which recieve the WMS layer parameters.
     *
     * @return true if a specified WMS layer is non-composable, false otherwise.
     *
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws WWRuntimeException       if the Capabilities document does not contain any of the required information.
     */
    public static boolean isNonComposableWMSLayer(Capabilities caps, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WMSCapabilities");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        if (layerNames == null || layerNames.length() == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] names = layerNames.split(",");
        if (names == null || names.length == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String layerName = names[0];
        Element layer = caps.getLayerByName(layerName);
        if (layer == null)
        {
            String message = Logging.getMessage("WMS.LayerNameMissing", layerName);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Boolean b = WWXML.getBoolean(layer, "@nonComposable", null);
        return (b != null) && b;
    }

    /**
     * Parses non-composable WMS layer parameters from the WMS {@link Capabilities} document starting at domElement. The
     * specified parameter list must contain a non-empty String value under the key {@link AVKey#LAYER_NAMES}, and it
     * must match one of the names layers in the specified Capabilities document. This writes output as key-value pairs
     * to params. Supported key and parameter names are: <table> <th><td>Key</td><td>Value</td><td>Type</td></th>
     * <tr><td>{@link AVKey#TILE_WIDTH}</td><td>WMS layer's fixedWidth attribute</td><td>Integer</td></tr>
     * <tr><td>{@link AVKey#TILE_HEIGHT}</td><td>WMS layer's fixedHeight attribute</td><td>Integer</td></tr>
     * <tr><td>{@link AVKey#TILE_ORIGIN}</td><td>WMS layer's GeographicTileOrigin element</td><td>LatLon</td></tr>
     * <tr><td>{@link AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>WMS layer's GeographicTileDelta/MaxTileDelta
     * element</td><td>LatLon</td></tr> <tr><td>{@link AVKey#NUM_LEVELS}</td><td>Function of WMS layer's
     * GeographicTileDelta/MaxTileDelta and GeographictileDelta/MinTileDelta elements</td><td>Integer</td></tr>
     * </table>
     *
     * @param caps   the WMS Capabilities document to parse for WMS layer parameters.
     * @param params the output key-value pairs which recieve the WMS layer parameters.
     *
     * @return a reference to params.
     *
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws WWRuntimeException       if the Capabilities document does not contain any of the required information.
     * @see #isNonComposableWMSLayer(gov.nasa.worldwind.wms.Capabilities, gov.nasa.worldwind.avlist.AVList)
     */
    public static AVList getNonComposableWMSLayerParams(Capabilities caps, AVList params)
    {
        if (caps == null)
        {
            String message = Logging.getMessage("nullValue.WMSCapabilities");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        if (layerNames == null || layerNames.length() == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String[] names = layerNames.split(",");
        if (names == null || names.length == 0)
        {
            String message = Logging.getMessage("nullValue.WMSLayerNames");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (names.length > 1)
        {
            String message = Logging.getMessage("WMS.MoreThanOneNonComposableLayer", layerNames);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String layerName = names[0];
        Element layer = caps.getLayerByName(layerName);
        if (layer == null)
        {
            String message = Logging.getMessage("WMS.LayerNameMissing", layerName);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int[] widthAndHeight = getLayerFixedWidthAndHeight(caps, layer);
        if (widthAndHeight == null || widthAndHeight.length != 2)
        {
            String message = Logging.getMessage("WMS.NoFixedWidthOrHeight", layerName);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LatLon tileOrigin = getLayerGeographicTileOrigin(layer);
        if (tileOrigin == null)
        {
            String message = Logging.getMessage("WMS.NoGeographicTileOrigin", layerName);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LatLon minTileDelta = getLayerGeographicMinTileDelta(layer);
        LatLon maxTileDelta = getLayerGeographicMaxTileDelta(layer);
        if (minTileDelta == null || maxTileDelta == null)
        {
            String message = Logging.getMessage("WMS.NoGeographicTileDelta", layerName);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        params.setValue(AVKey.TILE_WIDTH, widthAndHeight[0]);
        params.setValue(AVKey.TILE_HEIGHT, widthAndHeight[1]);

        params.setValue(AVKey.TILE_ORIGIN, tileOrigin);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, maxTileDelta);

        Integer i = computeLayerNumLevels(minTileDelta, maxTileDelta);
        params.setValue(AVKey.NUM_LEVELS, i);

        return params;
    }

    /**
     * Convenience metohd to get the OGC GetCapabilities URL from a specified parameter list. If all the necessary
     * parameters are available, this returns the GetCapabilities URL. Otherwise this returns null.
     *
     * @param params parameter list to get the GetCapabilities parameters from.
     *
     * @return a OGC GetCapabilities URL, or null if the necessary parameters are not available.
     *
     * @throws IllegalArgumentException if the parameter list is null.
     */
    public static URL getOGCGetCapabilitiesURL(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String uri = params.getStringValue(AVKey.GET_CAPABILITIES_URL);
        if (uri == null || uri.length() == 0)
            return null;

        String service = params.getStringValue(AVKey.SERVICE_NAME);
        if (service == null || service.length() == 0)
            return null;

        if (service.equals(Capabilities.WMS_SERVICE_NAME))
            service = "WMS";

        try
        {
            CapabilitiesRequest request = new CapabilitiesRequest(new URI(uri), service);
            return request.getUri().toURL();
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage("generic.URIInvalid", uri);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
        catch (MalformedURLException e)
        {
            String message = Logging.getMessage("generic.URIInvalid", uri);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }

        return null;
    }

    /**
     * Convenience method to get the OGC {@link AVKey#LAYER_NAMES} parameter from a specified parameter list. If the
     * parameter is available as a String, this returns all the OGC layer names found in that String. Otherwise this
     * returns null.
     *
     * @param params parameter list to get the layer names from.
     *
     * @return an array of layer names, or null if none exist.
     *
     * @throws IllegalArgumentException if the parameter list is null.
     */
    public static String[] getOGCLayerNames(AVList params)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String s = params.getStringValue(AVKey.LAYER_NAMES);
        if (s == null || s.length() == 0)
            return null;

        return s.split(",");
    }

    /**
     * Convenience method for parsing parameters about a configuration file in a specified {@link FileStore}. This
     * writes output as key-value pairs to params. If a parameter from the XML document already exists in params, that
     * parameter is ignored. Supported key and parameter names are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td>DataCacheName</td><td>String</td></tr>
     * </table>
     * <p/>
     * The parameter <code>DataCacheName</code> is chosen as the file name's relative parent directory path in the file
     * store. This convention is chosen so that the configuration file sits along side the component's cached data,
     * therefore the cache name points to the directory containing both the configuration file and the contents. This
     * enables the component described by <code>DataCacheName</code> to be moved withing the file store (along with the
     * configuration file), because the <code>DataCacheName</code> parameter is bound at discovery time, and not hard
     * wired in the configuration file itself.
     *
     * @param fileStore          file store which contains the file path.
     * @param dataConfigFileName file store path of the configuration information.
     * @param params             the output key-value pairs which recieve the LevelSet parameters. A null reference is
     *                           permitted.
     *
     * @return a reference to params, or a new AVList if params is null. Returns null if the configuration file cannot
     *         be read.
     *
     * @throws IllegalArgumentException if either the file store or file name is null, or if the configuration file
     *                                  cannot be read for any reason.
     */
    public static AVList getFileStoreParams(FileStore fileStore, String dataConfigFileName, AVList params)
    {
        if (fileStore == null)
        {
            String message = Logging.getMessage("nullValue.FileStoreIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dataConfigFileName == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        String s = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (s == null || s.length() == 0)
        {
            s = WWIO.getParentFilePath(dataConfigFileName);
            if (s != null && s.length() != 0)
                params.setValue(AVKey.DATA_CACHE_NAME, s);
        }

        return params;
    }

    /**
     * Appends LevelSet parameters as elements to a specified context. If a parameter key exists, that parameter is
     * appended to the context. Supported key and element paths are: <table> <th><td>Key</td><td>Name</td><td>Path</td></th>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td>DatasetName</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td>DataCacheName</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SERVICE}</td><td>Service/URL</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>ExpiryTime</td><td>Long</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#EXPIRY_TIME}</td><td>LastUpdate</td><td>Long</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#FORMAT_SUFFIX}</td><td>FormatSuffix</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_LEVELS}</td><td>NumLevels/@count</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#NUM_EMPTY_LEVELS}</td><td>NumLevels/@numEmpty</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#INACTIVE_LEVELS}</td><td>NumLevels/@inactive</td><td>String</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR}</td><td>Sector</td><td>{@link
     * gov.nasa.worldwind.geom.Sector}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#SECTOR_RESOLUTION_LIMITS}</td><td>SectorResolutionLimit</td>
     * <td>{@link LevelSet.SectorResolution}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_ORIGIN}</td><td>TileOrigin/LatLon</td><td>{@link
     * gov.nasa.worldwind.geom.LatLon}</td></tr> <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_WIDTH}</td><td>TileSize/Dimension/@width</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#TILE_HEIGHT}</td><td>TileSize/Dimension/@height</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#LEVEL_ZERO_TILE_DELTA}</td><td>LastUpdate</td><td>LatLon</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MAX_ABSENT_TILE_ATTEMPTS}</td><td>MaxAbsentTileAttempts</td><td>Integer</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MIN_ABSENT_TILE_CHECK_INTERVAL}</td><td>MinAbsentTileCheckInterval</td><td>Integer</td></tr>
     * </table>
     *
     * @param params  the key-value pairs which define the LevelSet parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createLevelSetElements(AVList params, Element context)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null)
        {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Title and cache name properties.
        WWXML.checkAndAppendTextElement(params, AVKey.DATASET_NAME, context, "DatasetName");
        WWXML.checkAndAppendTextElement(params, AVKey.DATA_CACHE_NAME, context, "DataCacheName");

        // Service properties.
        String s = params.getStringValue(AVKey.SERVICE);
        if (s != null && s.length() > 0)
        {
            // The service element may already exist, in which case we want to append the "URL" element to the existing
            // service element.
            Element el = WWXML.getElement(context, "Service", null);
            if (el == null)
                el = WWXML.appendElementPath(context, "Service");
            WWXML.appendText(el, "URL", s);
        }

        // Expiry time properties.
        WWXML.checkAndAppendLongElement(params, AVKey.EXPIRY_TIME, context, "LastUpdate");

        // Image format properties.
        WWXML.checkAndAppendTextElement(params, AVKey.FORMAT_SUFFIX, context, "FormatSuffix");

        // Tile structure properties.
        Integer numLevels = AVListImpl.getIntegerValue(params, AVKey.NUM_LEVELS);
        if (numLevels != null)
        {
            Element el = WWXML.appendElementPath(context, "NumLevels");
            el.setAttribute("count", Integer.toString(numLevels));

            Integer i = AVListImpl.getIntegerValue(params, AVKey.NUM_EMPTY_LEVELS, 0);
            el.setAttribute("numEmpty", Integer.toString(i));

            s = params.getStringValue(AVKey.INACTIVE_LEVELS);
            if (s != null && s.length() > 0)
                el.setAttribute("inactive", s);
        }

        WWXML.checkAndAppendSectorElement(params, AVKey.SECTOR, context, "Sector");
        WWXML.checkAndAppendSectorResolutionElement(params, AVKey.SECTOR_RESOLUTION_LIMITS, context,
            "SectorResolutionLimit");
        WWXML.checkAndAppendLatLonElement(params, AVKey.TILE_ORIGIN, context, "TileOrigin/LatLon");

        Integer tileWidth = AVListImpl.getIntegerValue(params, AVKey.TILE_WIDTH);
        Integer tileHeight = AVListImpl.getIntegerValue(params, AVKey.TILE_HEIGHT);
        if (tileWidth != null && tileHeight != null)
        {
            Element el = WWXML.appendElementPath(context, "TileSize/Dimension");
            el.setAttribute("width", Integer.toString(tileWidth));
            el.setAttribute("height", Integer.toString(tileHeight));
        }

        WWXML.checkAndAppendLatLonElement(params, AVKey.LEVEL_ZERO_TILE_DELTA, context, "LevelZeroTileDelta/LatLon");

        // Retrieval properties.
        if (params.getValue(AVKey.MAX_ABSENT_TILE_ATTEMPTS) != null ||
            params.getValue(AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL) != null)
        {
            Element el = WWXML.getElement(context, "AbsentTiles", null);
            if (el == null)
                el = WWXML.appendElementPath(context, "AbsentTiles");

            WWXML.checkAndAppendIntegerlement(params, AVKey.MAX_ABSENT_TILE_ATTEMPTS, el, "MaxAttempts");
            WWXML.checkAndAppendTimeElement(params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL, el, "MinCheckInterval/Time");
        }

        return context;
    }

    /**
     * Appends WMS layer parameters as elements to a specified context. If a parameter key exists, that parameter is
     * appended to the context. Supported key and element paths are: <table> <th><td>Key</td><td>Name</td><td>Type</td></th>
     * <tr><td>{@link AVKey#WMS_VERSION}</td><td>Service/@version</td><td>String</td></tr> <tr><td>{@link
     * AVKey#LAYER_NAMES}</td><td>Service/LayerNames</td><td>String</td></tr> <tr><td>{@link
     * AVKey#STYLE_NAMES}</td><td>Service/StyleNames</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_MAP_URL}</td><td>Service/GetMapURL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#GET_CAPABILITIES_URL}</td><td>Service/GetCapabilitiesURL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#IMAGE_FORMAT}</td><td>ImageFormat</td><td>String</td></tr> <tr><td>{@link
     * AVKey#SERVICE}</td><td>AVKey#GET_MAP_URL</td><td>String</td></tr> <tr><td>{@link
     * AVKey#DATASET_NAME}</td><td>AVKey.LAYER_NAMES</td><td>String</td></tr> </table>
     *
     * @param params  the key-value pairs which define the WMS layer parameters.
     * @param context the XML document root on which to append parameter elements.
     *
     * @return a reference to context.
     *
     * @throws IllegalArgumentException if either the parameters or the context are null.
     */
    public static Element createWMSLayerElements(AVList params, Element context)
    {
        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (context == null)
        {
            String message = Logging.getMessage("nullValue.ContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        XPath xpath = WWXML.makeXPath();

        // Service properties. The service element may already exist, in which case we want to append the "URL" element
        // to the existing service element.
        Element el = WWXML.getElement(context, "Service", xpath);
        if (el == null)
            el = WWXML.appendElementPath(context, "Service");

        // Try to get the SERVICE_NAME property, but default to "OGC:WMS".
        String s = AVListImpl.getStringValue(params, AVKey.SERVICE_NAME, Capabilities.WMS_SERVICE_NAME);
        if (s != null && s.length() > 0)
            el.setAttribute("serviceName", s);

        s = params.getStringValue(AVKey.WMS_VERSION);
        if (s != null && s.length() > 0)
            el.setAttribute("version", s);

        WWXML.checkAndAppendTextElement(params, AVKey.LAYER_NAMES, el, "LayerNames");
        WWXML.checkAndAppendTextElement(params, AVKey.STYLE_NAMES, el, "StyleNames");
        WWXML.checkAndAppendTextElement(params, AVKey.GET_MAP_URL, el, "GetMapURL");
        WWXML.checkAndAppendTextElement(params, AVKey.GET_CAPABILITIES_URL, el, "GetCapabilitiesURL");

        // Since this is a WMS tiled image layer, we want to express the service URL as a GetMap URL. If we have a
        // GET_MAP_URL property, then remove any existing SERVICE property from the DOM document.
        s = params.getStringValue(AVKey.GET_MAP_URL);
        if (s != null && s.length() > 0)
        {
            Element urlElem = WWXML.getElement(context, "Service/URL", xpath);
            if (urlElem != null)
                el.removeChild(urlElem);
        }

        // Image format properties.
        WWXML.checkAndAppendTextElement(params, AVKey.IMAGE_FORMAT, context, "ImageFormat");

        return context;
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetStringParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String s = params.getStringValue(paramKey);
        if (s == null)
        {
            s = context.getString(paramName);
            if (s != null && s.length() > 0)
                params.setValue(paramKey, s);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetStringArrayParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            String[] strings = context.getStringArray(paramName);
            if (strings != null && strings.length > 0)
                params.setValue(paramKey, strings);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetUniqueStringsParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            String[] strings = context.getUniqueStrings(paramName);
            if (strings != null && strings.length > 0)
                params.setValue(paramKey, strings);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetDoubleParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Double d = context.getDouble(paramName);
            if (d != null)
                params.setValue(paramKey, d);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetIntegerParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Integer i = context.getInteger(paramName);
            if (i != null)
                params.setValue(paramKey, i);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetLongParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Long l = context.getLong(paramName);
            if (l != null)
                params.setValue(paramKey, l);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetBooleanParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Boolean b = context.getBoolean(paramName);
            if (b != null)
                params.setValue(paramKey, b);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetLatLonParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            LatLon ll = context.getLatLon(paramName);
            if (ll != null)
                params.setValue(paramKey, ll);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetSectorParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Sector sector = context.getSector(paramName);
            if (sector != null)
                params.setValue(paramKey, sector);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetSectorResolutionParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            DataConfiguration[] configs = context.getChildren(paramName);
            if (configs == null || configs.length == 0)
                return;

            LevelSet.SectorResolution[] srs = new LevelSet.SectorResolution[configs.length];

            for (int i = 0; i < configs.length; i++)
            {
                LevelSet.SectorResolution sr = configs[i].getSectorResolutionLimit(null);
                if (sr != null)
                    srs[i] = sr;
            }

            params.setValue(paramKey, srs);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetTimeParam(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Long l = context.getTimeInMillis(paramName);
            if (l != null)
                params.setValue(paramKey, l);
        }
    }

    /**
     * Checks a parameter list for a specified key and if not present attempts to find a value for the key from an
     * parameter matching a specified name. If found, the key and value are added to the parameter list.
     *
     * @param context   the context in which look for the parameter.
     * @param params    the parameter list.
     * @param paramKey  the key used to identify the paramater in the parameter list.
     * @param paramName the parameter name identifying the parameter value within the specified context.
     *
     * @throws IllegalArgumentException if either the context, parameter list, parameter key or parameter name are
     *                                  null.
     */
    public static void checkAndSetTimeParamAsInteger(DataConfiguration context, AVList params, String paramKey,
        String paramName)
    {
        if (context == null)
        {
            String message = Logging.getMessage("nullValue.DataConfigurationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
        {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramKey == null)
        {
            String message = Logging.getMessage("nullValue.ParameterKeyIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (paramName == null)
        {
            String message = Logging.getMessage("nullValue.ParameterNameIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Object o = params.getValue(paramKey);
        if (o == null)
        {
            Long l = context.getTimeInMillis(paramName);
            if (l != null)
                params.setValue(paramKey, l.intValue());
        }
    }

    protected static int getNumEmptyLevels(LevelSet levelSet)
    {
        int i;
        for (i = 0; i < levelSet.getNumLevels(); i++)
        {
            if (!levelSet.getLevel(i).isEmpty())
                break;
        }

        return i;
    }

    protected static String getInactiveLevels(LevelSet levelSet)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levelSet.getNumLevels(); i++)
        {
            if (!levelSet.getLevel(i).isActive())
            {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(i);
            }
        }

        return (sb.length() > 0) ? sb.toString() : null;
    }

    protected static String chooseImageFormat(Capabilities caps, String[] formatOrderPreference)
    {
        String[] formats = caps.getGetMapFormats();
        if (formats == null || formats.length == 0)
            return null;

        // No preferred formats specified; just use the first in the caps list.
        if (formatOrderPreference == null || formatOrderPreference.length == 0)
            return formats[0];

        for (String s : formatOrderPreference)
        {
            for (String f : formats)
            {
                if (f.equalsIgnoreCase(s))
                    return f;
            }
        }

        return formats[0]; // No preferred formats recognized; just use the first in the caps list.
    }

    protected static String makeTitle(Capabilities caps, String layerNames, String styleNames)
    {
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++)
        {
            if (sb.length() > 0)
                sb.append(", ");

            String layerName = lNames[i];
            Element layer = caps.getLayerByName(layerName);
            String layerTitle = caps.getLayerTitle(layer);
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i)
                continue;

            String styleName = sNames[i];
            Element style = caps.getLayerStyleByName(layer, styleName);
            if (style == null)
                continue;

            sb.append(" : ");
            String styleTitle = caps.getStyleTitle(layer, style);
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }

    protected static LatLon getLayerGeographicTileOrigin(Element layer)
    {
        return getLayerLatLon(layer, "GeographicTileOrigin");
    }

    protected static LatLon getLayerGeographicMinTileDelta(Element layer)
    {
        return getLayerLatLon(layer, "GeographicTileDelta/MinTileDelta");
    }

    protected static LatLon getLayerGeographicMaxTileDelta(Element layer)
    {
        return getLayerLatLon(layer, "GeographicTileDelta/MaxTileDelta");
    }

    protected static int[] getLayerFixedWidthAndHeight(Capabilities caps, Element layer)
    {
        Integer width;
        Integer height;

        String s = caps.getLayerFixedWidth(layer);
        if (s == null)
            return null;

        width = WWUtil.convertStringToInteger(s);
        if (width == null)
            return null;

        s = caps.getLayerFixedHeight(layer);
        if (s == null)
            return null;

        height = WWUtil.convertStringToInteger(s);
        if (height == null)
            return null;

        return new int[] {width, height};
    }

    protected static LatLon getLayerLatLon(Element layer, String path)
    {
        XPath xpath = WWXML.makeXPath();

        Element el = WWXML.getElement(layer, path, xpath);
        if (el == null)
            return null;

        Double latDegrees = WWXML.getDouble(el, "Latitude", xpath);
        Double lonDegrees = WWXML.getDouble(el, "Longitude", xpath);
        if (latDegrees == null || lonDegrees == null)
            return null;

        return LatLon.fromDegrees(latDegrees, lonDegrees);
    }

    protected static int computeLayerNumLevels(LatLon minDelta, LatLon maxDelta)
    {
        return Math.max(
            computeLayerNumLevels(minDelta.getLatitude(), maxDelta.getLatitude()),
            computeLayerNumLevels(minDelta.getLongitude(), maxDelta.getLongitude()));
    }

    protected static int computeLayerNumLevels(Angle minDelta, Angle maxDelta)
    {
        double log2MinDelta = WWMath.logBase2(minDelta.getDegrees());
        double log2MaxDelta = WWMath.logBase2(maxDelta.getDegrees());
        return 1 + (int) Math.round(log2MaxDelta - log2MinDelta);
    }
}
