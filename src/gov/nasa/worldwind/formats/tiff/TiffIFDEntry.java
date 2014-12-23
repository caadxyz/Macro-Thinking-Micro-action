/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.formats.tiff;

/**
 * A bag for holding individual entries from a Tiff ImageFileDirectory.
 *
 * @author brownrigg
 * @version $Id: TiffIFDEntry.java 6539 2008-09-10 16:39:49Z rick $
 */
public class TiffIFDEntry
{
    public TiffIFDEntry(int tag, int type, long count, long valOffset) throws IllegalArgumentException
    {
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.valOffset = valOffset;
    }

    public long asLong() throws IllegalStateException
    {
        if (this.type != TiffTypes.SHORT && this.type != TiffTypes.LONG)
            throw new IllegalStateException("Attempt to access Tiff IFD-entry as int: tag/type=" +
                Long.toHexString(tag) + "/" + type);
       if (this.type == TiffTypes.SHORT && this.count == 1)
            return valOffset >> 16;
        else
            return valOffset;
    }

    public long asOffset()
    {
        return valOffset;
    }

    // package visibility is intended...
    int tag;
    int type;
    long count;
    long valOffset;
}
