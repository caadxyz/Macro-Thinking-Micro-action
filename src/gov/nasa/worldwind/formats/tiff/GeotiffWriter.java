/* Copyright (C) 2001, 2008 United States Government as represented by
   the Administrator of the National Aeronautics and Space Administration.
   All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.*;

import javax.imageio.*;
import java.io.*;
import java.awt.image.*;
import java.nio.channels.*;
import java.nio.*;
import java.util.*;

/**
 * @author brownrigg
 * @version $Id$
 */

public class GeotiffWriter
{
    private RandomAccessFile targetFile;
    private FileChannel theChannel;

    // We need the size in bytes of various primitives...
    private static final int DOUBLE_SIZEOF = Double.SIZE / Byte.SIZE;
    private static final int FLOAT_SIZEOF = Float.SIZE / Byte.SIZE;
    private static final int INTEGER_SIZEOF = Integer.SIZE / Byte.SIZE;
    private static final int SHORT_SIZEOF = Short.SIZE / Byte.SIZE;

    public GeotiffWriter(String filename) throws IOException
    {
        // the initializer does the validity checking...
        commonInitializer(filename);
    }

    public GeotiffWriter(File targetFile) throws IOException
    {
        if (targetFile == null)
        {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        commonInitializer(targetFile.getAbsolutePath());
    }

    //
    // Merely consolidates the error checking for the ctors in one place.
    //
    private void commonInitializer(String targetFilename) throws IOException
    {
        if (targetFilename == null)
        {
            String msg = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        File file = new File(targetFilename);
        if (!file.getParentFile().canWrite())
        {
            String msg = Logging.getMessage("GeotiffWriter.BadFile", targetFilename);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.targetFile = new RandomAccessFile(targetFilename, "rw");
        this.theChannel = this.targetFile.getChannel();
    }

    public void close()
    {
        try
        {
            this.targetFile.close();
        }
        catch (Exception ex) { /* best effort */ }
    }

    public void write(BufferedImage image) throws IOException
    {
        if (image == null)
        {
            String msg = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // how we proceed in part depends upon the image type...
        int type = image.getType();
        if (type == BufferedImage.TYPE_3BYTE_BGR || type == BufferedImage.TYPE_4BYTE_ABGR ||
            type == BufferedImage.TYPE_4BYTE_ABGR_PRE || type == BufferedImage.TYPE_INT_RGB ||
            type == BufferedImage.TYPE_INT_BGR || type == BufferedImage.TYPE_INT_ARGB ||
            type == BufferedImage.TYPE_INT_ARGB_PRE)
            writeColorImage(image);
        else if (type == BufferedImage.TYPE_BYTE_GRAY)
            writeGrayscaleImage(image);
        else
        {
            String msg = Logging.getMessage("GeotiffWriter.UnsupportedType", type);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private void writeColorImage(BufferedImage image) throws IOException
    {
        int numBands = image.getRaster().getNumBands();

        // write the header...
        writeHeader();

        // write the image data...
        int numRows = image.getHeight();
        int numCols = image.getWidth();
        int[] stripCounts = new int[numRows];
        int[] stripOffsets = new int[numRows];
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(numCols * numBands);
        Raster rast = image.getRaster();

        for (int i = 0; i < numRows; i++)
        {
            stripOffsets[i] = (int) this.theChannel.position();
            stripCounts[i] = numCols * numBands;
            int[] rowData = rast.getPixels(0, i, image.getWidth(), 1, (int[]) null);
            dataBuff.clear();
            for (int j = 0; j < numCols * numBands; j++)
            {
                putUnsignedByte(dataBuff, rowData[j]);
            }
            dataBuff.flip();
            this.theChannel.write(dataBuff);
        }

        // write out values for the tiff tags and build up the IFD. These are supposed to be sorted; for now
        // do this manually here.
        ArrayList<TiffIFDEntry> ifds = new ArrayList<TiffIFDEntry>(10);

        ifds.add(new TiffIFDEntry(TiffTags.IMAGE_WIDTH, TiffTypes.LONG, 1, numCols));
        ifds.add(new TiffIFDEntry(TiffTags.IMAGE_LENGTH, TiffTypes.LONG, 1, numRows));

        byte[] tmp = new byte[numBands * 2];
        for (int i = 0; i < numBands * 2; i++)
        {
            tmp[i] = ((i % 2) == 0) ? (byte) 0 : (byte) 8;
        }
        ByteBuffer buff = ByteBuffer.wrap(tmp);
        long offset = this.theChannel.position();
        this.theChannel.write(buff);
        ifds.add(new TiffIFDEntry(TiffTags.BITS_PER_SAMPLE, TiffTypes.SHORT, numBands, offset));

        ifds.add(new TiffIFDEntry(TiffTags.COMPRESSION, TiffTypes.LONG, 1, TiffConstants.COMPRESSION_NONE));
        ifds.add(new TiffIFDEntry(TiffTags.PHOTO_INTERPRETATION, TiffTypes.SHORT, 1, TiffConstants.PHOTOINTERP_RGB));

        offset = this.theChannel.position();
        dataBuff = ByteBuffer.allocateDirect(stripOffsets.length * INTEGER_SIZEOF);
        for (int i = 0; i < stripOffsets.length; i++)
        {
            dataBuff.putInt(stripOffsets[i]);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(TiffTags.STRIP_OFFSETS, TiffTypes.LONG, stripOffsets.length, (int) offset));

        ifds.add(new TiffIFDEntry(TiffTags.SAMPLES_PER_PIXEL, TiffTypes.SHORT, 1, numBands));
        ifds.add(new TiffIFDEntry(TiffTags.ROWS_PER_STRIP, TiffTypes.LONG, 1, 1));

        offset = this.theChannel.position();
        dataBuff.clear();  // stripOffsets and stripCounts are same length by design; can reuse the ByteBuffer...
        for (int i = 0; i < stripCounts.length; i++)
        {
            dataBuff.putInt(stripCounts[i]);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(TiffTags.STRIP_BYTE_COUNTS, TiffTypes.LONG, stripCounts.length, (int) offset));

        ifds.add(new TiffIFDEntry(TiffTags.PLANAR_CONFIGURATION, TiffTypes.SHORT, 1,
            TiffConstants.PLANARCONFIG_CHUNKY));

        writeIFDs(ifds);
    }

    //
    // We only support 8-bit/sample currently (Tiff spec allows for 4 bit/sample).
    //
    private void writeGrayscaleImage(BufferedImage image) throws IOException
    {
        int numBands = 1;

        // write the header...
        writeHeader();

        // write the image data...
        int numRows = image.getHeight();
        int numCols = image.getWidth();
        int[] stripCounts = new int[numRows];
        int[] stripOffsets = new int[numRows];
        ByteBuffer dataBuff = ByteBuffer.allocateDirect(numCols * numBands);
        Raster rast = image.getRaster();

        for (int i = 0; i < numRows; i++)
        {
            stripOffsets[i] = (int) this.theChannel.position();
            stripCounts[i] = numCols * numBands;
            int[] rowData = rast.getPixels(0, i, image.getWidth(), 1, (int[]) null);
            dataBuff.clear();
            for (int j = 0; j < numCols * numBands; j++)
            {
                putUnsignedByte(dataBuff, rowData[j]);
            }
            dataBuff.flip();
            this.theChannel.write(dataBuff);
        }

        // write out values for the tiff tags and build up the IFD. These are supposed to be sorted; for now
        // do this manually here.
        ArrayList<TiffIFDEntry> ifds = new ArrayList<TiffIFDEntry>(10);

        ifds.add(new TiffIFDEntry(TiffTags.IMAGE_WIDTH, TiffTypes.LONG, 1, numCols));
        ifds.add(new TiffIFDEntry(TiffTags.IMAGE_LENGTH, TiffTypes.LONG, 1, numRows));

        ifds.add(new TiffIFDEntry(TiffTags.BITS_PER_SAMPLE, TiffTypes.SHORT, numBands, 8));

        ifds.add(new TiffIFDEntry(TiffTags.COMPRESSION, TiffTypes.LONG, 1, TiffConstants.COMPRESSION_NONE));
        ifds.add(new TiffIFDEntry(TiffTags.PHOTO_INTERPRETATION, TiffTypes.SHORT, 1,
            TiffConstants.PHOTOINTERP_BLACKISZERO));

        long offset = this.theChannel.position();
        dataBuff = ByteBuffer.allocateDirect(stripOffsets.length * INTEGER_SIZEOF);
        for (int i = 0; i < stripOffsets.length; i++)
        {
            dataBuff.putInt(stripOffsets[i]);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(TiffTags.STRIP_OFFSETS, TiffTypes.LONG, stripOffsets.length, (int) offset));

        ifds.add(new TiffIFDEntry(TiffTags.SAMPLES_PER_PIXEL, TiffTypes.SHORT, 1, numBands));
        ifds.add(new TiffIFDEntry(TiffTags.ROWS_PER_STRIP, TiffTypes.LONG, 1, 1));

        offset = this.theChannel.position();
        dataBuff.clear();  // stripOffsets and stripCounts are same length by design; can reuse the ByteBuffer...
        for (int i = 0; i < stripCounts.length; i++)
        {
            dataBuff.putInt(stripCounts[i]);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);
        ifds.add(new TiffIFDEntry(TiffTags.STRIP_BYTE_COUNTS, TiffTypes.LONG, stripCounts.length, (int) offset));

        writeIFDs(ifds);
    }

    private void writeHeader() throws IOException
    {
        byte[] hdr = new byte[8];
        ByteBuffer buff = ByteBuffer.wrap(hdr);
        buff.putShort((short) 0x4D4D);  // magic numbers...
        buff.put((byte) 0);
        buff.put((byte) 42);
        buff.putInt(0);                // we'll patch this up later after writing the image...
        buff.flip();
        this.theChannel.write(buff);
    }

    private void writeIFDs(ArrayList<TiffIFDEntry> ifds) throws IOException
    {
        long offset = this.theChannel.position();

        // This is supposed to start on a word boundary, via decree of the spec.
        long adjust = offset % 4L;
        offset += (adjust == 0) ? 0 : (4L - adjust);

        this.theChannel.position(offset);

        ByteBuffer dataBuff = ByteBuffer.allocateDirect(ifds.size() * 12);

        // The IFD directory is preceeded by a SHORT count of the number of entries...
        putUnsignedShort(dataBuff, ifds.size());
        dataBuff.flip();
        this.theChannel.write(dataBuff);

        dataBuff.clear();
        for (TiffIFDEntry ifd : ifds)
        {
            putUnsignedShort(dataBuff, ifd.tag);
            putUnsignedShort(dataBuff, ifd.type);
            putUnsignedInt(dataBuff, ifd.count);
            if (ifd.type == TiffTypes.SHORT && ifd.count == 1)
            {
                // these get packed in the first few bytes...
                putUnsignedShort(dataBuff, (int) ifd.valOffset);
                dataBuff.putShort((short) 0);
            }
            else
                putUnsignedInt(dataBuff, ifd.valOffset);
        }
        dataBuff.flip();
        this.theChannel.write(dataBuff);

        // The spec requires 4 bytes of zeros at the end...
        dataBuff.clear();
        dataBuff.putInt(0);
        dataBuff.flip();
        this.theChannel.write(dataBuff);

        // go back and patch up the ifd offset in header...
        this.theChannel.position(4);
        dataBuff.clear();
        putUnsignedInt(dataBuff, offset);
        dataBuff.flip();
        this.theChannel.write(dataBuff);
    }

    private void putUnsignedByte(ByteBuffer buff, int value)
    {
        buff.put((byte) (value & 0xff));
    }

    private void putUnsignedShort(ByteBuffer buff, int value)
    {
        buff.putShort((short) (value & 0xffff));
    }

    private void putUnsignedInt(ByteBuffer buff, long value)
    {
        buff.putInt((int) (value & 0xffffffffL));
    }
}
