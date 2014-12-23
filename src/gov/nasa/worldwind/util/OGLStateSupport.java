/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Vec4;

import javax.media.opengl.GL;
import java.awt.*;

/**
 * @author dcollins
 * @version $Id: OGLStateSupport.java 12805 2009-11-18 08:47:49Z dcollins $
 */
public class OGLStateSupport
{
    public static String COLOR_NO_PREMULTIPLIED_ALPHA = "OGLStateSupport.ColorNoPremultipliedAlpha";
    public static String COLOR_PREMULTIPLIED_ALPHA = "OGLStateSupport.ColorPremultipliedAlpha";
    public static String LIGHT_DIRECTIONAL_FROM_VIEWER_POSITION = "OGLStateSupport.LightDirectionalFromViewerPosition";

    protected boolean enableAlphaTest;
    protected boolean enableBlending;
    protected boolean enableColor;
    protected boolean enableLighting;
    protected String colorMode;
    protected java.awt.Color color = java.awt.Color.WHITE;
    protected double opacity = 1d;
    protected String lightType;
    protected Vec4 lightPosition;

    public OGLStateSupport()
    {
    }

    public boolean isEnableAlphaTest()
    {
        return this.enableAlphaTest;
    }

    public void setEnableAlphaTest(boolean enable)
    {
        this.enableAlphaTest = enable;
    }

    public boolean isEnableBlending()
    {
        return this.enableBlending;
    }

    public void setEnableBlending(boolean enable)
    {
        this.enableBlending = enable;
    }

    public boolean isEnableColor()
    {
        return this.enableColor;
    }

    public void setEnableColor(boolean enable)
    {
        this.enableColor = enable;
    }

    public boolean isEnableLighting()
    {
        return this.enableLighting;
    }

    public void setEnableLighting(boolean enable)
    {
        this.enableLighting = enable;
    }

    public String getColorMode()
    {
        return this.colorMode;
    }

    public void setColorMode(String type)
    {
        this.colorMode = type;
    }

    public Color getColor()
    {
        return this.color;
    }

    public double getOpacity()
    {
        return this.opacity;
    }

    public void setColor(java.awt.Color color)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        float[] compArray = new float[4];
        this.color.getRGBComponents(compArray);

        this.color = color;
        this.opacity = (double) compArray[3];
    }

    public void setColor(java.awt.Color color, double opacity)
    {
        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (opacity < 0 || opacity > 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "opacity < 0 or opacity > 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.color = color;
        this.opacity = opacity;
    }

    public String getLightType()
    {
        return this.lightType;
    }

    public void setLightType(String type)
    {
        this.lightType = type;
    }

    public Vec4 getLightPosition()
    {
        return this.lightPosition;
    }

    public void setLightPosition(Vec4 lightPosition)
    {
        this.lightPosition = lightPosition;
    }

    public int getAttributeBits()
    {
        int attribBits = 0;

        if (this.enableColor)
        {
            // For current color.
            attribBits |= GL.GL_CURRENT_BIT;
        }

        if (this.enableBlending)
        {
            // For enable alpha test, enable blending, alpha func, blend func.
            attribBits |= GL.GL_COLOR_BUFFER_BIT;
        }

        if (this.enableLighting)
        {
            // For disable color material, enable lighting, enable light, light properties.
            attribBits |= GL.GL_LIGHTING_BIT;
            // For enable normalize.
            attribBits |= GL.GL_TRANSFORM_BIT;
        }

        return attribBits;
    }

    public void apply(GL gl)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.enableAlphaTest)
        {
            this.setupAlphaTestState(gl);
        }

        if (this.enableBlending)
        {
            this.setupBlendingState(gl, this.colorMode);
        }

        if (this.enableColor)
        {
            this.setupColorState(gl, this.colorMode, this.color, this.opacity);
        }

        if (this.enableLighting)
        {
            this.setupLightingState(gl, this.lightType, this.lightPosition);
        }
    }

    protected void setupAlphaTestState(GL gl)
    {
        gl.glEnable(GL.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL.GL_GREATER, 0.0f);
    }

    protected void setupBlendingState(GL gl, String colorMode)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        gl.glEnable(GL.GL_BLEND);
        OGLUtil.applyBlending(gl, colorMode != null && colorMode.equals(COLOR_PREMULTIPLIED_ALPHA));
    }

    protected void setupColorState(GL gl, String colorMode, java.awt.Color color, double opacity)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (color == null)
        {
            String message = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        OGLUtil.applyColor(gl, color, opacity, colorMode != null && colorMode.equals(COLOR_PREMULTIPLIED_ALPHA));
    }

    protected void setupLightingState(GL gl, String lightType, Vec4 lightPosition)
    {
        if (gl == null)
        {
            String message = Logging.getMessage("nullValue.GLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        gl.glDisable(GL.GL_COLOR_MATERIAL);
        gl.glDisable(GL.GL_LIGHT0);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT1);
        gl.glEnable(GL.GL_NORMALIZE);

        gl.glLightModeli(GL.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, GL.GL_TRUE);
        gl.glShadeModel(GL.GL_SMOOTH);

        if (lightType != null && lightType.equals(LIGHT_DIRECTIONAL_FROM_VIEWER_POSITION))
        {
            OGLUtil.applyLightingDirectionalFromViewer(gl, GL.GL_LIGHT1, lightPosition);
        }
    }
}
