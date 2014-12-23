/* Copyright (C) 2001, 2008 United States Government as represented by
   the Administrator of the National Aeronautics and Space Administration.
   All Rights Reserved.
 */

package gov.nasa.worldwind.formats.tiff;

import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author brownrigg
 * @version $Id: GeotiffReader.java 11586 2009-06-11 19:02:34Z jparsons $
 */
public class GeotiffReader
{

    public GeotiffReader(File sourceFile) throws IOException
    {
        this(sourceFile.getAbsolutePath());
    }

    public GeotiffReader(String sourceFilename) throws IOException
    {
        this.sourceFilename = sourceFilename;
        this.sourceFile = new RandomAccessFile(sourceFilename, "r");
        this.theChannel = this.sourceFile.getChannel();

        // Note that by reading this early on, we detect whether we indeed have a tiff file, and help
        // ensure the various accessors (beyond read()) are successful.
        readIFDs();
    }

    public void close()
    {
        try
        {
            this.sourceFile.close();
        }
        catch (Exception ex)
        { /* best effort */ }
    }

    public int getNumImages() throws IOException
    {
        return (this.ifds != null) ? this.ifds.size() : 0;
    }

    public int getWidth(int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        TiffIFDEntry widthEntry = getByTag(ifds.get(imageIndex), TiffTags.IMAGE_WIDTH);
        return (int) widthEntry.asLong();
    }

    public int getHeight(int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);
        TiffIFDEntry heightEntry = getByTag(ifds.get(imageIndex), TiffTags.IMAGE_LENGTH);
        return (int) heightEntry.asLong();
    }

    public BufferedImage read() throws IOException
    {
        return read(0);
    }

    public BufferedImage read(int imageIndex) throws IOException
    {
        checkImageIndex(imageIndex);

        // Extract the various IFD tags we need to read this image. We want to loop over the tag set once, instead
        // multiple times if we simply used our general getByTag() method.
        TiffIFDEntry widthEntry = null;
        TiffIFDEntry lengthEntry = null;
        TiffIFDEntry bitsPerSampleEntry = null;
        TiffIFDEntry samplesPerPixelEntry = null;
        TiffIFDEntry photoInterpEntry = null;
        TiffIFDEntry stripOffsetsEntry = null;
        TiffIFDEntry stripCountsEntry = null;
        TiffIFDEntry rowsPerStripEntry = null;
        TiffIFDEntry planarConfigEntry = null;
        TiffIFDEntry colorMapEntry = null;
        TiffIFDEntry sampleFormatEntry = null;
        boolean tiffDifferencing = false;

        TiffIFDEntry[] ifd = this.ifds.get(imageIndex);
        for (TiffIFDEntry entry : ifd)
        {
            switch (entry.tag)
            {
                case TiffTags.IMAGE_WIDTH:
                    widthEntry = entry;
                    break;
                case TiffTags.IMAGE_LENGTH:
                    lengthEntry = entry;
                    break;
                case TiffTags.BITS_PER_SAMPLE:
                    bitsPerSampleEntry = entry;
                    break;
                case TiffTags.SAMPLES_PER_PIXEL:
                    samplesPerPixelEntry = entry;
                    break;
                case TiffTags.PHOTO_INTERPRETATION:
                    photoInterpEntry = entry;
                    break;
                case TiffTags.STRIP_OFFSETS:
                    stripOffsetsEntry = entry;
                    break;
                case TiffTags.STRIP_BYTE_COUNTS:
                    stripCountsEntry = entry;
                    break;
                case TiffTags.ROWS_PER_STRIP:
                    rowsPerStripEntry = entry;
                    break;
                case TiffTags.PLANAR_CONFIGURATION:
                    planarConfigEntry = entry;
                    break;
                case TiffTags.COLORMAP:
                    colorMapEntry = entry;
                    break;
                case TiffTags.SAMPLE_FORMAT:
                    sampleFormatEntry = entry;
                    break;
            }
        }

        // Check that we have the mandatory tags present...
        if (widthEntry == null || lengthEntry == null || samplesPerPixelEntry == null || photoInterpEntry == null ||
            stripOffsetsEntry == null || stripCountsEntry == null || rowsPerStripEntry == null
            || planarConfigEntry == null)
        {
            String message = Logging.getMessage("GeotiffReader.MissingTags");
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        TiffIFDEntry notToday = getByTag(ifd, TiffTags.COMPRESSION);
        boolean lzwCompressed = false;
        if (notToday != null && notToday.asLong() == TiffConstants.COMPRESSION_LZW)
        {
            lzwCompressed = true;
            TiffIFDEntry predictorEntry = getByTag(ifd, TiffTags.TIFF_PREDICTOR);
            if ((predictorEntry != null) && (predictorEntry.asLong() != 0))
                tiffDifferencing = true;
        }
        else if (notToday != null && notToday.asLong() != TiffConstants.COMPRESSION_NONE)
        {
            String message = Logging.getMessage("GeotiffReader.CompressionFormatNotSupported");
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        notToday = getByTag(ifd, TiffTags.TILE_WIDTH);
        if (notToday != null)
        {
            String message = Logging.getMessage("GeotiffReader.NoTiled");
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        int width = (int) widthEntry.asLong();
        int height = (int) lengthEntry.asLong();
        int samplesPerPixel = (int) samplesPerPixelEntry.asLong();
        long photoInterp = photoInterpEntry.asLong();
        long rowsPerStrip = rowsPerStripEntry.asLong();
        long planarConfig = planarConfigEntry.asLong();
        int[] bitsPerSample = getBitsPerSample(bitsPerSampleEntry);
        long[] stripOffsets = getStripsArray(stripOffsetsEntry);
        long offset = stripOffsets[0];
        long[] stripCounts = getStripsArray(stripCountsEntry);

        ColorModel colorModel;
        WritableRaster raster;

        //
        // TODO: This isn't terribly robust; we know how to deal with a few specific types...
        //

        if (samplesPerPixel == 1 && bitsPerSample.length == 1 && bitsPerSample[0] == 16)
        {
            // 16-bit grayscale (typical of elevation data, for example)...
            long sampleFormat =
                (sampleFormatEntry != null) ? sampleFormatEntry.asLong() : TiffConstants.SAMPLEFORMAT_UNSIGNED;
            int dataBuffType =
                (sampleFormat == TiffConstants.SAMPLEFORMAT_SIGNED) ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;

            colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), bitsPerSample, false,
                false, Transparency.OPAQUE, dataBuffType);
            int[] offsets = new int[] {0};
            ComponentSampleModel sampleModel = new ComponentSampleModel(dataBuffType, width, height, 1, width, offsets);
            short[][] imageData = readPlanar16(width, height, samplesPerPixel, stripOffsets, stripCounts, rowsPerStrip);
            DataBuffer dataBuff = (dataBuffType == DataBuffer.TYPE_SHORT) ?
                new DataBufferShort(imageData, width * height, offsets) :
                new DataBufferUShort(imageData, width * height, offsets);

            raster = Raster.createWritableRaster(sampleModel, dataBuff, new Point(0, 0));
        }
        else if (samplesPerPixel == 1 && bitsPerSample.length == 1 && bitsPerSample[0] == 32 &&
            sampleFormatEntry != null && sampleFormatEntry.asLong() == TiffConstants.SAMPLEFORMAT_IEEEFLOAT)
        {
            // 32-bit grayscale (typical of elevation data, for example)...
            colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), bitsPerSample, false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
            int[] offsets = new int[] {0};
            ComponentSampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
                offsets);
            float[][] imageData = readPlanarFloat32(width, height, samplesPerPixel, stripOffsets, stripCounts,
                rowsPerStrip);
            DataBuffer dataBuff = new DataBufferFloat(imageData, width * height, offsets);
            raster = Raster.createWritableRaster(sampleModel, dataBuff, new Point(0, 0));
        }
        else
        {

            // make sure a DataBufferByte is going to do the trick
            for (int bits : bitsPerSample)
            {
                if (bits != 8)
                {
                    String message = Logging.getMessage("GeotiffReader.Not8bit", bits);
                    Logging.logger().warning(message);
                    throw new IOException(message);
                }
            }

            // byte image data; could be RGB-component, grayscale, or indexed-color.
            // Set up an appropriate ColorModel...
            if (samplesPerPixel > 1)
            {
                int transparency = Transparency.OPAQUE;
                boolean hasAlpha = false;
                if (samplesPerPixel == 4)
                {
                    transparency = Transparency.TRANSLUCENT;
                    hasAlpha = true;
                }
                colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), bitsPerSample,
                    hasAlpha,
                    false, transparency, DataBuffer.TYPE_BYTE);
            }
            else
            {
                // grayscale or indexed-color?
                if (photoInterp == TiffConstants.PHOTOINTERP_PALETTE)
                {
                    // indexed...
                    if (colorMapEntry == null)
                    {
                        String message = Logging.getMessage("GeotiffReader.MissingColormap");
                        Logging.logger().severe(message);
                        throw new IOException(message);
                    }
                    byte[][] cmap = readColorMap(colorMapEntry);
                    colorModel = new IndexColorModel(bitsPerSample[0], (int) colorMapEntry.count / 3, cmap[0], cmap[1],
                        cmap[2]);
                }
                else
                {
                    // grayscale...
                    colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), bitsPerSample,
                        false,
                        false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                }
            }

            int[] bankOffsets = new int[samplesPerPixel];
            for (int i = 0; i < samplesPerPixel; i++)
            {
                bankOffsets[i] = i;
            }
            int[] offsets = new int[(planarConfig == TiffConstants.PLANARCONFIG_CHUNKY) ? 1 : samplesPerPixel];
            for (int i = 0; i < offsets.length; i++)
            {
                offsets[i] = 0;
            }

            // construct the right SampleModel...
            ComponentSampleModel sampleModel;
            if (samplesPerPixel == 1)
                sampleModel = new ComponentSampleModel(DataBuffer.TYPE_BYTE, width, height, 1, width, bankOffsets);
            else
                sampleModel = (planarConfig == TiffConstants.PLANARCONFIG_CHUNKY) ?
                    new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, samplesPerPixel,
                        width * samplesPerPixel, bankOffsets) :
                    new BandedSampleModel(DataBuffer.TYPE_BYTE, width, height, width, bankOffsets, offsets);

            // Get the image data and make our Raster...
            byte[][] imageData;
            if (planarConfig == TiffConstants.PLANARCONFIG_CHUNKY)
            {
                if (lzwCompressed && (samplesPerPixel > 2))
                {
                    imageData = new byte[1][width * height * samplesPerPixel];

                    imageData[0] = readLZWCompressed(width, height, offset,
                        samplesPerPixel, tiffDifferencing, stripOffsets, stripCounts);
                }
                else
                    imageData = readPixelInterleaved8(width, height, samplesPerPixel, stripOffsets, stripCounts);
            }
            else
                imageData = readPlanar8(width, height, samplesPerPixel, stripOffsets, stripCounts, rowsPerStrip);

            DataBufferByte dataBuff = new DataBufferByte(imageData, width * height, offsets);
            raster = Raster.createWritableRaster(sampleModel, dataBuff, new Point(0, 0));
        }

        // Finally, put it all together to get our BufferedImage...
        return new BufferedImage(colorModel, raster, false, null);
    }

    /**
     * Returns true if georeferencing information was found in this file.
     * <p/>
     * Note: see getGeoKeys() for determining if projection information is contained in the file.
     *
     * @return Returns true if the file contains georeferencing information; false otherwise.
     */
    public boolean isGeotiff()
    {
        return (this.geoCodec != null);
    }

    /**
     *
     * @return Returns the GeoCodec that was constructed from this file. Returns null if no Geotiff tags were found.
     */
    public GeoCodec getGeoCodec()
    {
        return this.geoCodec;
    }

    private void repackageGeoReferencingTags() throws IOException
    {
        GeoCodec codec = new GeoCodec();
        boolean isGeotiff = false;

        TiffIFDEntry[] ifd = ifds.get(0);
        for (TiffIFDEntry entry : ifd)
        {
            switch (entry.tag)
            {
                case TiffTags.MODEL_PIXELSCALE:
                    codec.setModelPixelScale(readDoubles(entry));
                    isGeotiff = true;
                    break;
                case TiffTags.MODEL_TIEPOINT:
                    codec.addModelTiePoints(readDoubles(entry));
                    isGeotiff = true;
                    break;
                case TiffTags.MODEL_TRANSFORMATION:
                    codec.setModelTransformation(readDoubles(entry));
                    isGeotiff = true;
                    break;
                case TiffTags.GEO_KEY_DIRECTORY:
                    codec.setGeokeys(readShorts(entry));
                    isGeotiff = true;
                    break;
                case TiffTags.GEO_DOUBLE_PARAMS:
                    codec.setDoubleParams(readDoubles(entry));
                    // mere presence of these don't constitute a proper geotiff...
                    break;
                case TiffTags.GEO_ASCII_PARAMS:
                    codec.setAsciiParams(readBytes(entry));
                    // mere presence of these don't constitute a proper geotiff...
                    break;
            }
        }

        if (isGeotiff)
        {
            this.geoCodec = codec;
        }
    }

    /*
     * Coordinates reading all the ImageFileDirectories in a Tiff file (there's typically only one).
     *
     */
    private void readIFDs() throws IOException
    {
        if (this.ifds != null)
            return;

        if (this.theChannel == null)
        {
            String message = Logging.getMessage("GeotiffReader.NullInputFile", this.sourceFilename);
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        // determine byte ordering...
        ByteBuffer ifh = ByteBuffer.allocate(8);  // Tiff image-file header
        try
        {
            this.theChannel.read(ifh);
            ifh.flip();
            if (ifh.get(0) == 0x4D && ifh.get(1) == 0x4D)
            {
                this.tiffFileOrder = ByteOrder.BIG_ENDIAN;
            }
            else if (ifh.get(0) == 0x49 && ifh.get(1) == 0x49)
            {
                this.tiffFileOrder = ByteOrder.LITTLE_ENDIAN;
            }
            else
            {
                throw new IOException();
            }
        }
        catch (IOException ex)
        {
            String message = Logging.getMessage("GeotiffReader.BadTiffSig");
            Logging.logger().severe(message);
            throw new IOException(message);
        }

        // skip the magic number and get offset to first (and likely only) ImageFileDirectory...
        ifh.order(this.tiffFileOrder).position(4);
        long ifdOffset = getUnsignedInt(ifh);

        // position the channel to the ImageFileDirectory...
        this.theChannel.position(ifdOffset);
        ifh.clear().limit(2);
        this.theChannel.read(ifh);
        ifh.flip();
        readIFD(ifh.getShort());

        // decode any geotiff tags and structures that may be present into a manager object...
        repackageGeoReferencingTags();
    }

    /*
     * Reads an ImageFileDirectory and places it in our list.  It is assumed the caller has
     * prepositioned the file to the first entry (i.e., just past the short designating the
     * number of entries).
     *
     * Calls itself recursively if additional IFDs are indicated.
     *
     */
    private void readIFD(int numEntries) throws IOException
    {
        try
        {
            TiffIFDEntry[] ifd = new TiffIFDEntry[numEntries];
            // We size our ByteBuffer to accommodate the entire IFD plus the integer at the end indicating the
            // offset to the next IFD...
            ByteBuffer buff = ByteBuffer.allocateDirect(
                numEntries * 2 * SHORT_SIZEOF * 2 * INTEGER_SIZEOF + INTEGER_SIZEOF);
            this.theChannel.read(buff);

            buff.order(this.tiffFileOrder).flip();
            for (int i = 0; i < numEntries; i++)
            {
                int tag = getUnsignedShort(buff);
                int type = getUnsignedShort(buff);
                long count = getUnsignedInt(buff);
                long valoffset;
                if (type == TiffTypes.SHORT && count == 1)
                {
                    // these get packed left-justified in the bytes...
                    int upper = getUnsignedShort(buff);
                    int lower = getUnsignedShort(buff);
                    valoffset = (0xffff & upper) << 16 | (0xffff & lower);
                }
                else
                    valoffset = getUnsignedInt(buff);
                ifd[i] = new TiffIFDEntry(tag, type, count, valoffset);
            }

            if (this.ifds == null)
                this.ifds = new ArrayList<TiffIFDEntry[]>(1);
            this.ifds.add(ifd);

            // If there's another IFD in this file, go get it (recursively)...
            long nextIFDOffset = getUnsignedInt(buff);
            if (nextIFDOffset > 0)
            {
                this.theChannel.position(nextIFDOffset);
                buff.clear().limit(2);
                this.theChannel.read(buff);
                buff.flip();
                readIFD(buff.getShort());
            }
        }
        catch (Exception ex)
        {
            String message = Logging.getMessage("GeotiffReader.BadIFD", ex.getMessage());
            Logging.logger().severe(message);
            throw new IOException(message);
        }
    }

    /*
     * Reads BYTE image data organized as a singular image plane (and pixel interleaved, in the case of color images).
     *
     */
    private byte[][] readPixelInterleaved8(int width, int height, int samplesPerPixel,
        long[] stripOffsets, long[] stripCounts) throws IOException
    {
        byte[][] data = new byte[1][width * height * samplesPerPixel];
        int offset = 0;

        ByteBuffer buff = ByteBuffer.wrap(data[0]);
        for (int i = 0; i < stripOffsets.length; i++)
        {
            this.theChannel.position(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((offset + len) >= data[0].length)
                len = data[0].length - offset;
            buff.limit(offset + len);
            this.theChannel.read(buff);
            offset += stripCounts[i];
        }

        return data;
    }

    /*
    *
    *
    */
    private byte[] readLZWCompressed(int width, int height, long offset, int samplesPerPixel,
        boolean differencing, long[] stripOffsets, long[] stripCounts) throws IOException
    {
        this.theChannel.position(offset);
        byte[] pixels = new byte[width * height * samplesPerPixel];
        int base = 0;
        for (int i = 0; i < stripOffsets.length; i++)
        {
            if (i > 0)
            {
                long skip = stripOffsets[i] - stripOffsets[i - 1] - stripCounts[i - 1];
                if (skip > 0)
                {
                    //in.skip(skip);
                    this.theChannel.position(this.theChannel.position() + skip);
                }
            }
            byte[] byteArray = new byte[(int) stripCounts[i]];
            ByteBuffer bBuffer = ByteBuffer.wrap(byteArray);
            int read = 0, left = byteArray.length;
            while (left > 0)
            {
                long r = this.theChannel.read(bBuffer);
                if (r == -1)
                {
                    break;
                }
                read += r;
                left -= r;
            }
            byteArray = lzwUncompress(byteArray, (width * samplesPerPixel));
            if (differencing)
            {
                for (int b = 0; b < byteArray.length; b++)
                {
                    if (b / samplesPerPixel % width == 0)
                        continue;
                    byteArray[b] += byteArray[b - samplesPerPixel];
                }
            }
            int k = 0;
            int bytesToRead = byteArray.length;
            bytesToRead = bytesToRead - (bytesToRead % width);
            int pmax = base + bytesToRead;
            if (pmax > width * height * samplesPerPixel)
                pmax = width * height * samplesPerPixel;

            for (int j = base; j < pmax; j++)
            {
                pixels[j] = byteArray[k++];
            }

            base += bytesToRead;
        }

        return pixels;
    }

    private static final int CLEAR_CODE = 256;
    private static final int EOI_CODE = 257;

    public byte[] lzwUncompress(byte[] input, int rowNumPixels)
    {
        if (input == null || input.length == 0)
            return input;
        byte[][] symbolTable = new byte[4096][1];
        int bitsToRead = 9; //default
        int nextSymbol = 258;
        int code;
        int oldCode = -1;

        ByteBuffer out = java.nio.ByteBuffer.allocate(rowNumPixels);
        CodeReader bb = new CodeReader(input);

        while (true)
        {
            code = bb.getCode(bitsToRead);

            if (code == EOI_CODE || code == -1)
                break;
            if (code == CLEAR_CODE)
            {
                // initialize symbol table
                for (int i = 0; i < 256; i++)
                {
                    symbolTable[i][0] = (byte) i;
                }
                nextSymbol = 258;
                bitsToRead = 9;
                code = bb.getCode(bitsToRead);

                if (code == EOI_CODE || code == -1)
                    break;

                out.put(symbolTable[code]);
                oldCode = code;
            }
            else
            {
                if (code < nextSymbol)
                {
                    out.put(symbolTable[code]);
                    ByteBuffer symbol = java.nio.ByteBuffer.allocate((symbolTable[oldCode].length + 1));
                    symbol.put(symbolTable[oldCode]);
                    symbol.put(symbolTable[code][0]);
                    symbolTable[nextSymbol] = symbol.array();
                    oldCode = code;
                    nextSymbol++;
                }
                else
                {
                    int size = symbolTable[oldCode].length + 1;
                    ByteBuffer symbol = java.nio.ByteBuffer.allocate(size);
                    symbol.put(symbolTable[oldCode]);
                    symbol.put(symbolTable[oldCode][0]);
                    byte[] outString = symbol.array();

                    out.put(outString);

                    symbolTable[nextSymbol] = outString;
                    oldCode = code;
                    nextSymbol++;
                }
                if (nextSymbol == 511)
                {
                    bitsToRead = 10;
                }
                if (nextSymbol == 1023)
                {
                    bitsToRead = 11;
                }
                if (nextSymbol == 2047)
                {
                    bitsToRead = 12;
                }
            }
        }
        return out.array();
    }

    /*
    * Reads BYTE image data organized as separate image planes.
    *
    */
    private byte[][] readPlanar8(int width, int height, int samplesPerPixel,
        long[] stripOffsets, long[] stripCounts, long rowsPerStrip) throws IOException
    {
        byte[][] data = new byte[samplesPerPixel][width * height];
        int band = 0;
        int offset = 0;
        int numRows = 0;

        ByteBuffer buff = ByteBuffer.wrap(data[band]);
        for (int i = 0; i < stripOffsets.length; i++)
        {
            this.theChannel.position(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((offset + len) >= data[band].length)
                len = data[band].length - offset;
            buff.limit(offset + len);
            this.theChannel.read(buff);
            offset += stripCounts[i];
            numRows += rowsPerStrip;
            if (numRows >= height && band < (data.length - 1))
            {
                buff = ByteBuffer.wrap(data[++band]);
                numRows = 0;
                offset = 0;
            }
        }

        return data;
    }

    /*
     * Reads SHORT image data organized as separate image planes.
     *
     */
    private short[][] readPlanar16(int width, int height, int samplesPerPixel,
        long[] stripOffsets, long[] stripCounts, long rowsPerStrip) throws IOException
    {
        short[][] data = new short[samplesPerPixel][width * height];
        int band = 0;
        int numRows = 0;

        ByteBuffer buff = ByteBuffer.allocateDirect(width * height * SHORT_SIZEOF);
        buff.order(this.tiffFileOrder);

        for (int i = 0; i < stripOffsets.length; i++)
        {
            this.theChannel.position(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((buff.position() + len) > data[band].length * SHORT_SIZEOF)
                len = data[band].length * SHORT_SIZEOF - buff.position();
            buff.limit(buff.position() + len);
            this.theChannel.read(buff);
            numRows += rowsPerStrip;
            if (numRows >= height)
            {
                buff.flip();
                ShortBuffer sbuff = buff.asShortBuffer();
                sbuff.get(data[band]);
                buff.clear();
                ++band;
                numRows = 0;
            }
        }

        return data;
    }

    /*
     * Reads FLOAT image data organized as separate image planes.
     *
     */
    private float[][] readPlanarFloat32(int width, int height, int samplesPerPixel,
        long[] stripOffsets, long[] stripCounts, long rowsPerStrip) throws IOException
    {
        float[][] data = new float[samplesPerPixel][width * height];
        int band = 0;
        int numRows = 0;

        ByteBuffer buff = ByteBuffer.allocateDirect(width * height * FLOAT_SIZEOF);
        buff.order(this.tiffFileOrder);

        for (int i = 0; i < stripOffsets.length; i++)
        {
            this.theChannel.position(stripOffsets[i]);
            int len = (int) stripCounts[i];
            if ((buff.position() + len) >= data[band].length * FLOAT_SIZEOF)
                len = data[band].length * FLOAT_SIZEOF - buff.position();
            buff.limit(buff.position() + len);
            this.theChannel.read(buff);
            numRows += rowsPerStrip;
            if (numRows >= height)
            {
                buff.flip();
                FloatBuffer fbuff = buff.asFloatBuffer();
                fbuff.get(data[band]);
                buff.clear();
                ++band;
                numRows = 0;
            }
        }

        return data;
    }

    /*
     * Reads a ColorMap.
     *
     */
    private byte[][] readColorMap(TiffIFDEntry colorMapEntry) throws IOException
    {
        // NOTE: TIFF gives total number of cmap values, which is 3 times the size of cmap table...
        // CLUT is composed of shorts, but we'll read as bytes (thus, the factor of 2)...
        int numEntries = (int) colorMapEntry.count / 3;
        byte[][] tmp = new byte[3][numEntries * 2];

        this.theChannel.position(colorMapEntry.asLong());

        // Unroll the loop; the TIFF spec says "...3 is the number of the counting, and the counting shall be 3..."
        // TIFF spec also says that all red values precede all green, which precede all blue.
        ByteBuffer buff = ByteBuffer.wrap(tmp[0]);
        this.theChannel.read(buff);
        buff = ByteBuffer.wrap(tmp[1]);
        this.theChannel.read(buff);
        buff = ByteBuffer.wrap(tmp[2]);
        this.theChannel.read(buff);

        // TIFF gives a ColorMap composed of unsigned shorts. Java's IndexedColorModel wants unsigned bytes.
        // Something's got to give somewhere...we'll do our best.
        byte[][] cmap = new byte[3][numEntries];
        for (int i = 0; i < 3; i++)
        {
            buff = ByteBuffer.wrap(tmp[i]);
            buff.order(this.tiffFileOrder);
            for (int j = 0; j < numEntries; j++)
            {
                cmap[i][j] = (byte) (0x00ff & buff.getShort());
            }
        }

        return cmap;
    }

    /*
     * Reads and returns an array of doubles from the file.
     *
     */
    private double[] readDoubles(TiffIFDEntry entry) throws IOException
    {
        double[] doubles = new double[(int) entry.count];
        ByteBuffer buff = ByteBuffer.allocateDirect(doubles.length * DOUBLE_SIZEOF);
        buff.order(this.tiffFileOrder);

        this.theChannel.position(entry.asOffset());
        this.theChannel.read(buff);

        buff.flip();
        DoubleBuffer dbuff = buff.asDoubleBuffer();
        dbuff.get(doubles);
        return doubles;
    }

    /*
     * Reads and returns an array of shorts from the file.
     *
     */
    private short[] readShorts(TiffIFDEntry entry) throws IOException
    {
        short[] shorts = new short[(int) entry.count];
        ByteBuffer buff = ByteBuffer.allocateDirect(shorts.length * SHORT_SIZEOF);
        buff.order(this.tiffFileOrder);

        this.theChannel.position(entry.asOffset());
        this.theChannel.read(buff);

        buff.flip();
        ShortBuffer sbuff = buff.asShortBuffer();
        sbuff.get(shorts);
        return shorts;
    }

    /*
     * Reads and returns an array of bytes from the file.
     *
     */
    private byte[] readBytes(TiffIFDEntry entry) throws IOException
    {
        byte[] bytes = new byte[(int) entry.count];
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        this.theChannel.position(entry.asOffset());
        this.theChannel.read(buff);
        return bytes;
    }

    /*
    * Returns the (first!) IFD-Entry with the given tag, or null if not found.
    *
    */
    private TiffIFDEntry getByTag(TiffIFDEntry[] ifd, int tag)
    {
        for (TiffIFDEntry anIfd : ifd)
        {
            if (anIfd.tag == tag)
            {
                return anIfd;
            }
        }
        return null;
    }

    /*
     * Utility method intended to read the array of StripOffsets or StripByteCounts.
     */
    private long[] getStripsArray(TiffIFDEntry stripsEntry) throws IOException
    {
        long[] offsets = new long[(int) stripsEntry.count];
        if (stripsEntry.count == 1)
        {
            // this is a special case, and it *does* happen!
            offsets[0] = stripsEntry.asLong();
        }
        else
        {
            long fileOffset = stripsEntry.asLong();
            this.theChannel.position(fileOffset);
            if (stripsEntry.type == TiffTypes.SHORT)
            {
                ByteBuffer buff = ByteBuffer.allocateDirect(offsets.length * SHORT_SIZEOF);
                this.theChannel.read(buff);
                buff.order(this.tiffFileOrder).flip();
                for (int i = 0; i < stripsEntry.count; i++)
                {
                    offsets[i] = getUnsignedShort(buff);
                }
            }
            else
            {
                ByteBuffer buff = ByteBuffer.allocateDirect(offsets.length * INTEGER_SIZEOF);
                this.theChannel.read(buff);
                buff.order(this.tiffFileOrder).flip();
                for (int i = 0; i < stripsEntry.count; i++)
                {
                    offsets[i] = getUnsignedInt(buff);
                }
            }
        }

        return offsets;
    }

    /*
    * Utility to extract bitsPerSample info (if present). This is a bit tricky, because if the samples/pixel == 1,
    * the bitsPerSample will fit in the offset/value field of the ImageFileDirectory element. In contrast, when
    * samples/pixel == 3, the 3 shorts that make up bitsPerSample don't fit in the offset/value field, so we have
    * to go track them down elsewhere in the file.  Finally, as bitsPerSample is optional for bilevel images,
    * we'll return something sane if this tag is absent.
    */
    private int[] getBitsPerSample(TiffIFDEntry entry) throws IOException
    {
        if (entry == null)
        {
            return new int[] {1};
        }  // the default according to the Tiff6.0 spec.

        if (entry.count == 1)
        {
            return new int[] {(int) entry.asLong()};
        }

        long[] tmp = getStripsArray(entry);
        int[] bits = new int[tmp.length];
        for (int i = 0; i < tmp.length; i++)
        {
            bits[i] = (int) tmp[i];
        }

        return bits;
    }

    /*
     * We need to check for a valid image index in several places. Consolidate that all here.
     * We throw an IllegalArgumentException if the index is not valid, otherwise, silently return.
     *
     */
    private void checkImageIndex(int imageIndex) throws IOException
    {
        if (imageIndex < 0 || imageIndex >= getNumImages())
        {
            String message = Logging.getMessage("GeotiffReader.BadImageIndex", imageIndex, 0, getNumImages());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
    }

    private int getUnsignedShort(ByteBuffer b)
    {
        return 0xffff & (int) b.getShort();
    }

    private long getUnsignedInt(ByteBuffer b)
    {
        return 0xffffffff & (long) b.getInt();
    }

    /*
     * Make sure we release this resource...
     *
     */
    protected void finalize() throws Throwable
    {
        try
        {
            this.sourceFile.close();
            super.finalize();
        }
        catch (Exception ex)
        { /* best effort */ }
    }

    private String sourceFilename;
    private RandomAccessFile sourceFile;
    private FileChannel theChannel;
    private ByteOrder tiffFileOrder;

    // The image-file-directory(ies)...
    private ArrayList<TiffIFDEntry[]> ifds = null;

    // Geotiff info...
    private GeoCodec geoCodec = null;

    // We need the size in bytes of various primitives...
    private static final int DOUBLE_SIZEOF = Double.SIZE / Byte.SIZE;
    private static final int FLOAT_SIZEOF = Float.SIZE / Byte.SIZE;
    private static final int INTEGER_SIZEOF = Integer.SIZE / Byte.SIZE;
    private static final int SHORT_SIZEOF = Short.SIZE / Byte.SIZE;

    //Inner class for reading individual codes during decompression
    private class CodeReader
    {
        private int currentByte;
        private int currentBit;
        private byte[] byteBuffer;
        private int bufferLength;
        private int[] backMask = new int[] {0x0000, 0x0001, 0x0003, 0x0007,
            0x000F, 0x001F, 0x003F, 0x007F};
        private int[] frontMask = new int[] {0x0000, 0x0080, 0x00C0, 0x00E0,
            0x00F0, 0x00F8, 0x00FC, 0x00FE};
        private boolean atEof;

        public CodeReader(byte[] byteBuffer)
        {
            //todo validate byteBuffer
            this.byteBuffer = byteBuffer;
            currentByte = 0;
            currentBit = 0;
            bufferLength = byteBuffer.length;
        }

        public int getCode(int numBitsToRead)
        {
            if (numBitsToRead < 0)
                return 0;
            if (atEof)
                return -1; //end of file
            int returnCode = 0;
            while (numBitsToRead != 0 && !atEof)
            {
                if (numBitsToRead >= 8 - currentBit)
                {
                    if (currentBit == 0) //get first
                    {
                        returnCode = returnCode << 8;
                        int cb = ((int) byteBuffer[currentByte]);
                        returnCode += (cb < 0 ? 256 + cb : cb);
                        numBitsToRead -= 8;
                        currentByte++;
                    }
                    else
                    {
                        returnCode = returnCode << (8 - currentBit);
                        returnCode += ((int) byteBuffer[currentByte]) & backMask[8 - currentBit];
                        numBitsToRead -= (8 - currentBit);
                        currentBit = 0;
                        currentByte++;
                    }
                }
                else
                {
                    returnCode = returnCode << numBitsToRead;
                    int cb = ((int) byteBuffer[currentByte]);
                    cb = (cb < 0 ? 256 + cb : cb);
                    returnCode += ((cb) & (0x00FF - frontMask[currentBit])) >> (8 - (currentBit + numBitsToRead));
                    currentBit += numBitsToRead;
                    numBitsToRead = 0;
                }
                if (currentByte == bufferLength)  //at eof
                {
                    atEof = true;
                    return returnCode;
                }
            }
            return returnCode;
        }
    }

}