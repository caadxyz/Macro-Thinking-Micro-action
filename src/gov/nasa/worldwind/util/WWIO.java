/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

/**
 * @author Tom Gaskins
 * @version $Id: WWIO.java 12789 2009-11-14 03:54:49Z tgaskins $
 */
package gov.nasa.worldwind.util;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.exception.WWRuntimeException;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.zip.*;

public class WWIO
{
    public static final String DELETE_ON_EXIT_PREFIX = "WWJDeleteOnExit";
    public static final String ILLEGAL_FILE_PATH_PART_CHARACTERS = "[" + "?/\\\\=+<>:;\\,\"\\|^\\[\\]" + "]";

    public static String formPath(String... pathParts)
    {
        StringBuilder sb = new StringBuilder();

        for (String pathPart : pathParts)
        {
            if (pathPart == null)
                continue;

            if (sb.length() > 0)
                sb.append(File.separator);
            sb.append(pathPart.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_"));
        }

        return sb.toString();
    }

    public static String stripIllegalFileNameCharacters(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return s.replaceAll(ILLEGAL_FILE_PATH_PART_CHARACTERS, "_");
    }

    public static String stripTrailingSeparator(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (s.endsWith("/") || s.endsWith("\\"))
            return s.substring(0, s.length() - 1);
        else
            return s;
    }

    public static String stripLeadingSeparator(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (s.startsWith("/") || s.startsWith("\\"))
            return s.substring(1, s.length());
        else
            return s;
    }

    public static String stripLeadingZeros(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int len = s.length();
        if (len < 2) // String is empty or is a single character, so there is nothing to strip.
            return s;

        int i = 0;
        while (i < len && s.charAt(i) == '0')
        {
            i++;
        }
        if (i == len) // String is just '0' characters. Leave the last one.
            i = len - 1;

        if (i == 0) // String doesn't contain any '0' characters, return the original string.
            return s;

        return s.substring(i, len); // String contains at least one leading '0' character.
    }

    /**
     * Converts a specified URL as to a path in the local file system. If the URL cannot be converted to a file path for
     * any reason, this returns null.
     *
     * @param url the URL to convert to a local file path.
     *
     * @return a local File path, or null if the URL could not be converted.
     *
     * @throws IllegalArgumentException if the url is null.
     */
    public static java.io.File convertURLToFile(java.net.URL url)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            return new java.io.File(url.toURI());
        }
        catch (java.net.URISyntaxException e)
        {
            String message = Logging.getMessage("generic.CannotConvertURLToFile", url.toString());
            Logging.logger().log(Level.SEVERE, message, e);
        }

        return null;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static boolean saveBuffer(ByteBuffer buffer, File file, boolean forceFilesystemWrite) throws IOException
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileOutputStream fos = null;
        FileChannel channel = null;
        FileLock lock;
        int numBytesWritten = 0;
        try
        {
            fos = new FileOutputStream(file);
            channel = fos.getChannel();
            lock = channel.tryLock();
            if (lock == null)
            {
                // The file is being written to, or some other process is keeping it to itself.
                // This is an okay condition, but worth noting.
                Logging.logger().log(Level.FINER, "WWIO.UnableToAcquireLockFor", file.getPath());
                return false;
            }

            for (buffer.rewind(); buffer.hasRemaining();)
            {
                numBytesWritten += channel.write(buffer);
            }

            // Optionally force writing to the underlying storage device. Doing so ensures that all contents are
            // written to the device (and not in the I/O cache) in the event of a system failure.
            if (forceFilesystemWrite)
                channel.force(true);
            fos.flush();
            return true;
        }
        catch (ClosedByInterruptException e)
        {
            Logging.logger().log(Level.FINE,
                Logging.getMessage("generic.interrupted", "WWIO.saveBuffer", file.getPath()), e);

            if (numBytesWritten > 0) // don't leave behind incomplete files
                file.delete();

            throw e;
        }
        catch (IOException e)
        {
            Logging.logger().log(Level.SEVERE, Logging.getMessage("WWIO.ErrorSavingBufferTo", file.getPath()), e);

            if (numBytesWritten > 0) // don't leave behind incomplete files
                file.delete();

            throw e;
        }
        finally
        {
            WWIO.closeStream(channel, file.getPath()); // also releases the lock
            WWIO.closeStream(fos, file.getPath());
        }
    }

    public static boolean saveBuffer(ByteBuffer buffer, File file) throws IOException
    {
        // By default, force changes to be written to the underlying storage device.
        return saveBuffer(buffer, file, true);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static boolean saveBufferToStream(ByteBuffer buffer, OutputStream fos)
        throws IOException
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (fos == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        WritableByteChannel channel;
        try
        {
            channel = Channels.newChannel(fos);

            for (buffer.rewind(); buffer.hasRemaining();)
            {
                channel.write(buffer);
            }

            fos.flush();
            return true;
        }
        finally
        {
            WWIO.closeStream(fos, null);
        }
    }

    public static MappedByteBuffer mapFile(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream is = new FileInputStream(file);
        try
        {
            return is.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        }
        finally
        {
            WWIO.closeStream(is, file.getPath());
        }
    }

    /**
     * Reads all the bytes from the specified {@link URL}, returning the bytes as a non-direct {@link ByteBuffer} with
     * the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param url the URL to read.
     *
     * @return the bytes from the specified URL, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the URL is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readURLContentToBuffer(URL url) throws IOException
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return readURLContentToBuffer(url, false);
    }

    /**
     * Reads all the bytes from the specified {@link URL}, returning the bytes as a {@link ByteBuffer} with the current
     * JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a non-direct ByteBuffer
     * otherwise. Direct buffers are backed by native memory, and may resite outside of the normal garbage-collected
     * heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param url            the URL to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the bytes from the specified URL, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the URL is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readURLContentToBuffer(URL url, boolean allocateDirect) throws IOException
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = url.openStream();
        return readStreamToBuffer(is, allocateDirect);
    }

    /**
     * Reads all the bytes from the specified {@link File}, returning the bytes as a non-direct {@link ByteBuffer} with
     * the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param file the file to read.
     *
     * @return the bytes from the specified file, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the file is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readFileToBuffer(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return readFileToBuffer(file, false);
    }

    /**
     * Reads all the bytes from the specified {@link File}, returning the bytes as a {@link ByteBuffer} with the current
     * JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a non-direct ByteBuffer
     * otherwise. Direct buffers are backed by native memory, and may reside outside of the normal garbage-collected
     * heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param file           the file to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the bytes from the specified file, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the file is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readFileToBuffer(File file, boolean allocateDirect) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream is = new FileInputStream(file);
        try
        {
            FileChannel fc = is.getChannel();
            int size = (int) fc.size();
            ByteBuffer buffer = allocateDirect ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
            for (int count = 0; count >= 0 && buffer.hasRemaining();)
            {
                count = fc.read(buffer);
            }
            buffer.flip();
            return buffer;
        }
        finally
        {
            WWIO.closeStream(is, file.getPath());
        }
    }

    public static ByteBuffer inflateFileToBuffer(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream is = new FileInputStream(file);
        try
        {
            return inflateStreamToBuffer(is);
        }
        finally
        {
            WWIO.closeStream(is, file.getPath());
        }
    }

    public static void deflateBufferToFile(ByteBuffer buffer, File file) throws IOException
    {
        saveBufferToStream(buffer, new DeflaterOutputStream(new FileOutputStream(file)));
    }

    public static ByteBuffer readZipEntryToBuffer(File zipFile, String entryName) throws IOException
    {
        if (zipFile == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        InputStream is = null;
        ZipEntry ze;
        try
        {
            ZipFile zf = new ZipFile(zipFile);
            if (zf.size() < 1)
            {
                String message = Logging.getMessage("WWIO.ZipFileIsEmpty", zipFile.getPath());
                Logging.logger().severe(message);
                throw new java.io.IOException(message);
            }

            if (entryName != null)
            {   // Read the specified entry
                ze = zf.getEntry(entryName);
                if (ze == null)
                {
                    String message = Logging.getMessage("WWIO.ZipFileEntryNIF", entryName, zipFile.getPath());
                    Logging.logger().severe(message);
                    throw new IOException(message);
                }
            }
            else
            {   // Read the first entry
                ze = zf.entries().nextElement(); // get the first entry
            }

            is = zf.getInputStream(ze);
            ByteBuffer buffer = null;
            if (ze.getSize() > 0)
            {
                buffer = transferStreamToByteBuffer(is, (int) ze.getSize());
                buffer.rewind();
            }
            return buffer;
        }
        finally
        {
            WWIO.closeStream(is, entryName);
        }
    }

    private static ByteBuffer transferStreamToByteBuffer(InputStream stream, int numBytes) throws IOException
    {
        if (stream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (numBytes < 1)
        {
            Logging.logger().severe("WWIO.NumberBytesTransferLessThanOne");
            throw new IllegalArgumentException(Logging.getMessage("WWIO.NumberBytesTransferLessThanOne"));
        }

        int bytesRead = 0;
        int count = 0;
        byte[] bytes = new byte[numBytes];
        while (count >= 0 && (numBytes - bytesRead) > 0)
        {
            count = stream.read(bytes, bytesRead, numBytes - bytesRead);
            if (count > 0)
            {
                bytesRead += count;
            }
        }
        ByteBuffer buffer = BufferUtil.newByteBuffer(bytes.length); // to get a jogl-compatible buffer
        return buffer.put(bytes);
    }

    /**
     * Reads all the available bytes from the specified {@link InputStream}, returning the bytes as a non-direct {@link
     * ByteBuffer} with the current JVM byte order. Non-direct buffers are backed by JVM heap memory.
     *
     * @param inputStream the stream to read.
     *
     * @return the bytes from the specified stream, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the stream is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readStreamToBuffer(InputStream inputStream) throws IOException
    {
        if (inputStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return readStreamToBuffer(inputStream, false);
    }

    /**
     * Reads all the available bytes from the specified {@link InputStream}, returning the bytes as a {@link ByteBuffer}
     * with the current JVM byte order. This returns a direct ByteBuffer if allocateDirect is true, and returns a
     * non-direct ByteBuffer otherwise. Direct buffers are backed by native memory, and may reside outside of the normal
     * garbage-collected heap. Non-direct buffers are backed by JVM heap memory.
     *
     * @param inputStream    the stream to read.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the bytes from the specified stream, with the current JVM byte order.
     *
     * @throws IllegalArgumentException if the stream is null.
     * @throws IOException              if an I/O error occurs.
     */
    public static ByteBuffer readStreamToBuffer(InputStream inputStream, boolean allocateDirect) throws IOException
    {
        if (inputStream == null)
        {
            String message = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        final int PAGE_SIZE = (int) Math.round(Math.pow(2, 16));

        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = allocateDirect ? ByteBuffer.allocateDirect(PAGE_SIZE) : ByteBuffer.allocate(PAGE_SIZE);

        int count = 0;
        while (count >= 0)
        {
            count = channel.read(buffer);
            if (count > 0 && !buffer.hasRemaining())
            {
                ByteBuffer biggerBuffer = allocateDirect ? ByteBuffer.allocateDirect(buffer.limit() + PAGE_SIZE)
                    : ByteBuffer.allocate(buffer.limit() + PAGE_SIZE);
                biggerBuffer.put((ByteBuffer) buffer.rewind());
                buffer = biggerBuffer;
            }
        }

        if (buffer != null)
            buffer.flip();

        return buffer;
    }

    public static ByteBuffer inflateStreamToBuffer(InputStream inputStream) throws IOException
    {
        return readStreamToBuffer(new InflaterInputStream(inputStream));
    }

    public static String replaceSuffix(String in, String newSuffix)
    {
        if (in == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        String suffix = newSuffix != null ? newSuffix : "";
        int p = in.lastIndexOf(".");
        return p >= 0 ? in.substring(0, p) + suffix : in + suffix;
    }

    public static String getSuffix(String filePath)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int len = filePath.length();
        int p = filePath.lastIndexOf(".");
        return (p >= 0 && p + 1 < len) ? filePath.substring(p + 1, len) : null;
    }

    /**
     * Returns the name of the file or directory denoted by the specified path. This is the last file name in the path,
     * or null if the path does not contain any file names.
     *
     * @param filePath a file path String.
     *
     * @return the last name in the specified path, or null if the path does not contain a name.
     *
     * @throws IllegalArgumentException if the file path is null.
     */
    public static String getFilename(String filePath)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        filePath = stripTrailingSeparator(filePath);

        int len = filePath.length();
        int p = filePath.lastIndexOf("/");
        if (p < 0)
            p = filePath.lastIndexOf("\\");
        return (p >= 0 && p + 1 < len) ? filePath.substring(p + 1, len) : null;
    }

    /**
     * Returns the file path's parent directory path, or null if the file path does not have a parent.
     *
     * @param filePath a file path String.
     *
     * @return the file path's parent directory, or null if the path does not have a parent.
     *
     * @throws IllegalArgumentException if the file path is null.
     */
    public static String getParentFilePath(String filePath)
    {
        if (filePath == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        filePath = stripTrailingSeparator(filePath);

        int len = filePath.length();
        int p = filePath.lastIndexOf("/");
        if (p < 0)
            p = filePath.lastIndexOf("\\");
        return (p > 0 && p < len) ? filePath.substring(0, p) : null;
    }

    public static File saveBufferToTempFile(ByteBuffer buffer, String suffix) throws IOException
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File outputFile = java.io.File.createTempFile("WorldWind", suffix != null ? suffix : "");
        outputFile.deleteOnExit();
        buffer.rewind();
        WWIO.saveBuffer(buffer, outputFile);

        return outputFile;
    }

    public static boolean isFileOutOfDate(URL url, long expiryTime)
    {
        if (url == null)
        {
            String message = Logging.getMessage("nullValue.URLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            // Determine whether the file can be treated like a File, e.g., a jar entry.
            URI uri = url.toURI();
            if (uri.isOpaque())
                return false; // TODO: Determine how to check the date of non-Files

            File file = new File(uri);

            return file.exists() && file.lastModified() < expiryTime;
        }
        catch (URISyntaxException e)
        {
            Logging.logger().log(Level.SEVERE, "WWIO.ExceptionValidatingFileExpiration", url);
            return false;
        }
    }

    public static Proxy configureProxy()
    {
        String proxyHost = Configuration.getStringValue(AVKey.URL_PROXY_HOST);
        if (proxyHost == null)
            return null;

        Proxy proxy = null;

        try
        {
            int proxyPort = Configuration.getIntegerValue(AVKey.URL_PROXY_PORT);
            String proxyType = Configuration.getStringValue(AVKey.URL_PROXY_TYPE);

            SocketAddress addr = new InetSocketAddress(proxyHost, proxyPort);
            if (proxyType.equals("Proxy.Type.Http"))
                proxy = new Proxy(Proxy.Type.HTTP, addr);
            else if (proxyType.equals("Proxy.Type.SOCKS"))
                proxy = new Proxy(Proxy.Type.SOCKS, addr);
        }
        catch (Exception e)
        {
            Logging.logger().log(Level.WARNING,
                Logging.getMessage("URLRetriever.ErrorConfiguringProxy", proxyHost), e);
        }

        return proxy;
    }

    public static String makeSuffixForMimeType(String mimeType)
    {
        if (mimeType == null)
        {
            String message = Logging.getMessage("nullValue.ImageFomat");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!mimeType.contains("/") || mimeType.endsWith("/"))
        {
            String message = Logging.getMessage("generic.InvalidImageFormat");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        String suffix = mimeType.substring(mimeType.lastIndexOf("/") + 1);

        suffix = suffix.replaceFirst("jpeg", "jpg"); // if jpeg, replace with conventional suffix
        suffix = suffix.replaceFirst("tiff", "tif"); // if tiff, replace with conventional suffix

        return "." + suffix;
    }

    public static Object getFileOrResourceAsStream(String path, Class c)
    {
        if (path == null)
        {
            String message = Logging.getMessage("nullValue.FilePathIsNull");
            throw new IllegalStateException(message);
        }

        File file = new File(path);
        if (file.exists())
        {
            try
            {
                return new FileInputStream(file);
            }
            catch (Exception e)
            {
                return e;
            }
        }

        if (c == null)
            c = WWIO.class;

        try
        {
            return c.getResourceAsStream("/" + path);
        }
        catch (Exception e)
        {
            return e;
        }
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link String}. The method creates a copy of the string's
     * contents and passes a steam reference to that copy.
     *
     * @param string the string to create a stream for, encoded in UTF-8.
     *
     * @return an {@link InputStream} for the string's contents.
     *
     * @throws IllegalArgumentException if <code>string</code> is null.
     */
    public static InputStream getInputStreamFromString(String string)
    {
        return getInputStreamFromString(string, "UTF-8");
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link String}. The method creates a copy of the string's
     * contents and passes a steam reference to that copy.
     *
     * @param string the string to create a stream for.
     * @param encoding the character encoding of the string. UTF-8 is used if null.
     *
     * @return an {@link InputStream} for the string's contents.
     *
     * @throws IllegalArgumentException if <code>string</code> is null.
     */
    public static InputStream getInputStreamFromString(String string, String encoding)
    {
        if (string == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        try
        {
            return new ByteArrayInputStream(string.getBytes(encoding != null ? encoding : "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WWRuntimeException(e); // should never happen because encoding is always UTF8.
        }
    }

    /**
     * Creates an {@link InputStream} for the contents of a {@link ByteBuffer}. The method creates a copy of the
     * buffer's contents and passes a steam reference to that copy.
     *
     * @param buffer the buffer to create a stream for.
     *
     * @return an {@link InputStream} for the buffer's contents.
     *
     * @throws IllegalArgumentException if <code>buffer</code> is null.
     */
    public static InputStream getInputStreamFromByteBuffer(ByteBuffer buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.ByteBufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (buffer.hasArray() && buffer.limit() == buffer.capacity()) // otherwise bytes beyond the limit are included
            return new ByteArrayInputStream(buffer.array());

        byte[] byteArray = new byte[buffer.limit()];
        buffer.get(byteArray);
        return new ByteArrayInputStream(byteArray);
    }

    public static boolean isAncestorOf(File file, File ancestor)
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (ancestor == null)
        {
            String message = Logging.getMessage("nullValue.AncestorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Traverse up the directory tree, visiting each node. If any node is equal to the specified ancestor,
        // then the files are related.
        File cur = file;
        while (cur != null && !cur.equals(ancestor))
        {
            cur = cur.getParentFile();
        }

        // If the ancestor appeared in the traversal, then we will have stopped before reaching the root and
        // cur will be non-null. Otherwise we exhaused our traversal and cur is null.
        return cur != null;
    }

    public static void copyFile(File source, File destination) throws IOException
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (destination == null)
        {
            String message = Logging.getMessage("nullValue.DestinationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel fic, foc;

        try
        {
            fis = new FileInputStream(source);
            fic = fis.getChannel();

            fos = new FileOutputStream(destination);
            foc = fos.getChannel();

            foc.transferFrom(fic, 0, fic.size());
            fos.flush();

            fis.close();
            fos.close();
        }
        finally
        {
            WWIO.closeStream(fis, source.getPath());
            WWIO.closeStream(fos, destination.getPath());
        }
    }

    public static void copyDirectory(File source, File destination, boolean copySubDirectories) throws IOException
    {
        if (source == null)
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (destination == null)
        {
            String message = Logging.getMessage("nullValue.DestinationIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!destination.exists())
            //noinspection ResultOfMethodCallIgnored
            destination.mkdirs();

        if (!destination.exists())
        {
            String message = Logging.getMessage("generic.CannotCreateFile", destination);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        File[] fileList = source.listFiles();
        if (fileList == null)
            return;

        List<File> childFiles = new ArrayList<File>();
        List<File> childDirs = new ArrayList<File>();
        for (File child : fileList)
        {
            if (child == null)
                continue;

            if (child.isDirectory())
                childDirs.add(child);
            else
                childFiles.add(child);
        }

        for (File childFile : childFiles)
        {
            File destFile = new File(destination, childFile.getName());
            copyFile(childFile, destFile);
        }

        if (copySubDirectories)
        {
            for (File childDir : childDirs)
            {
                File destDir = new File(destination, childDir.getName());
                copyDirectory(childDir, destDir, copySubDirectories);
            }
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void deleteDirectory(File file) throws IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        File[] fileList = file.listFiles();
        if (fileList != null)
        {
            List<File> childFiles = new ArrayList<File>();
            List<File> childDirs = new ArrayList<File>();
            for (File child : fileList)
            {
                if (child == null)
                    continue;

                if (child.isDirectory())
                    childDirs.add(child);
                else
                    childFiles.add(child);
            }

            for (File childFile : childFiles)
            {
                childFile.delete();
            }

            for (File childDir : childDirs)
            {
                deleteDirectory(childDir);
            }
        }
    }

    /**
     * Close a stream and catch any {@link IOException} generated in the process. Stream types supported are {@link
     * InputStream},  {@link OutputStream}, {@link Reader} and {@link Writer}.
     *
     * @param stream the stream to close. If null, this method does nothing.
     * @param name   the name of the stream to place in the log message if an exception is encountered.
     */
    public static void closeStream(Object stream, String name)
    {
        if (stream == null)
            return;

        try
        {
            if (stream instanceof InputStream)
                ((InputStream) stream).close();

            else if (stream instanceof OutputStream)
                ((OutputStream) stream).close();

            else if (stream instanceof Reader)
                ((Reader) stream).close();

            else if (stream instanceof Writer)
                ((Writer) stream).close();

            else if (stream instanceof FileChannel)
                ((FileChannel) stream).close();

            else
            {
                String message = Logging.getMessage("WWIO.StreamTypeNotSupported", name != null ? name : "Unknown");
                Logging.logger().warning(message);
            }
        }
        catch (IOException e)
        {
            String message = Logging.getMessage("generic.ExceptionClosingStream", e, name != null ? name : "Unknown");
            Logging.logger().severe(message);
        }
    }

    /**
     * Open and read a text file into {@link String}.
     *
     * @param file a {@link File} reference to the file to open and read.
     *
     * @return a {@link String} containing the contents of the file.
     *
     * @throws IllegalArgumentException if the file is null.
     */
    public static String readTextFile(File file)
    {
        if (file == null)
        {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        StringBuilder sb = new StringBuilder();

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        catch (IOException e)
        {
            String msg = Logging.getMessage("generic.ExceptionAttemptingToReadFile", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
            return null;
        }
        finally
        {
            WWIO.closeStream(reader, file.getPath());
        }

        return sb.toString();
    }

    /**
     * Save a {@link String} to a text file.
     *
     * @param text the {@link String} to write to the file.
     * @param file a {@link File} reference to the file to create.
     *
     * @throws IllegalArgumentException if the text string or file is null.
     */
    public static void writeTextFile(String text, File file)
    {
        if (file == null)
        {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (text == null)
        {
            String msg = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(text);
        }
        catch (IOException e)
        {
            String msg = Logging.getMessage("generic.ExceptionAttemptingToWriteTo", file.getPath());
            Logging.logger().log(java.util.logging.Level.SEVERE, msg);
        }
        finally
        {
            WWIO.closeStream(writer, file.getPath());
        }
    }

    /**
     * Opens a file located via an absolute path or a path relative to the classpath.
     *
     * @param fileName the path of the file to open, either absolute or relative to the classpath.
     * @param c        the class that will be used to find a path relative to the classpath.
     *
     * @return an {@link InputStream} to the open file
     *
     * @throws IllegalArgumentException if the file name is null.
     * @throws WWRuntimeException       if an exception occurs or the file can't be found. The causing exception is
     *                                  available via this exception's {@link Throwable#initCause(Throwable)} method.
     */
    public static InputStream openFileOrResourceStream(String fileName, Class c)
    {
        if (fileName == null)
        {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Object streamOrException = WWIO.getFileOrResourceAsStream(fileName, c);

        if (streamOrException instanceof Exception)
        {
            String msg = Logging.getMessage("generic.CannotOpenFile", fileName);
            throw new WWRuntimeException(msg, (Exception) streamOrException);
        }

        return (InputStream) streamOrException;
    }

    /**
     * Create a {@link String} from a {@link ByteBuffer}.
     *
     * @param buffer   the byte buffer to convert.
     * @param encoding the encoding do use. If null is specified then UTF-8 is used.
     *
     * @return the string representation of the bytes in the buffer decoded according to the specified encoding.
     */
    public static String byteBufferToString(ByteBuffer buffer, String encoding)
    {
        if (buffer == null)
        {
            String msg = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return Charset.forName(encoding != null ? encoding : "UTF-8").decode(buffer).toString();
    }

    /**
     * Create a {@link ByteBuffer} from a {@link String}.
     *
     * @param string   the string to convert.
     * @param encoding the encoding do use. If null is specified then UTF-8 is used.
     *
     * @return the ByteBuffer representation of the string decoded according to the specified encoding.
     *
     * @throws UnsupportedEncodingException if the specified encoding is not supported
     */
    public static ByteBuffer stringToByteBuffer(String string, String encoding) throws UnsupportedEncodingException
    {
        if (string == null)
        {
            String msg = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        return ByteBuffer.wrap(string.getBytes(encoding != null ? encoding : "UTF-8"));
    }

    /**
     * Open a reader on an input source. The source may be one of the following: <ul> <li>{@link Reader}</li> <li>{@link
     * InputStream}</li> <li>{@link File}</li> <li>{@link URL}</li> <li>{@link String}</li> </ul>
     * <p/>
     * Readers are used to read character streams.
     *
     * @param src the input source of one of the above types.
     *
     * @return a reader for the input source.
     *
     * @throws java.io.IOException if i/o or other errors occur trying to create the reader.
     */
    public static java.io.Reader openReader(Object src) throws java.io.IOException
    {
        java.io.Reader r = null;

        if (src instanceof java.io.Reader)
            r = (java.io.Reader) src;
        else if (src instanceof java.io.InputStream)
            r = new java.io.InputStreamReader((java.io.InputStream) src);
        else if (src instanceof java.io.File)
            r = new java.io.FileReader((java.io.File) src);
        else if (src instanceof java.net.URL)
            r = new java.io.InputStreamReader(((java.net.URL) src).openStream());
        else if (src instanceof String)
            r = new java.io.StringReader((String) src);

        return r;
    }

    public static URL makeURL(String path)
    {
        try
        {
            return new URL(path);
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }
}
