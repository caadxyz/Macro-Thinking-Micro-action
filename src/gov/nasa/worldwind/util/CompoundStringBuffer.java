/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

/**
 * An implementation of {@link CompoundBuffer} which manages a collection of {@link CharSequence} buffers in a single
 * {@link StringBuilder}.
 *
 * @author dcollins
 * @version $Id: CompoundStringBuffer.java 12458 2009-08-14 20:07:51Z dcollins $
 */
public class CompoundStringBuffer extends CompoundBuffer<StringBuilder, CharSequence>
{
    /**
     * Constructs a CompoundStringBuffer with the specified initial capacity.
     *
     * @param initialCapacity   the compound buffer's initial capacity.
     * @param subSequenceLength an estimate of the size of each sub-sequence.
     *
     * @throws IllegalArgumentException if the initialCapacity is negative.
     */
    public CompoundStringBuffer(int initialCapacity, int subSequenceLength)
    {
        super(initialCapacity, new StringBuilder(initialCapacity * subSequenceLength));
    }

    /**
     * Returns the substring identified by the specified index. Index i corresponds to the ith string.
     *
     * @param index the index of the string to get.
     *
     * @return the string at the specified index.
     *
     * @throws IllegalArgumentException if the index is out of range.
     */
    public String getSubString(int index)
    {
        CharSequence subSequence = this.getSubBuffer(index);
        return subSequence.toString();
    }

    protected int getSize(CharSequence buffer)
    {
        return buffer.length();
    }

    protected CharSequence getSubBuffer(int position, int length)
    {
        CharSequence subSequence = this.buffer.subSequence(position, position + length);
        return WWUtil.trimCharSequence(subSequence);
    }

    protected void append(CharSequence buffer)
    {
        this.buffer.append(buffer);
    }
}
