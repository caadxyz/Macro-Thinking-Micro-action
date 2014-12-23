/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import com.sun.opengl.util.BufferUtil;

import java.nio.*;

/**
 * A collection of useful {@link Buffer} methods, all static.
 *
 * @author dcollins
 * @version $Id: WWBufferUtil.java 12800 2009-11-17 20:24:42Z dcollins $
 */
public class WWBufferUtil
{
    /** The size of a char primitive type, in bytes. */
    public static final int SIZEOF_CHAR = 2;

    /**
     * Allocates a new direct {@link CharBuffer} of the specified size, in chars.
     *
     * @param size the new buffer's size.
     *
     * @return the new buffer.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static CharBuffer newCharBuffer(int size)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SIZEOF_CHAR * size).order(ByteOrder.nativeOrder());
        return byteBuffer.asCharBuffer();
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in bytes. The BufferWrapper is backed by a Buffer of
     * bytes.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newByteBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer buffer = (allocateDirect ? BufferUtil.newByteBuffer(size) : ByteBuffer.allocate(size));
        return new BufferWrapper.ByteBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in shorts. The BufferWrapper is backed by a Buffer
     * of shorts.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newShortBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ShortBuffer buffer = (allocateDirect ? BufferUtil.newShortBuffer(size) : ShortBuffer.allocate(size));
        return new BufferWrapper.ShortBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in ints. The BufferWrapper is backed by a Buffer of
     * ints.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newIntBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        IntBuffer buffer = (allocateDirect ? BufferUtil.newIntBuffer(size) : IntBuffer.allocate(size));
        return new BufferWrapper.IntBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in floats. The BufferWrapper is backed by a Buffer
     * of floats.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newFloatBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FloatBuffer buffer = (allocateDirect ? BufferUtil.newFloatBuffer(size) : FloatBuffer.allocate(size));
        return new BufferWrapper.FloatBufferWrapper(buffer);
    }

    /**
     * Allocates a new {@link BufferWrapper} of the specified size, in doubles. The BufferWrapper is backed by a Buffer
     * of doubles.
     *
     * @param size           the new BufferWrapper's size.
     * @param allocateDirect true to allocate and return a direct buffer, false to allocate and return a non-direct
     *                       buffer.
     *
     * @return the new BufferWrapper.
     *
     * @throws IllegalArgumentException if size is negative.
     */
    public static BufferWrapper newDoubleBufferWrapper(int size, boolean allocateDirect)
    {
        if (size < 0)
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", size);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DoubleBuffer buffer = (allocateDirect ? BufferUtil.newDoubleBuffer(size) : DoubleBuffer.allocate(size));
        return new BufferWrapper.DoubleBufferWrapper(buffer);
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in bytes.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static ByteBuffer copyOf(ByteBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ByteBuffer newBuffer = BufferUtil.newByteBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in chars.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static CharBuffer copyOf(CharBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        CharBuffer newBuffer = newCharBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in shorts.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static ShortBuffer copyOf(ShortBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        ShortBuffer newBuffer = BufferUtil.newShortBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in ints.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static IntBuffer copyOf(IntBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        IntBuffer newBuffer = BufferUtil.newIntBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in floats.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static FloatBuffer copyOf(FloatBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        FloatBuffer newBuffer = BufferUtil.newFloatBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in doubles.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static DoubleBuffer copyOf(DoubleBuffer buffer, int newSize)
    {
        if (newSize < 0 || newSize < buffer.remaining())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        DoubleBuffer newBuffer = BufferUtil.newDoubleBuffer(newSize);

        BufferStateHandler bsh = new BufferStateHandler();
        bsh.pushState(buffer);
        try
        {
            newBuffer.put(buffer);
            newBuffer.rewind();
        }
        finally
        {
            bsh.popState(buffer);
        }

        return newBuffer;
    }

    /**
     * Returns a copy of the specified buffer, with the specified new size. The new size must be greater than or equal
     * to the specified buffer's size. If the new size is greater than the specified buffer's size, this returns a new
     * buffer which is partially filled with the contents of the specified buffer.
     *
     * @param buffer  the buffer to copy.
     * @param newSize the new buffer's size, in doubles.
     * @param factory the factory used to create the new buffer.
     *
     * @return the new buffer, with the specified size.
     *
     * @throws IllegalArgumentException if the buffer is null, if the new size is negative, or if the new size is less
     *                                  than the buffer's remaing elements.
     */
    public static BufferWrapper copyOf(BufferWrapper buffer, int newSize, BufferFactory factory)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (newSize < 0 || newSize < buffer.length())
        {
            String message = Logging.getMessage("generic.SizeOutOfRange", newSize);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (factory == null)
        {
            String message = Logging.getMessage("nullValue.FactoryIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        BufferWrapper newBuffer = factory.newBuffer(newSize);
        newBuffer.putSubBuffer(0, buffer);
        return newBuffer;
    }

    /**
     * Returns the minimum and maximum floating point values in the specified buffer. Values equivalent to the specified
     * <code>missingDataSignal</code> are ignored. This returns null if the buffer is empty or contains only missing
     * values.
     *
     * @param buffer            the buffer to search for the minimum and maximum values.
     * @param missingDataSignal the number indicating a specific floating point value to ignore.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the buffer
     *         is empty or contains only missing values.
     *
     * @throws IllegalArgumentException if the buffer is null.
     */
    public static double[] computeExtremeValues(BufferWrapper buffer, double missingDataSignal)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < buffer.length(); i++)
        {
            double value = buffer.getDouble(i);
            
            if (Double.compare(value, missingDataSignal) == 0)
                continue;

            if (min > value)
                min = value;
            if (max < value)
                max = value;
        }

        if (Double.compare(min, Double.MAX_VALUE) == 0 || Double.compare(max, -Double.MAX_VALUE) == 0)
            return null;

        return new double[] {min, max};
    }

    /**
     * Returns the minimum and maximum floating point values in the specified buffer. Values equivalent to
     * <code>Double.NaN</code> are ignored. This returns null if the buffer is empty or contains only NaN values.
     *
     * @param buffer the buffer to search for the minimum and maximum values.
     *
     * @return an array containing the minimum value in index 0 and the maximum value in index 1, or null if the buffer
     *         is empty or contains only NaN values.
     *
     * @throws IllegalArgumentException if the buffer is null.
     */
    public static double[] computeExtremeValues(BufferWrapper buffer)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        return computeExtremeValues(buffer, Double.NaN);
    }
}
