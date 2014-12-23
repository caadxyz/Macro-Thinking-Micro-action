/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.formats.dds;

import gov.nasa.worldwind.util.*;

import java.awt.image.*;

/**
 * <code>DDSCompressor</code> will convert an in-memory image into a DDS file encoded with one of the DXT block
 * compression schemes. If the caller wants to encode using a certain type of DXT compression,
 * <code>DDSCompressor</code> provides the appropriate methods to do that. Otherwise, <code>DDSCompressor</code> will
 * choose the DXT compression scheme that best suits the source image.
 * <p>
 * Each compression method accepts a reference to a <code>DXTCompressionAttributes</code>. This compressor will perform
 * the appropriate actions according to the attributes, such as building mip maps and converting the source image to a
 * premultiplied alpha format.
 *
 * @author dcollins
 * @version $Id: DDSCompressor.java 9600 2009-03-22 20:04:40Z tgaskins $
 */
public class DDSCompressor
{
    /**
     * Creates a new <code>DDSCompressor</code>, but otherwise does nothing.
     */
    public DDSCompressor()
    {
    }

    /**
     * Convenience method to convert the specified <code>imageBuffer</code> to DDS according to the default attributes.
     * The bytes in <code>imageBuffer</code> must be readable by {@link javax.imageio.ImageIO}. Once the image data is
     * read, this is equivalent to calling {#compressImage(java.awt.image.BufferedImage)} with the BufferedImage
     * created by ImageIO. If the image data is not in a format understood by ImageIO, this method will return null.
     *
     * @param imageBuffer image file data to convert to the DDS file format.
     *
     * @return little endian ordered ByteBuffer containing the dds file bytes, or null if the bytes in
     *         <code>imageBuffer</code> are not in a format understood by ImageIO.
     * @throws java.io.IOException if the bytes in <code>imageBuffer</code> are in a format understood by ImageIO, but
     *         the image data cannot be read by ImageIO.
     * @throws IllegalArgumentException if <code>imageBuffer</code> is null.
     */
    public static java.nio.ByteBuffer compressImageBuffer(java.nio.ByteBuffer imageBuffer) throws java.io.IOException
    {
        if (imageBuffer == null)
        {
            String message = Logging.getMessage("nullValue.Image");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.io.InputStream inputStream = WWIO.getInputStreamFromByteBuffer(imageBuffer);
        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(inputStream);
        if (image == null)
        {
            return null;
        }

        return compressImage(image);
    }

    /**
     * Convenience method to convert the specified image <code>file</code> to DDS according to the default attributes.
     * The <code>file</code> must be readable by {@link javax.imageio.ImageIO}. Once the file is read, this is
     * equivalent to calling {#compressImage(java.awt.image.BufferedImage)} with the BufferedImage created by ImageIO.
     * If the file is not in a format understood by ImageIO, this method will return null.
     *
     * @param file image file to convert to the DDS file format.
     *
     * @return little endian ordered ByteBuffer containing the dds file bytes, or null if the <code>file</code> is not
     *         in a format understood by ImageIO.
     * @throws java.io.IOException if <code>file</code> is in a format understood by ImageIO, but the image data
     *         cannot be read by ImageIO.
     * @throws IllegalArgumentException if <code>file</code> is null, does not exist, or read permission is not
     *                                  allowed on the <code>file</code>.
     */
    public static java.nio.ByteBuffer compressImageFile(java.io.File file) throws java.io.IOException
    {
        if (file == null)
        {
            String message = Logging.getMessage("nullValue.FileIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!file.exists() || !file.canRead())
        {
            String message = Logging.getMessage("DDSConverter.NoFileOrNoPermission");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
        if (image == null)
        {
            return null;
        }

        return compressImage(image);
    }

    /**
     * Convenience method to convert the specified <code>image</code> to DDS according to the default attributes. This
     * will choose the DXT compression format best suited for the image type.
     *
     * @param image image to convert to the DDS file format.
     *
     * @return little endian ordered ByteBuffer containing the dds file bytes.
     * @throws IllegalArgumentException if <code>image</code> is null, or if <code>image</code> has non power of two
     *                                  dimensions.
     */
    public static java.nio.ByteBuffer compressImage(java.awt.image.BufferedImage image)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!WWMath.isPowerOfTwo(image.getWidth()) || !WWMath.isPowerOfTwo(image.getHeight()))
        {
            String message = Logging.getMessage("generic.InvalidImageSize", image.getWidth(), image.getHeight());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DDSCompressor compressor = new DDSCompressor();
        DXTCompressionAttributes attributes = new DXTCompressionAttributes();
        attributes.setBuildMipmaps(true); // Always build mipmaps.
        attributes.setPremultiplyAlpha(true); // Always create premultiplied alpha format files..
        attributes.setDXTFormat(0); // Allow the DDSCompressor to choose the appropriate DXT format.
        return compressor.compressImage(image, attributes);
    }

    /**
     * Convert the specified <code>image</code> to DDS according to the <code>attributes</code>. If the caller
     * specified a DXT format in the attributes, then we return a compressor matching that format. Otherwise, we
     * choose one automatically from the image type. If no choice can be made from the image type, we default to using
     * a DXT3 compressor.
     *
     * @param image image to convert to the DDS file format.
     * @param attributes attributes that will control the compression.
     *
     * @return buffer little endian ordered ByteBuffer containing the dds file bytes.
     * @throws IllegalArgumentException if either <code>image</code> or <code>attributes</code> are null, or if
     *                                  <code>image</code> has non power of two dimensions.
     */
    public java.nio.ByteBuffer compressImage(java.awt.image.BufferedImage image, DXTCompressionAttributes attributes)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!WWMath.isPowerOfTwo(image.getWidth()) || !WWMath.isPowerOfTwo(image.getHeight()))
        {
            String message = Logging.getMessage("generic.InvalidImageSize", image.getWidth(), image.getHeight());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DXTCompressor compressor = this.getDXTCompressor(image, attributes);
        return this.doCompressImage(compressor, image, attributes);
    }

    /**
     * Convert the specified <code>image</code> to DDS using the DXT1 codec, and otherwise according to the
     * <code>attributes</code>.
     *
     * @param image image to convert to the DDS file format using the DXT1 codec.
     * @param attributes attributes that will control the compression.
     *
     * @return buffer little endian ordered ByteBuffer containing the dds file bytes.
     * @throws IllegalArgumentException if either <code>image</code> or <code>attributes</code> are null, or if
     *                                  <code>image</code> has non power of two dimensions.
     */
    public java.nio.ByteBuffer compressImageDXT1(java.awt.image.BufferedImage image,
        DXTCompressionAttributes attributes)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!WWMath.isPowerOfTwo(image.getWidth()) || !WWMath.isPowerOfTwo(image.getHeight()))
        {
            String message = Logging.getMessage("generic.InvalidImageSize", image.getWidth(), image.getHeight());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DXT1Compressor compressor = new DXT1Compressor();
        return this.doCompressImage(compressor, image, attributes);
    }

    /**
     * Convert the specified <code>image</code> to DDS using the DXT3 codec, and otherwise according to the
     * <code>attributes</code>.
     *
     * @param image image to convert to the DDS file format using the DXT3 codec.
     * @param attributes attributes that will control the compression.
     *
     * @return buffer little endian ordered ByteBuffer containing the dds file bytes.
     * @throws IllegalArgumentException if either <code>image</code> or <code>attributes</code> are null, or if
     *                                  <code>image</code> has non power of two dimensions.
     */
    public java.nio.ByteBuffer compressImageDXT3(java.awt.image.BufferedImage image,
        DXTCompressionAttributes attributes)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (!WWMath.isPowerOfTwo(image.getWidth()) || !WWMath.isPowerOfTwo(image.getHeight()))
        {
            String message = Logging.getMessage("generic.InvalidImageSize", image.getWidth(), image.getHeight());
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DXT3Compressor compressor = new DXT3Compressor();
        return this.doCompressImage(compressor, image, attributes);
    }

    protected java.nio.ByteBuffer doCompressImage(DXTCompressor compressor, java.awt.image.BufferedImage image,
        DXTCompressionAttributes attributes)
    {
        // Create the DDS header structure that describes the specified image, compressor, and compression attributes.
        DDSHeader header = this.createDDSHeader(compressor, image, attributes);

        // Compute the DDS file size and mip map levels. If the attributes specify to build mip maps, then we compute
        // the total file size including mip maps, create a chain of mip map images, and update the DDS header to
        // describe the number of mip map levels. Otherwise, we compute the file size for a single image and do nothing
        // to the DDS header.
        java.awt.image.BufferedImage[] mipMapLevels = null;
        int fileSize = 4 + header.getSize();

        if (attributes.isBuildMipmaps())
        {
            mipMapLevels = this.buildMipMaps(image, attributes);
            for (java.awt.image.BufferedImage mipMapImage : mipMapLevels)
            {
                fileSize += compressor.getCompressedSize(mipMapImage, attributes);
            }

            header.setFlags(header.getFlags()
                | DDSConstants.DDSD_MIPMAPCOUNT);
            header.setMipMapCount(mipMapLevels.length);
        }
        else
        {
            fileSize += compressor.getCompressedSize(image, attributes);
        }

        // Create a little endian buffer that will hold the bytes of the DDS file.
        java.nio.ByteBuffer buffer = this.createBuffer(fileSize);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // Write the DDS magic number and DDS header to the file.
        buffer.putInt(DDSConstants.MAGIC);
        this.writeDDSHeader(header, buffer);

        // Write the compressed DXT blocks to the DDS file. If the attributes specify to build mip maps, then we write
        // each mip map level to the DDS file, starting with level 0 and ending with level N. Otherwise, we write a
        // single image to the DDS file.
        if (mipMapLevels == null)
        {
            compressor.compressImage(image, attributes, buffer);
        }
        else
        {
            for (java.awt.image.BufferedImage mipMapImage : mipMapLevels)
            {
                compressor.compressImage(mipMapImage, attributes, buffer);
            }
        }

        buffer.rewind();
        return buffer;
    }


    protected DXTCompressor getDXTCompressor(java.awt.image.BufferedImage image, DXTCompressionAttributes attributes)
    {
        // If the caller specified a DXT format in the attributes, then we return a compressor matching that format.
        // Otherwise, we choose one automatically from the image type. If no choice can be made from the image type,
        // we default to using a DXT3 compressor.

        if (attributes.getDXTFormat() == DDSConstants.D3DFMT_DXT1)
        {
            return new DXT1Compressor();
        }
        else if (attributes.getDXTFormat() == DDSConstants.D3DFMT_DXT2
              || attributes.getDXTFormat() == DDSConstants.D3DFMT_DXT3)
        {
            return new DXT3Compressor();
        }
        else if (!image.getColorModel().hasAlpha())
        {
            return new DXT1Compressor();
        }
        else
        {
            return new DXT3Compressor();
        }
    }

    protected java.nio.ByteBuffer createBuffer(int size)
    {
        return java.nio.ByteBuffer.allocateDirect(size);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected java.awt.image.BufferedImage[] buildMipMaps(java.awt.image.BufferedImage image,
        DXTCompressionAttributes attributes)
    {
        // Build the mipmap chain using a premultiplied alpha image format. This is necessary to ensure that
        // transparent colors do not bleed into the opaque colors. For example, without premultiplied alpha the colors
        // in a totally transparent pixel may contribute when one mipmap level is filtered (with either a box or a
        // bilinear filter) to produce the pixels for the next level.
        //
        // The DXT color block extractor typically accessed BufferedImage data via a call to getRGB(). This returns
        // a packed 8888 ARGB int, where the color components are known to be not premultiplied, and in the sRGB color
        // space. Therefore computing mipmaps in this way does not affect the rest of the DXT pipeline, unless color
        // data is accessed directly. In this case, such code would be responsible for recognizing the color model
        // (premultiplied) and behaving accordingly.

        int mipmapImageType = BufferedImage.TYPE_INT_ARGB_PRE;
        int maxLevel = ImageUtil.getMaxMipmapLevel(image.getWidth(), image.getHeight());

        return ImageUtil.buildMipmaps(image, mipmapImageType, maxLevel);
    }

    protected DDSHeader createDDSHeader(DXTCompressor compressor, java.awt.image.BufferedImage image,
        DXTCompressionAttributes attributes)
    {
        DDSPixelFormat pixelFormat = new DDSPixelFormat();
        pixelFormat.setFlags(pixelFormat.getFlags()
            | DDSConstants.DDPF_FOURCC);
        pixelFormat.setFourCC(compressor.getDXTFormat());

        DDSHeader header = new DDSHeader();
        header.setFlags(header.getFlags()
            | DDSConstants.DDSD_WIDTH
            | DDSConstants.DDSD_HEIGHT
            | DDSConstants.DDSD_LINEARSIZE
            | DDSConstants.DDSD_PIXELFORMAT
            | DDSConstants.DDSD_CAPS);
        header.setWidth(image.getWidth());
        header.setHeight(image.getHeight());
        header.setLinearSize(compressor.getCompressedSize(image, attributes));
        header.setPixelFormat(pixelFormat);
        header.setCaps(header.getCaps() | DDSConstants.DDSCAPS_TEXTURE);

        return header;
    }

    /**
     * Documentation on the DDS header format is available at
     * http://msdn.microsoft.com/en-us/library/bb943982(VS.85).aspx
     *
     * @param header header structure to write.
     * @param buffer buffer that will receive the header structure bytes.
     */
    protected void writeDDSHeader(DDSHeader header, java.nio.ByteBuffer buffer)
    {
        int pos = buffer.position();

        buffer.putInt(header.getSize());            // dwSize
        buffer.putInt(header.getFlags());           // dwFlags
        buffer.putInt(header.getHeight());          // dwHeight
        buffer.putInt(header.getWidth());           // dwWidth
        buffer.putInt(header.getLinearSize());      // dwLinearSize
        buffer.putInt(header.getDepth());           // dwDepth
        buffer.putInt(header.getMipMapCount());     // dwMipMapCount
        buffer.position(buffer.position() + 44);    // dwReserved1[11] (unused)
        this.writeDDSPixelFormat(header.getPixelFormat(), buffer); // ddpf
        buffer.putInt(header.getCaps());            // dwCaps
        buffer.putInt(header.getCaps2());           // dwCaps2
        buffer.putInt(header.getCaps3());           // dwCaps3
        buffer.putInt(header.getCaps4());           // dwCaps4
        buffer.position(buffer.position() + 4);     // dwReserved2 (unused)

        buffer.position(pos + header.getSize());
    }

    /**
     * Documentation on the DDS pixel format is available at
     * http://msdn.microsoft.com/en-us/library/bb943984(VS.85).aspx
     *
     * @param pixelFormat pixel format structure to write.
     * @param buffer buffer that will receive the pixel format structure bytes.
     */
    protected void writeDDSPixelFormat(DDSPixelFormat pixelFormat, java.nio.ByteBuffer buffer)
    {
        int pos = buffer.position();

        buffer.putInt(pixelFormat.getSize());           // dwSize
        buffer.putInt(pixelFormat.getFlags());          // dwFlags
        buffer.putInt(pixelFormat.getFourCC());         // dwFourCC
        buffer.putInt(pixelFormat.getRGBBitCount());    // dwRGBBitCount
        buffer.putInt(pixelFormat.getRBitMask());       // dwRBitMask
        buffer.putInt(pixelFormat.getGBitMask());       // dwGBitMask
        buffer.putInt(pixelFormat.getBBitMask());       // dwBBitMask
        buffer.putInt(pixelFormat.getABitMask());       // dwABitMask

        buffer.position(pos + pixelFormat.getSize());
    }
}
