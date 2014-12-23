/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.formats.dds.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

// TODO: support for LayerSet.xsd format nested descriptors
/**
 * @author dcollins
 * @version $Id: WWDotNetLayerSetInstaller.java 8941 2009-02-21 00:33:27Z dcollins $
 */
public class WWDotNetLayerSetInstaller extends AbstractDataStoreProducer
{
    static class ProductionState
    {
        // Production parameters.
        AVList productionParams;
        // Progress counters.
        int numSources;
        int curSource;
        int[] numSourceFiles;
        int[] numInstalledFiles;
    }

    public WWDotNetLayerSetInstaller()
    {
    }

    public String getDataSourceDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("DataStoreProducer.WWDotNetLayerSet.Description"));

        DataDescriptorReader reader = new WWDotNetLayerSetReader();
        String suffix = WWIO.makeSuffixForMimeType(reader.getMimeType());
        sb.append(" (").append("*").append(suffix).append(")");

        return sb.toString();
    }

    protected void doStartProduction(AVList parameters) throws Exception
    {
        this.getProductionResultsList().clear();
        Iterable<DataSource> dataSources = this.getDataSourceList();
        ProductionState productionState = new ProductionState();

        // Initialize any missing production parameters with suitable defaults.
        this.initProductionParameters(parameters, productionState);
        // Set the progress parameters for the current data sources.
        this.setProgressParameters(dataSources, productionState);

        for (DataSource source : dataSources)
        {
            productionState.curSource++;
            this.install(source, productionState);
        }
    }

    protected void initProductionParameters(AVList parameters, ProductionState productionState)
    {
        Object o = parameters.getValue(AVKey.MIME_TYPE);
        if (o == null || !(o instanceof String))
            parameters.setValue(AVKey.MIME_TYPE, "image/dds");

        productionState.productionParams = parameters;
    }

    protected String validateProductionParameters(AVList parameters)
    {
        StringBuilder sb = new StringBuilder();

        Object o = parameters.getValue(AVKey.FILE_STORE_LOCATION);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1)
            sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreLocation"));

        o = parameters.getValue(AVKey.DATA_CACHE_NAME);
        // It's okay if the cache path is empty, but if specified it must be a String.
        if (o != null && !(o instanceof String))
            sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreFolder"));

        if (sb.length() == 0)
            return null;

        return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", sb.toString());
    }

    protected String validateDataSource(DataSource dataSource)
    {
        StringBuilder sb = new StringBuilder();

        if (dataSource.getSource() instanceof DataDescriptor)
        {
            DataDescriptor dataDescriptor = (DataDescriptor) dataSource.getSource();
            if (!dataDescriptor.hasKey(AVKey.WORLD_WIND_DOT_NET_LAYER_SET))
                sb.append((sb.length() > 0 ? ", " : "")).append("DataDescriptor not a World Wind .NET LayerSet: ")
                        .append(dataSource);
        }
        else
        {
            try
            {
                WWDotNetLayerSetReader reader = new WWDotNetLayerSetReader();
                reader.setSource(dataSource.getSource());
                if (!reader.canRead())
                    sb.append((sb.length() > 0 ? ", " : "")).append("source not a World Wind .NET LayerSet: ")
                            .append(dataSource);
            }
            catch (java.io.IOException e)
            {
                sb.append((sb.length() > 0 ? ", " : "")).append("source not readable: ").append(dataSource);
            }
        }

        if (sb.length() == 0)
            return null;

        return Logging.getMessage("DataStoreProducer.InvalidDataSource", sb.toString());
    }

    //**************************************************************//
    //********************  LayerSet Installation  *****************//
    //**************************************************************//

    // TODO
    // DONE (1) back out all changes if anything fails
    // DONE (2) convert .NET filenames
    // DONE (3) convert to app-specified image format
    // DONE (4) Write descriptor xml
    // (5) Descriptive logging
    protected void install(DataSource dataSource, ProductionState productionState) throws java.io.IOException
    {
        DataDescriptor dataDescriptor;
        java.io.File sourceLocation;
        java.io.File installLocation;
        java.io.File installDescriptor;
        String installMimeType = null;
        AVList installParams = productionState.productionParams;
        
        Object result = this.readSource(dataSource);
        if (result instanceof DataDescriptor)
        {
            dataDescriptor = (DataDescriptor) result;            
        }
        else
        {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        result = this.sourceLocationFor(dataSource, dataDescriptor);
        if (result instanceof java.io.File)
        {
            sourceLocation = (java.io.File) result;
        }
        else
        {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        result = this.installLocationFor(installParams, dataSource, dataDescriptor);
        if (result instanceof java.io.File)
        {
            installLocation = (java.io.File) result;
            installDescriptor = new java.io.File(installLocation, this.filenameFor(dataDescriptor));

            if (WWIO.isAncestorOf(sourceLocation, installLocation))
            {
                String message = Logging.getMessage("DataStoreProducer.CannotInstallInto",
                    sourceLocation, installLocation);
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }
            else if (WWIO.isAncestorOf(installLocation, sourceLocation))
            {
                String message = Logging.getMessage("DataStoreProducer.CannotInstallInto",
                    installLocation, sourceLocation);
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }
        }
        else
        {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        try
        {
            result = installParams.getValue(AVKey.MIME_TYPE);
            if (result != null)
            {
                installMimeType = result.toString();
                dataDescriptor.setValue(AVKey.FORMAT_SUFFIX, WWIO.makeSuffixForMimeType(installMimeType));
            }

            productionState.numSourceFiles[productionState.curSource] = this.countWWDotNetFiles(sourceLocation);

            this.installWWDotNetDiretory(sourceLocation, installLocation, installMimeType, productionState);

            if (productionState.numSourceFiles[productionState.curSource] != productionState.numInstalledFiles[productionState.curSource])
            {
                String message = Logging.getMessage("DataStoreProducer.IncompleteInstallation", installLocation);
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }
        }
        catch (java.io.IOException e)
        {
            // Back out all file system changes made so far.
            WWIO.deleteDirectory(installLocation);
            
            String message = Logging.getMessage("generic.ExceptionWhileWriting", installLocation);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            throw e;
        }

        try
        {
            WWDotNetLayerSetWriter writer = new WWDotNetLayerSetWriter();
            writer.setDestination(installDescriptor);
            writer.write(dataDescriptor);
        }
        catch (java.io.IOException e)
        {
            // Back out all file system changes made so far.
            WWIO.deleteDirectory(installLocation);
            //noinspection ResultOfMethodCallIgnored
            installDescriptor.delete();

            String message = Logging.getMessage("generic.ExceptionWhileWriting", installDescriptor);
            Logging.logger().severe(message);
            throw e;
        }

        this.getProductionResultsList().add(dataDescriptor);
    }

    protected Object readSource(DataSource dataSource)
    {
        Object result;

        try
        {
            WWDotNetLayerSetReader reader = new WWDotNetLayerSetReader();
            reader.setSource(dataSource.getSource());
            result = reader.read();
        }
        catch (java.io.IOException e)
        {
            result = Logging.getMessage("generic.ExceptionWhileReading", dataSource);
        }

        return result;
    }

    protected Object sourceLocationFor(DataSource dataSource, DataDescriptor descriptor)
    {
        Object result;

        if (dataSource.getSource() instanceof java.io.File)
        {
            java.io.File file = (java.io.File) dataSource.getSource();
            result = file.getParentFile();
            if (result == null)
            {
                result = Logging.getMessage("DataStoreProducer.FileWithoutParent", dataSource);
            }
        }
        else
        {
            String s = descriptor.getStringValue(AVKey.WORLD_WIND_DOT_NET_PERMANENT_DIRECTORY);
            if (s != null)
            {
                java.io.File file = new java.io.File(s);
                if (file.exists())
                {
                    result = file;
                }
                else
                {
                    result = Logging.getMessage("DataStoreProducer.WWDotNetPermanentDirectoryInvalid", s);
                }
            }
            else
            {
                result = Logging.getMessage("DataStoreProducer.WWDotNetPermanentDirectoryMissing", dataSource);
            }
        }

        return result;
    }

    protected Object installLocationFor(AVList installParams, DataSource dataSource, DataDescriptor descriptor)
    {
        String path = null;

        String s = installParams.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (s != null)
            path = appendPathPart(path, s);

        s = installParams.getStringValue(AVKey.DATA_CACHE_NAME);
        if (s != null)
            path = appendPathPart(path, s);

        if (descriptor != null)
            if (descriptor.getName() != null)
                path = appendPathPart(path, descriptor.getName());
        
        if (path == null || path.length() < 1)
            return Logging.getMessage("DataStoreProducer.InvalidDataSource", dataSource);

        return new java.io.File(path);
    }

    protected String filenameFor(DataDescriptor descriptor)
    {
        String name = descriptor.getStringValue(AVKey.DATASET_NAME);
        if (name != null)
            name = WWIO.replaceSuffix(name, ".xml");
        if (name == null)
            name = "WorldWindDotNetLayerSet.xml";
        return WWIO.formPath(name);
    }

    private static String appendPathPart(String firstPart, String secondPart)
    {
        if (secondPart == null || secondPart.length() == 0)
            return firstPart;
        if (firstPart == null || firstPart.length() == 0)
            return secondPart;

        firstPart = WWIO.stripTrailingSeparator(firstPart);
        secondPart = WWIO.stripLeadingSeparator(secondPart);

        return firstPart + System.getProperty("file.separator") + secondPart;
    }

    //**************************************************************//
    //********************  Imagery Installation  ******************//
    //**************************************************************//

    private void installWWDotNetDiretory(java.io.File source, java.io.File destination, String installMimeType,
                                         ProductionState productionState) throws java.io.IOException
    {
        if (!destination.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            destination.mkdirs();
        }

        if (!destination.exists())
        {
            String message = Logging.getMessage("generic.CannotCreateFile", destination);
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }

        java.io.File[] fileList = source.listFiles();
        if (fileList == null)
            return;

        java.util.List<java.io.File> childFiles = new java.util.ArrayList<java.io.File>();
        java.util.List<java.io.File> childDirs = new java.util.ArrayList<java.io.File>();
        for (java.io.File child : fileList)
        {
            if (child == null) // Don't allow null subfiles.
                continue;

            if (child.isHidden()) // Ignore hidden files.
                continue;

            if (child.isDirectory())
                childDirs.add(child);
            else
                childFiles.add(child);
        }

        for (java.io.File childFile : childFiles)
        {
            if (!isWWDotNetFile(childFile))
                continue;

            java.io.File destFile = makeWWJavaFile(destination, childFile.getName(), installMimeType);
            this.installWWDotNetFile(childFile, destFile, productionState);

            if (!destFile.exists())
            {
                String message = Logging.getMessage("generic.CannotCreateFile", destFile);
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }
        }

        for (java.io.File childDir : childDirs)
        {
            if (!isWWDotNetDirectory(childDir))
                continue;

            java.io.File destDir = makeWWJavaDirectory(destination, childDir.getName());
            this.installWWDotNetDiretory(childDir, destDir, installMimeType, productionState);
        }
    }

    private void installWWDotNetFile(java.io.File source, java.io.File destination, ProductionState productionState)
                                     throws java.io.IOException
    {
        // Bypass file installation if:
        // (a) destination is newer than source, and
        // (b) source and destination have identical size.
        if (destination.exists() && source.lastModified() >= destination.lastModified()
                && source.length() == destination.length())
        {
            return;
        }

        String sourceSuffix = WWIO.getSuffix(source.getName());
        String destinationSuffix = WWIO.getSuffix(destination.getName());

        // Source and destination types match. Copy the source file directly.
        if (sourceSuffix.equalsIgnoreCase(destinationSuffix))
        {
            WWIO.copyFile(source, destination);
        }
        // Destination type is different. Convert the source file and write the converstion to the destionation.
        else
        {
            if (destinationSuffix.equalsIgnoreCase("dds"))
            {
                java.nio.ByteBuffer sourceBuffer = DDSCompressor.compressImageFile(source);
                WWIO.saveBuffer(sourceBuffer, destination);
            }
            else
            {
                java.awt.image.BufferedImage sourceImage = javax.imageio.ImageIO.read(source);
                javax.imageio.ImageIO.write(sourceImage, destinationSuffix, destination);
            }
        }

        this.updateProgress(productionState);
    }

    private static java.io.File makeWWJavaDirectory(java.io.File dir, String dirname)
    {
        return new java.io.File(dir, WWIO.stripLeadingZeros(dirname));
    }

    private static java.io.File makeWWJavaFile(java.io.File dir, String filename, String installMimeType)
    {
        // If the filename does not match the standard pattern, then return a file with that name.
        String[] tokens = filename.split("[._]");
        if (tokens == null || tokens.length < 3 || tokens[0].length() < 1 || tokens[1].length() < 1)
            return new java.io.File(dir, filename);

        // If an installation type is specified, override the file extension with the new type.
        if (installMimeType != null)
            tokens[2] = WWIO.makeSuffixForMimeType(installMimeType);
            // Otherwise keep the existing extension. Add a leading '.' so that both cases can be handled transparently.
        else if (tokens[2].length() > 1)
            tokens[2] = "." + tokens[2];

        // If the filename is "000n_000m.foo", then the contents of tokens[] are:
        // tokens[0] = "000n"
        // tokens[1] = "000m"
        // tokens[2] = "foo"
        StringBuilder sb = new StringBuilder();
        sb.append(WWIO.stripLeadingZeros(tokens[0])).append("_").append(WWIO.stripLeadingZeros(tokens[1]));
        sb.append(tokens[2]);
        return new java.io.File(dir, sb.toString());
    }

    private static boolean isWWDotNetDirectory(java.io.File file)
    {
        String pattern = "\\d+";
        return file.getName().matches(pattern);
    }

    private static boolean isWWDotNetFile(java.io.File file)
    {
        String pattern = "\\d+[_]\\d+[.]\\w+";
        return file.getName().matches(pattern);
    }

    //**************************************************************//
    //********************  Progress and Verification  *************//
    //**************************************************************//

    private int countWWDotNetFiles(java.io.File source)
    {
        int count = 0;

        java.io.File[] fileList = source.listFiles();
        if (fileList == null)
            return count;

        java.util.List<java.io.File> childFiles = new java.util.ArrayList<java.io.File>();
        java.util.List<java.io.File> childDirs = new java.util.ArrayList<java.io.File>();
        for (java.io.File child : fileList)
        {
            if (child == null) // Don't allow null subfiles.
                continue;

            if (child.isHidden()) // Ignore hidden files.
                continue;

            if (child.isDirectory())
                childDirs.add(child);
            else
                childFiles.add(child);
        }

        for (java.io.File childFile : childFiles)
        {
            if (!isWWDotNetFile(childFile))
                continue;

            count++;
        }

        for (java.io.File childDir : childDirs)
        {
            if (!isWWDotNetDirectory(childDir))
                continue;

            count += countWWDotNetFiles(childDir);
        }

        return count;
    }

    //**************************************************************//
    //********************  Progress Parameters  *******************//
    //**************************************************************//

    protected void setProgressParameters(Iterable<?> dataSources, ProductionState productionState)
    {
        int numSources = 0;
        //noinspection UnusedDeclaration
        for (Object o : dataSources)
            numSources++;

        productionState.numSources = numSources;
        productionState.curSource = -1;
        productionState.numSourceFiles = new int[numSources];
        productionState.numInstalledFiles = new int[numSources];
    }

    private void updateProgress(ProductionState productionState)
    {
        double oldProgress = this.computeProgress(productionState);
        productionState.numInstalledFiles[productionState.curSource]++;
        double newProgress = this.computeProgress(productionState);

        this.firePropertyChange(AVKey.PROGRESS, oldProgress, newProgress);
    }

    private double computeProgress(ProductionState productionState)
    {
        double progress = 0.0;
        for (int i = 0; i <= productionState.curSource; i++)
        {
            progress += (productionState.numInstalledFiles[i] /
                (double) productionState.numSourceFiles[i]) * (1.0 / (double) productionState.numSources);
        }
        return progress;
    }
}
