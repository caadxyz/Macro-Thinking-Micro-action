/*
Copyright (C) 2001, 2009 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.AVKey;

import java.awt.*;
import java.io.FileFilter;
import java.util.ArrayList;

/**
 * @author tag
 * @version $Id: WWUtil.java 12993 2010-01-09 06:32:06Z tgaskins $
 */
public class WWUtil
{
    /**
     * Converts a specified string to an integer value. Returns null if the string cannot be converted.
     *
     * @param s the string to convert.
     *
     * @return integer value of the string, or null if the string cannot be converted.
     *
     * @throws IllegalArgumentException if the string is null.
     */
    public static Integer convertStringToInteger(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            if (s.length() == 0)
                return null;

            return Integer.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.ConversionError", s);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }

    /**
     * Converts a specified string to a floating point value. Returns null if the string cannot be converted.
     *
     * @param s the string to convert.
     *
     * @return floating point value of the string, or null if the string cannot be converted.
     *
     * @throws IllegalArgumentException if the string is null.
     */
    public static Double convertStringToDouble(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            if (s.length() == 0)
                return null;

            return Double.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.ConversionError", s);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }

    /**
     * Converts a specified string to a long integer value. Returns null if the string cannot be converted.
     *
     * @param s the string to convert.
     *
     * @return long integer value of the string, or null if the string cannot be converted.
     *
     * @throws IllegalArgumentException if the string is null.
     */
    public static Long convertStringToLong(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            if (s.length() == 0)
                return null;

            return Long.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.ConversionError", s);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }

    /**
     * Converts a specified string to a boolean value. Returns null if the string cannot be converted.
     *
     * @param s the string to convert.
     *
     * @return boolean value of the string, or null if the string cannot be converted.
     *
     * @throws IllegalArgumentException if the string is null.
     */
    public static Boolean convertStringToBoolean(String s)
    {
        if (s == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        try
        {
            if (s.length() == 0)
                return null;

            return Boolean.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.ConversionError", s);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }
    }

    /**
     * Returns a sub sequence of the specified {@link CharSequence}, with leading and trailing whitespace omitted. If
     * the CharSequence has length zero, this returns a reference to the CharSequence. If the CharSequence represents
     * and empty character sequence, this returns an empty CharSequence.
     *
     * @param charSequence the CharSequence to trim.
     *
     * @return a sub sequence with leading and trailing whitespace omitted.
     *
     * @throws IllegalArgumentException if the charSequence is null.
     */
    public static CharSequence trimCharSequence(CharSequence charSequence)
    {
        if (charSequence == null)
        {
            String message = Logging.getMessage("nullValue.CharSequenceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        int len = charSequence.length();
        if (len == 0)
            return charSequence;

        int start, end;

        for (start = 0; (start < len) && charSequence.charAt(start) == ' '; start++)
        {
        }

        for (end = charSequence.length() - 1; (end > start) && charSequence.charAt(end) == ' '; end--)
        {
        }

        return charSequence.subSequence(start, end + 1);
    }

    public static void alignComponent(Component parent, Component child, String alignment)
    {
        Dimension prefSize = child.getPreferredSize();
        java.awt.Point parentLocation = parent != null ? parent.getLocation() : new java.awt.Point(0, 0);
        Dimension parentSize = parent != null ? parent.getSize() : Toolkit.getDefaultToolkit().getScreenSize();

        int x = parentLocation.x;
        int y = parentLocation.y;

        if (alignment != null && alignment.equals(AVKey.RIGHT))
        {
            x += parentSize.width - 50;
            y += parentSize.height - prefSize.height;
        }
        else if (alignment != null && alignment.equals(AVKey.CENTER))
        {
            x += (parentSize.width - prefSize.width) / 2;
            y += (parentSize.height - prefSize.height) / 2;
        }
        // else it's left aligned by default

        child.setLocation(x, y);
    }

    public static Color makeColorBrighter(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] hsbComponents = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbComponents);
        float hue = hsbComponents[0];
        float saturation = hsbComponents[1];
        float brightness = hsbComponents[2];

        saturation /= 3f;
        brightness *= 3f;

        if (saturation < 0f)
            saturation = 0f;

        if (brightness > 1f)
            brightness = 1f;

        int rgbInt = Color.HSBtoRGB(hue, saturation, brightness);

        return new Color(rgbInt);
    }

    public static Color makeColorDarker(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] hsbComponents = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsbComponents);
        float hue = hsbComponents[0];
        float saturation = hsbComponents[1];
        float brightness = hsbComponents[2];

        saturation *= 3f;
        brightness /= 3f;

        if (saturation > 1f)
            saturation = 1f;

        if (brightness < 0f)
            brightness = 0f;

        int rgbInt = Color.HSBtoRGB(hue, saturation, brightness);

        return new Color(rgbInt);
    }

    public static Color computeContrastingColor(Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] compArray = new float[4];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
        int colorValue = compArray[2] < 0.5f ? 255 : 0;
        int alphaValue = color.getAlpha();

        return new Color(colorValue, colorValue, colorValue, alphaValue);
    }

    /**
     * Returns a String encoding of the specified <code>color</code>. The Color can be restored with a call to {@link
     * #decodeColor(String)}.
     *
     * @param color Color to encode.
     *
     * @return String encoding of the specified <code>color</code>.
     *
     * @throws IllegalArgumentException If <code>color</code> is null.
     */
    public static String encodeColor(java.awt.Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Encode the red, green, blue, and alpha components
        int rgba = (color.getRed() & 0xFF) << 24
            | (color.getGreen() & 0xFF) << 16
            | (color.getBlue() & 0xFF) << 8
            | (color.getAlpha() & 0xFF);
        return String.format("%#08X", rgba);
    }

    /**
     * Returns the Color described by the String <code>encodedString</code>. This understands Colors encoded with a call
     * to {@link #encodeColor(java.awt.Color)}. If <code>encodedString</code> cannot be decoded, this method returns
     * null.
     *
     * @param encodedString String to decode.
     *
     * @return Color decoded from the specified <code>encodedString</code>, or null if the String cannot be decoded.
     *
     * @throws IllegalArgumentException If <code>encodedString</code> is null.
     */
    public static java.awt.Color decodeColor(String encodedString)
    {
        if (encodedString == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (!encodedString.startsWith("0x") && !encodedString.startsWith("0X"))
            return null;

        // The hexadecimal representation for an RGBA color can result in a value larger than
        // Integer.MAX_VALUE (for example, 0XFFFF). Therefore we decode the string as a long,
        // then keep only the lower four bytes.
        Long longValue;
        try
        {
            longValue = Long.parseLong(encodedString.substring(2), 16);
        }
        catch (NumberFormatException e)
        {
            String message = Logging.getMessage("generic.ConversionError", encodedString);
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            return null;
        }

        int i = (int) (longValue & 0xFFFFFFFFL);
        return new java.awt.Color(
            (i >> 24) & 0xFF,
            (i >> 16) & 0xFF,
            (i >> 8) & 0xFF,
            i & 0xFF);
    }

    public static String[] listFileNames(String filePath, FileFilter filter)
    {
        if (filePath == null)
        {
            String msg = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Do not recurse.
        return doListFileNames(filePath, filter, false, false);
    }

    public static String[] listAllFileNames(String filePath, FileFilter filter)
    {
        if (filePath == null)
        {
            String msg = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, and continue to search each branch after a match is found.
        return doListFileNames(filePath, filter, true, false);
    }

    public static String[] listTopFileNames(String filePath, FileFilter filter)
    {
        if (filePath == null)
        {
            String msg = Logging.getMessage("nullValue.FilePathIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (filter == null)
        {
            String msg = Logging.getMessage("nullValue.FilterIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // Recurse, but stop recursively searching a branch after a match is found.
        return doListFileNames(filePath, filter, true, true);
    }

    protected static String[] doListFileNames(String filePath, FileFilter filter, boolean recurse,
        boolean exitBranchOnFirstMatch)
    {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists())
        {
            String msg = Logging.getMessage("generic.FileNotFound", filePath);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        ArrayList<String> names = new ArrayList<String>();
        doListFileNames(file, filter, recurse, exitBranchOnFirstMatch, names);

        String[] array = new String[names.size()];
        names.toArray(array);
        return array;
    }

    protected static void doListFileNames(java.io.File file, FileFilter filter,
        boolean recurse, boolean exitBranchOnFirstMatch, java.util.Collection<String> names)
    {
        java.util.ArrayList<java.io.File> subDirs = new java.util.ArrayList<java.io.File>();
        boolean haveMatch = false;

        // Search the children of the specified directory. If the child is a directory, append it to the list of sub
        // directories to search later. Otherwise, try to list the file as a match. If the file is a match and
        // exitBranchOnFirstMatch is true, then exit this branch without considering any other files. This has the
        // effect of choosing files closest to the search root.
        for (java.io.File childFile : file.listFiles())
        {
            if (childFile == null)
                continue;

            if (listFile(childFile, filter, names))
                haveMatch = true;

            if (childFile.isDirectory())
                subDirs.add(childFile);
        }

        if (!recurse || (haveMatch && exitBranchOnFirstMatch))
            return;

        // Recursively search each sub-directory. If exitBranchOnFirstMatch is true, then we did not find a match under
        // this directory.
        for (java.io.File childDir : subDirs)
        {
            doListFileNames(childDir, filter, recurse, exitBranchOnFirstMatch, names);
        }
    }

    protected static boolean listFile(java.io.File file, FileFilter filter,
        java.util.Collection<String> names)
    {
        if (!filter.accept(file))
            return false;

        names.add(file.getPath());
        return true;
    }

    /**
     *  Determine whether an object reference is null or a reference to an empty string.
     * @param s the refernce to examine.
     * @return true if the reference is null or is a zero-length {@link String}.
     */
    public static boolean isEmpty(Object s)
    {
        return s == null || (s instanceof String && ((String) s).length() == 0);
    }

    /**
     * Creates a two-element array of default min and max values, typically used to initialize extreme values searches.
     *
     * @return a two-element array of extreme values. Entry 0 is the maximum double value; entry 1 is the negative of
     * the maximum double value;
     */
    public static double[] defaultMinMix()
    {
        return new double[] {Double.MAX_VALUE, -Double.MAX_VALUE};
    }
}
