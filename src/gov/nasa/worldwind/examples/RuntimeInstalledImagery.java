/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.*;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.*;

import java.io.File;

/**
 * @author tag
 * @version $Id: RuntimeInstalledImagery.java 8329 2009-01-05 21:29:51Z dcollins $
 */
public class RuntimeInstalledImagery extends ApplicationTemplate
{
    private static final String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
    private static final String GEORSS_ICON_PATH = "images/georss.png";
    private static final String NASA_ICON_PATH = "images/32x32-icon-nasa.png";
    private static final String INSTALL_PATH = "RuntimeInstalledImagery/";

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, false);

            try
            {
                DataSource ds1 = createDataSource(WWJ_SPLASH_PATH, Sector.fromDegrees(35, 45, -115, -95));
                DataSource ds2 = createDataSource(GEORSS_ICON_PATH, Sector.fromDegrees(25, 33, -120, -110));
                DataSource ds3 = createRasterDataSource(NASA_ICON_PATH, Sector.fromDegrees(25, 35, -100, -90));

                DataDescriptor dd1 = produceTiledImagery("World Wind Splash Image", INSTALL_PATH + WWJ_SPLASH_PATH, ds1);
                DataDescriptor dd2 = produceTiledImagery("GeoRSS Icon", INSTALL_PATH + GEORSS_ICON_PATH, ds2);
                DataDescriptor dd3 = produceTiledImagery("NASA Icon", INSTALL_PATH + NASA_ICON_PATH, ds3);

                Layer layer1 = createImageLayer(dd1);
                Layer layer2 = createImageLayer(dd2);
                Layer layer3 = createImageLayer(dd3);

                insertBeforeCompass(this.getWwd(), layer3);
                insertBeforeCompass(this.getWwd(), layer2);
                insertBeforeCompass(this.getWwd(), layer1);

                this.getLayerPanel().update(this.getWwd());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private static Layer createImageLayer(DataDescriptor dataDescriptor)
    {
        if (!AVKey.TILED_IMAGERY.equals(dataDescriptor.getType()))
            return null;

        TiledImageLayer layer = new BasicTiledImageLayer(dataDescriptor);
        layer.setNetworkRetrievalEnabled(false);
        layer.setUseTransparentTextures(true);
        if (dataDescriptor.getName() != null)
            layer.setName(dataDescriptor.getName());

        return layer;
    }

    private static DataSource createDataSource(String filePath, Sector sector) throws Exception
    {
        // Create a DataSource from the given filePath and sector.
        DataSource dataSource = new BasicDataSource(filePath);
        dataSource.setValue(AVKey.SECTOR, sector);
        return dataSource;
    }

    private static DataSource createRasterDataSource(String filePath, Sector sector) throws Exception
    {
        // Read the filePath as a DataRaster using ImageIO, and return a DataSource that points to the
        // in-memory DataRaster.

        DataSource fileDataSource = createDataSource(filePath, sector);
        DataRasterReader reader = new ImageIORasterReader();
        DataRaster[] rasters = reader.read(fileDataSource);
        return new BasicDataSource(rasters[0]);
    }

    private static DataDescriptor produceTiledImagery(String name, String installPath, DataSource dataSource) throws Exception
    {
        String installLocation = getDefaultInstallLocation();

        // If a DataDescriptor already exists at the appropriate install location, then bypass tile production
        // and return the DataDescriptor reference.
        DataDescriptor dataDescriptor = findDataDescriptor(installLocation, installPath);
        if (dataDescriptor != null)
            return dataDescriptor;
        
        // Create the production parameters. This will tell the producer where to install the raster tiles,
        // and what name to tag the metadata with.
        AVList params = new AVListImpl();
        params.setValue(AVKey.FILE_STORE_LOCATION, installLocation);
        params.setValue(AVKey.DATA_CACHE_NAME, installPath);
        params.setValue(AVKey.DATASET_NAME, name);

        // Creating tiled imagery with the production parameters and DataSource.
        DataStoreProducer producer = new TiledImageProducer();
        producer.setStoreParameters(params);
        producer.offerDataSource(dataSource);
        producer.startProduction();

        // Extract the production results as a DataDescriptor.
        Iterable<?> results = producer.getProductionResults();
        if (!results.iterator().hasNext())
            return null;

        Object o = results.iterator().next();
        return (o != null && o instanceof DataDescriptor) ? (DataDescriptor) o : null;
    }

    private static String getDefaultInstallLocation()
    {
        // Try to find the first valid install file store location.
        FileStore fileStore = WorldWind.getDataFileStore();
        for (File location : fileStore.getLocations())
        {
            if (fileStore.isInstallLocation(location.getPath()))
            {
                // If the location does not exist, then attempt to create it.
                if (!location.exists())
                    if (!location.mkdirs())
                        continue;

                return location.getPath();
            }
        }

        return null;
    }

    private static DataDescriptor findDataDescriptor(String installLocation, String path)
    {
        // Attempt to find an installed DataDescriptor under the specified installLocation, and with the specified
        // cache path.

        File thisFile = new File(installLocation, path);
        try
        {
            for (DataDescriptor dataDescriptor : WorldWind.getDataFileStore().findDataDescriptors(installLocation))
            {
                File thatFile = new File(dataDescriptor.getFileStoreLocation(), dataDescriptor.getFileStorePath());
                if (thisFile.equals(thatFile))
                    return dataDescriptor;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Installed Surface Images", RuntimeInstalledImagery.AppFrame.class);
    }
}
