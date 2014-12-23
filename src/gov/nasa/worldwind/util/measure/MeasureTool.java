/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util.measure;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * A utility class to interactively draw shapes and measure distance and area across the terrain. When armed, the class
 * monitors mouse events to allow the definition of a measure shape that can be one of {@link #SHAPE_LINE}, {@link
 * #SHAPE_PATH}, {@link #SHAPE_POLYGON}, {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link
 * #SHAPE_QUAD}.
 *
 * <p>In order to allow user interaction with the measuring shape, a controller must be set by calling {@link
 * #setController(MeasureToolController)} with a new instance of a <code>MeasureToolController</code>.</p>
 *
 * <p>The interaction sequence for drawing a shape and measuring is as follows: <ul> <li>Set the measure shape.</li>
 * <li>Arm the <code>MeasureTool</code> object by calling its {@link #setArmed(boolean)} method with an argument of
 * true.</li> <li>Click on the terrain to add points.</li> <li>Disarm the <code>MeasureTool</code> object by calling its
 * {@link #setArmed(boolean)} method with an argument of false. </li> <li>Read the measured length or area by calling
 * the <code>MeasureTool</code> {@link #getLength()} or {@link #getArea()} method. Note that the length and area can be
 * queried at any time during or after the process.</li> </ul> </p> <p>While entering points or after the measure tool
 * has been disarmed, dragging the control points allow to change the initial points positions and alter the measure
 * shape.</p>
 *
 * <p> While the <code>MeasureTool</code> is armed, pressing and immediately releasing mouse button one while also
 * pressing the control key (Ctl) removes the last point entered. </p>
 *
 * <p>Arming and disarming the <code>MeasureTool</code> does not change the contents or attributes of the measure tool's
 * layer. Note that the measure tool will NOT disarm itself after the second point of a line or a regular shape has been
 * entered - the MeasureToolController has that responsability.</p>
 *
 * <p><b>Setting the measure shape from the application</b></p>
 *
 * <p>The application can set the measure shape to an arbitrary list of positions using {@link
 * #setPositions(java.util.ArrayList)}. If the provided list contains two positions, the measure shape will be set to
 * {@link #SHAPE_LINE}. If more then two positions are provided, the measure shape will be set to {@link #SHAPE_PATH} if
 * the last position differs from the first (open path), or {@link #SHAPE_POLYGON} if the path is closed.</p>
 *
 * <p>The application can also set the measure shape to a predefined regular shape by calling {@link
 * #setMeasureShapeType(String, Position, double, double, Angle)}, providing a shape type (one of {@link #SHAPE_CIRCLE},
 * {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}), a center position, a wdth, a height (in
 * meters) and a heading angle.</p>
 *
 * <p>Finally, the application can use an existing <code>Polyline</code> or <code>SurfaceShape</code> by using {@link
 * #setMeasureShape(Polyline)} or {@link #setMeasureShape(SurfaceShape)}. The surface shape can be one of
 * <code>SurfacePolygon</code>, <code>SurfaceQuad</code>, <code>SurfaceSquare</code>, <code>SurfaceEllipse</code> or
 * <code>SurfaceCircle</code>.
 *
 * <p><b>Measuring</b></p>
 *
 * <p>The application can read the measured length or area by calling the <code>MeasureTool</code> {@link #getLength()}
 * or {@link #getArea()} method. These methods will return -1 when no value is available.</p>
 *
 * <p>Regular shapes are defined by a center position, a width a height and a heading angle. Those attributes can be
 * accessed by calling the {@link #getCenterPosition()}, {@link #getWidth()}, {@link #getHeight()} and {@link
 * #getOrientation()} methods.</p>
 *
 * <p>The measurements are dislayed in units specified in the measure tool's {@link UnitsFormat} object. Access to the
 * units format is via the method {@link #getUnitsFormat()}.
 *
 * <p><b>Events</b></p>
 *
 * <p>The <code>MeasureTool</code> will send events on several occasions: when the position list has changed - {@link
 * #EVENT_POSITION_ADD}, {@link #EVENT_POSITION_REMOVE} or {@link #EVENT_POSITION_REPLACE}, when metrics has changed
 * {@link #EVENT_METRIC_CHANGED} or when the tool is armed or disarmed {@link #EVENT_ARMED}.</p>
 *
 * <p>Events will also be fired at the start and end of a rubber band operation during shape creation: {@link
 * #EVENT_RUBBERBAND_START} and {@link #EVENT_RUBBERBAND_STOP}.</p>
 *
 * <p>See {@link gov.nasa.worldwind.examples.MeasureToolPanel} for some events usage.</p>
 *
 * <p>Several instances of this class can be used simultaneously. However, each instance should be disposed of after
 * usage by calling the {@link #dispose()} method.</p>
 *
 * @author Patrick Murris
 * @version $Id: MeasureTool.java 12738 2009-10-24 00:02:27Z tgaskins $
 * @see MeasureToolController
 */
public class MeasureTool extends AVListImpl implements Disposable
{

    public static final String SHAPE_LINE = "MeasureTool.ShapeLine";
    public static final String SHAPE_PATH = "MeasureTool.ShapePath";
    public static final String SHAPE_POLYGON = "MeasureTool.ShapePolygon";
    public static final String SHAPE_CIRCLE = "MeasureTool.ShapeCircle";
    public static final String SHAPE_ELLIPSE = "MeasureTool.ShapeEllipse";
    public static final String SHAPE_QUAD = "MeasureTool.ShapeQuad";
    public static final String SHAPE_SQUARE = "MeasureTool.ShapeSquare";

    public static final String EVENT_POSITION_ADD = "MeasureTool.AddPosition";
    public static final String EVENT_POSITION_REMOVE = "MeasureTool.RemovePosition";
    public static final String EVENT_POSITION_REPLACE = "MeasureTool.ReplacePosition";
    public static final String EVENT_METRIC_CHANGED = "MeasureTool.MetricChanged";
    public static final String EVENT_ARMED = "MeasureTool.Armed";
    public static final String EVENT_RUBBERBAND_START = "MeasureTool.RubberBandStart";
    public static final String EVENT_RUBBERBAND_STOP = "MeasureTool.RubberBandStop";

    public static final String ANGLE_LABEL = "MeasureTool.AngleLabel";
    public static final String AREA_LABEL = "MeasureTool.AreaLabel";
    public static final String LENGTH_LABEL = "MeasureTool.LengthLabel";
    public static final String PERIMETER_LABEL = "MeasureTool.PerimeterLabel";
    public static final String RADIUS_LABEL = "MeasureTool.RadiusLabel";
    public static final String HEIGHT_LABEL = "MeasureTool.HeightLabel";
    public static final String WIDTH_LABEL = "MeasureTool.WidthLabel";
    public static final String HEADING_LABEL = "MeasureTool.HeadingLabel";
    public static final String CENTER_LATITUDE_LABEL = "MeasureTool.CenterLatitudeLabel";
    public static final String CENTER_LONGITUDE_LABEL = "MeasureTool.CenterLongitudeLabel";
    public static final String LATITUDE_LABEL = "MeasureTool.LatitudeLabel";
    public static final String LONGITUDE_LABEL = "MeasureTool.LongitudeLabel";
    public static final String ACCUMULATED_LABEL = "MeasureTool.AccumulatedLabel";
    public static final String MAJOR_AXIS_LABEL = "MeasureTool.MajorAxisLabel";
    public static final String MINOR_AXIS_LABEL = "MeasureTool.MinorAxisLabel";

    protected final WorldWindow wwd;
    protected MeasureToolController controller;

    protected ArrayList<Position> positions = new ArrayList<Position>();
    protected ArrayList<Renderable> controlPoints = new ArrayList<Renderable>();
    protected RenderableLayer applicationLayer;
    protected CustomRenderableLayer layer;
    protected CustomRenderableLayer controlPointsLayer;
    protected CustomRenderableLayer shapeLayer;
    protected Polyline line;
    protected SurfaceShape surfaceShape;
    protected ScreenAnnotation annotation;

    protected Color lineColor = Color.YELLOW;
    protected Color fillColor = new Color(.6f, .6f, .4f, .5f);
    protected double lineWidth = 2;
    protected String pathType = AVKey.GREAT_CIRCLE;
    protected AnnotationAttributes controlPointsAttributes;
    protected AnnotationAttributes annotationAttributes;
    protected String measureShapeType = SHAPE_LINE;
    protected boolean followTerrain = false;
    protected boolean showControlPoints = true;
    protected boolean showAnnotation = true;
    protected UnitsFormat unitsFormat = new UnitsFormat();

    // Rectangle enclosed regular shapes attributes
    protected Rectangle2D.Double shapeRectangle = null;
    protected Position shapeCenterPosition = null;
    protected Angle shapeOrientation = null;
    protected int shapeIntervals = 64;

    protected static class CustomRenderableLayer extends RenderableLayer implements PreRenderable, Renderable
    {
        public void render(DrawContext dc)
        {
            if (dc.isPickingMode() && !this.isPickEnabled())
                return;
            if (!this.isEnabled())
                return;

            super.render(dc);
        }
    }

    /**
     * Construct a new measure tool drawing events from the specified <code>WorldWindow</code>.
     *
     * @param wwd the <code>WorldWindow</code> to draw events from.
     */
    public MeasureTool(final WorldWindow wwd)
    {
        this(wwd, null);
    }

    /**
     * Construct a new measure tool drawing events from the specified <code>WorldWindow</code> and using the given
     * <code>RenderableLayer</code>.
     *
     * @param wwd              the <code>WorldWindow</code> to draw events from.
     * @param applicationLayer the <code>RenderableLayer</code> to use. May be null. If specified, the caller is
     *                         responsible for adding the layer to the model and enabling it.
     */
    public MeasureTool(final WorldWindow wwd, RenderableLayer applicationLayer)
    {
        if (wwd == null)
        {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.wwd = wwd;
        this.applicationLayer = applicationLayer; // can be null

        // Set up layers

        this.layer = createCustomRenderableLayer();
        this.shapeLayer = createCustomRenderableLayer();
        this.controlPointsLayer = createCustomRenderableLayer();

        this.shapeLayer.setPickEnabled(false);
        this.layer.setName("Measure Tool");
        this.layer.addRenderable(this.shapeLayer);          // add shape layer to render layer
        this.layer.addRenderable(this.controlPointsLayer);  // add control points layer to render layer
        this.controlPointsLayer.setEnabled(this.showControlPoints);
        if (this.applicationLayer != null)
            this.applicationLayer.addRenderable(this.layer);    // add render layer to the application provided layer
        else
            this.wwd.getModel().getLayers().add(this.layer);    // add render layer to the globe model

        // Init control points rendering attributes
        this.controlPointsAttributes = new AnnotationAttributes();
        // Define an 8x8 square centered on the screen point
        this.controlPointsAttributes.setFrameShape(FrameFactory.SHAPE_RECTANGLE);
        this.controlPointsAttributes.setLeader(FrameFactory.LEADER_NONE);
        this.controlPointsAttributes.setAdjustWidthToText(Annotation.SIZE_FIXED);
        this.controlPointsAttributes.setSize(new Dimension(8, 8));
        this.controlPointsAttributes.setDrawOffset(new Point(0, -4));
        this.controlPointsAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.controlPointsAttributes.setBorderWidth(0);
        this.controlPointsAttributes.setCornerRadius(0);
        this.controlPointsAttributes.setBackgroundColor(Color.BLUE);    // Normal color
        this.controlPointsAttributes.setTextColor(Color.GREEN);         // Highlighted color
        this.controlPointsAttributes.setHighlightScale(1.2);
        this.controlPointsAttributes.setDistanceMaxScale(1);            // No distance scaling
        this.controlPointsAttributes.setDistanceMinScale(1);
        this.controlPointsAttributes.setDistanceMinOpacity(1);

        // Annotation attributes
        this.setInitialLabels();
        this.annotationAttributes = new AnnotationAttributes();
        this.annotationAttributes.setFrameShape(FrameFactory.SHAPE_NONE);
        this.annotationAttributes.setInsets(new Insets(0, 0, 0, 0));
        this.annotationAttributes.setDrawOffset(new Point(0, 10));
        this.annotationAttributes.setTextAlign(AVKey.CENTER);
        this.annotationAttributes.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        this.annotationAttributes.setFont(Font.decode("Arial-Bold-14"));
        this.annotationAttributes.setTextColor(Color.WHITE);
        this.annotationAttributes.setBackgroundColor(Color.BLACK);
        this.annotationAttributes.setSize(new Dimension(220, 0));
        this.annotation = new ScreenAnnotation("", new Point(0, 0), this.annotationAttributes);
        this.annotation.getAttributes().setVisible(false);
        this.annotation.getAttributes().setDrawOffset(null); // use defaults
        this.shapeLayer.addRenderable(this.annotation);
    }

    protected void setInitialLabels()
    {
        this.setLabel(ACCUMULATED_LABEL, Logging.getMessage(ACCUMULATED_LABEL));
        this.setLabel(ANGLE_LABEL, Logging.getMessage(ANGLE_LABEL));
        this.setLabel(AREA_LABEL, Logging.getMessage(AREA_LABEL));
        this.setLabel(CENTER_LATITUDE_LABEL, Logging.getMessage(CENTER_LATITUDE_LABEL));
        this.setLabel(CENTER_LONGITUDE_LABEL, Logging.getMessage(CENTER_LONGITUDE_LABEL));
        this.setLabel(HEADING_LABEL, Logging.getMessage(HEADING_LABEL));
        this.setLabel(HEIGHT_LABEL, Logging.getMessage(HEIGHT_LABEL));
        this.setLabel(LATITUDE_LABEL, Logging.getMessage(LATITUDE_LABEL));
        this.setLabel(LONGITUDE_LABEL, Logging.getMessage(LONGITUDE_LABEL));
        this.setLabel(LENGTH_LABEL, Logging.getMessage(LENGTH_LABEL));
        this.setLabel(MAJOR_AXIS_LABEL, Logging.getMessage(MAJOR_AXIS_LABEL));
        this.setLabel(MINOR_AXIS_LABEL, Logging.getMessage(MINOR_AXIS_LABEL));
        this.setLabel(PERIMETER_LABEL, Logging.getMessage(PERIMETER_LABEL));
        this.setLabel(RADIUS_LABEL, Logging.getMessage(RADIUS_LABEL));
        this.setLabel(WIDTH_LABEL, Logging.getMessage(WIDTH_LABEL));
    }

    public WorldWindow getWwd()
    {
        return this.wwd;
    }

    /**
     * Return the {@link UnitsFormat} instance governing the measurement value display units and format.
     *
     * @return the tool's units format instance.
     */
    public UnitsFormat getUnitsFormat()
    {
        return this.unitsFormat;
    }

    /**
     * Set the measure tool's @{link UnitsFormat} instance that governs measurement value display units and format.
     *
     * @param unitsFormat the units format instance.
     *
     * @throws IllegalArgumentException if the units format instance is null.
     */
    public void setUnitsFormat(UnitsFormat unitsFormat)
    {
        if (unitsFormat == null)
        {
            String msg = Logging.getMessage("nullValue.Format");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.unitsFormat = unitsFormat;
    }

    /** @return Instance of the custom renderable layer to use of our internal layers */
    protected CustomRenderableLayer createCustomRenderableLayer()
    {
        return new CustomRenderableLayer();
    }

    /**
     * Set the controller object for this measure tool - can be null.
     *
     * @param controller the controller object for this measure tool.
     */
    public void setController(MeasureToolController controller)
    {
        if (this.controller != null)
        {
            this.wwd.getInputHandler().removeMouseListener(this.controller);
            this.wwd.getInputHandler().removeMouseMotionListener(this.controller);
            this.wwd.removePositionListener(this.controller);
            this.wwd.removeSelectListener(this.controller);
            this.wwd.removeRenderingListener(this.controller);
            this.controller = null;
        }
        if (controller != null)
        {
            this.controller = controller;
            this.controller.setMeasureTool(this);
            this.wwd.getInputHandler().addMouseListener(this.controller);
            this.wwd.getInputHandler().addMouseMotionListener(this.controller);
            this.wwd.addPositionListener(this.controller);
            this.wwd.addSelectListener(this.controller);
            this.wwd.addRenderingListener(this.controller);
        }
    }

    public void setLabel(String labelName, String label)
    {
        if (labelName != null && labelName.length() > 0)
            this.setValue(labelName, label);
    }

    public String getLabel(String labelName)
    {
        if (labelName == null)
        {
            String msg = Logging.getMessage("nullValue.LabelName");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        String label = this.getStringValue(labelName);

        return label != null ? label : this.unitsFormat.getStringValue(labelName);
    }

    /**
     * Get the <code>MeasureToolController</code> for this measure tool.
     *
     * @return the <code>MeasureToolController</code> for this measure tool.
     */
    public MeasureToolController getController()
    {
        return this.controller;
    }

    /**
     * Arms and disarms the measure tool controller. When armed, the controller monitors user input and builds the shape
     * in response to user actions. When disarmed, the controller ignores all user input.
     *
     * @param state true to arm the controller, false to disarm it.
     */
    public void setArmed(boolean state)
    {
        if (this.controller != null)
            this.controller.setArmed(state);
    }

    /**
     * Identifies whether the measure tool controller is armed.
     *
     * @return true if armed, false if not armed.
     */
    public boolean isArmed()
    {
        return this.controller != null && this.controller.isArmed();
    }

    /**
     * Returns the measure tool layer.
     *
     * @return the layer containing the measure shape and control points.
     */
    public RenderableLayer getLayer()
    {
        return this.layer;
    }

    /**
     * Returns the applilcation layer passed to the constructor.
     *
     * @return the layer containing the measure shape and control points.
     */
    public RenderableLayer getApplicationLayer()
    {
        return applicationLayer;
    }

    /**
     * Returns the polyline currently used to display lines and path.
     *
     * @return the polyline currently used to display lines and path.
     */
    public Polyline getLine()
    {
        return this.line;
    }

    /**
     * Returns the surface shape currently used to display polygons.
     *
     * @return the surface shape currently used to display polygons.
     */
    public SurfaceShape getSurfaceShape()
    {
        return this.surfaceShape;
    }

    /**
     * Get the list of positions that define the current measure shape.
     *
     * @return the list of positions that define the current measure shape.
     */
    public ArrayList<? extends Position> getPositions()
    {
        return this.positions;
    }

    /**
     * Set the measure shape to an arbitrary list of positions. If the provided list contains two positions, the measure
     * shape will be set to {@link #SHAPE_LINE}. If more then two positions are provided, the measure shape will be set
     * to {@link #SHAPE_PATH} if the last position differs from the first (open path), or {@link #SHAPE_POLYGON} if the
     * path is closed.
     *
     * @param newPositions the shape position list.
     */
    public void setPositions(ArrayList<? extends Position> newPositions)
    {
        if (newPositions == null)
        {
            String msg = Logging.getMessage("nullValue.PositionsListIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (newPositions.size() < 2)
            return;

        this.clear();

        // Setup the proper measure shape
        boolean closedShape = newPositions.get(0).equals(newPositions.get(newPositions.size() - 1));
        if (newPositions.size() > 2 && closedShape)
            setMeasureShapeType(SHAPE_POLYGON);
        else
            setMeasureShapeType(getPathType(newPositions));

        // Import positions and create control points
        for (int i = 0; i < newPositions.size(); i++)
        {
            Position pos = newPositions.get(i);
            this.positions.add(pos);
            if (i < newPositions.size() - 1 || !closedShape)
                addControlPoint(pos, "PositionIndex", this.positions.size() - 1);
        }

        // Update line heading if needed
        if (this.measureShapeType.equals(SHAPE_LINE))
            this.shapeOrientation = LatLon.greatCircleAzimuth(this.positions.get(0), this.positions.get(1));

        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    /**
     * Get the list of control points associated with the current measure shape.
     *
     * @return the list of control points associated with the current measure shape.
     */
    public ArrayList<Renderable> getControlPoints()
    {
        return this.controlPoints;
    }

    /**
     * Get the attributes associated with the control points.
     *
     * @return the attributes associated with the control points.
     */
    public AnnotationAttributes getControlPointsAttributes()
    {
        return this.controlPointsAttributes;
    }

    /**
     * Get the attributes associated with the tool tip annotation.
     *
     * @return the attributes associated with the tool tip annotation.
     */
    public AnnotationAttributes getAnnotationAttributes()
    {
        return this.annotationAttributes;
    }

    public void setLineColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.lineColor = color;
        if (this.line != null)
        {
            this.line.setColor(color);
        }
        if (this.surfaceShape != null)
        {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            attr.setOutlineMaterial(new Material(color));
            attr.setOutlineOpacity(color.getAlpha() / 255d);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public Color getLineColor()
    {
        return this.lineColor;
    }

    public void setFillColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.fillColor = color;
        if (this.surfaceShape != null)
        {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            attr.setInteriorMaterial(new Material(color));
            attr.setInteriorOpacity(color.getAlpha() / 255d);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public Color getFillColor()
    {
        return this.fillColor;
    }

    public void setLineWidth(double width)
    {
        this.lineWidth = width;
        if (this.line != null)
            this.line.setLineWidth(width);
        if (this.surfaceShape != null)
        {
            ShapeAttributes attr = this.surfaceShape.getAttributes();
            attr.setOutlineWidth(width);
            this.surfaceShape.setAttributes(attr);
        }
        this.wwd.redraw();
    }

    public double getLineWidth()
    {
        return this.lineWidth;
    }

    public String getPathType()
    {
        return this.pathType;
    }

    public void setPathType(String type)
    {
        this.pathType = type;
        if (this.line != null)
            this.line.setPathType(polylinePathTypeFromKey(type));
        if (this.surfaceShape != null)
            this.surfaceShape.setPathType(type);
        this.wwd.redraw();
    }

    @SuppressWarnings({"StringEquality"})
    protected static int polylinePathTypeFromKey(String type)
    {
        if (type != null && type == AVKey.GREAT_CIRCLE)
        {
            return Polyline.GREAT_CIRCLE;
        }
        else if (type != null && (type == AVKey.RHUMB_LINE || type == AVKey.LOXODROME))
        {
            return Polyline.RHUMB_LINE;
        }
        else
        {
            return Polyline.LINEAR;
        }
    }

    protected static String keyFromPolylinePathType(int type)
    {
        if (type == Polyline.GREAT_CIRCLE)
        {
            return AVKey.GREAT_CIRCLE;
        }
        else if (type == Polyline.RHUMB_LINE)
        {
            return AVKey.RHUMB_LINE;
        }
        else
        {
            return AVKey.LINEAR;
        }
    }

    public boolean isShowControlPoints()
    {
        return this.showControlPoints;
    }

    public void setShowControlPoints(boolean state)
    {
        this.showControlPoints = state;
        this.controlPointsLayer.setEnabled(state);
        this.wwd.redraw();
    }

    public boolean isShowAnnotation()
    {
        return this.showAnnotation;
    }

    public void setShowAnnotation(boolean state)
    {
        this.showAnnotation = state;
    }

    /** Removes all positions from the shape, clear attributes. */
    public void clear()
    {
        while (this.positions.size() > 0 || this.controlPoints.size() > 0)
        {
            this.removeControlPoint();
        }

        this.shapeCenterPosition = null;
        this.shapeOrientation = null;
        this.shapeRectangle = null;
    }

    public boolean isMeasureShape(Object o)
    {
        return o == this.shapeLayer;
    }

    /**
     * Get the measure shape type. can be one of {@link #SHAPE_LINE}, {@link #SHAPE_PATH}, {@link #SHAPE_POLYGON},
     * {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @return the measure shape type.
     */
    public String getMeasureShapeType()
    {
        return this.measureShapeType;
    }

    /**
     * Set the measure shape type. can be one of {@link #SHAPE_LINE}, {@link #SHAPE_PATH}, {@link #SHAPE_POLYGON},
     * {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE}, {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}. This will reset the
     * measure tool and clear the current measure shape.
     *
     * @param shape the measure shape type.
     */
    public void setMeasureShapeType(String shape)
    {
        if (shape == null)
        {
            String msg = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.measureShapeType.equals(shape))
        {
            setArmed(false);
            clear();
            this.measureShapeType = shape;
        }
    }

    /**
     * Set and initialize the measure shape to one of the regular shapes {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE},
     * {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @param shapeType      the shape type.
     * @param centerPosition ther shape center position.
     * @param radius         the shape radius of half width/height.
     */
    public void setMeasureShapeType(String shapeType, Position centerPosition, double radius)
    {
        setMeasureShapeType(shapeType, centerPosition, radius * 2, radius * 2, Angle.ZERO);
    }

    /**
     * Set and initialize the measure shape to one of the regular shapes {@link #SHAPE_CIRCLE}, {@link #SHAPE_ELLIPSE},
     * {@link #SHAPE_SQUARE} or {@link #SHAPE_QUAD}.
     *
     * @param shapeType      the shape type.
     * @param centerPosition ther shape center position.
     * @param width          the shape width.
     * @param height         the shape height.
     * @param orientation    the shape orientation or azimuth angle - clockwise from north.
     */
    public void setMeasureShapeType(String shapeType, Position centerPosition, double width, double height,
        Angle orientation)
    {
        if (shapeType == null)
        {
            String msg = Logging.getMessage("nullValue.ShapeType");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (centerPosition == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (orientation == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (isRegularShape(shapeType))
        {
            setArmed(false);
            clear();
            if ((shapeType.equals(SHAPE_CIRCLE) || shapeType.equals(SHAPE_SQUARE)) && width != height)
            {
                width = Math.max(width, height);
                height = Math.max(width, height);
            }
            // Set regular shape properties
            this.measureShapeType = shapeType;
            this.shapeCenterPosition = centerPosition;
            this.shapeRectangle = new Rectangle2D.Double(0, 0, width, height);
            this.shapeOrientation = orientation;
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Update screen shapes
            updateMeasureShape();
            this.firePropertyChange(EVENT_POSITION_REPLACE, null, null);
            this.wwd.redraw();
        }
    }

    /**
     * Set the measure shape to an existing <code>Polyline</code>.
     *
     * @param line a <code>Polyline</code> instance.
     */
    public void setMeasureShape(Polyline line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        setArmed(false);
        this.clear();

        // Clear and replace current shape
        if (this.surfaceShape != null)
        {
            this.shapeLayer.removeRenderable(this.surfaceShape);
            this.surfaceShape = null;
        }
        if (this.line != null)
            this.shapeLayer.removeRenderable(this.line);
        this.line = line;
        this.shapeLayer.addRenderable(line);
        // Grab some of the line attributes
        setFollowTerrain(line.isFollowTerrain());
        setPathType(keyFromPolylinePathType(line.getPathType()));
        // Update position list and create control points
        int i = 0;
        for (Position pos : line.getPositions())
        {
            this.positions.add(pos);
            addControlPoint(pos, "PositionIndex", i++);
        }
        // Set proper measure shape type
        this.measureShapeType = getPathType(this.positions);
        this.firePropertyChange(EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    protected String getPathType(List<? extends Position> positions)
    {
        return positions.size() > 2 ? SHAPE_PATH : SHAPE_LINE;
    }

    /**
     * Set the measure shape to an existing <code>SurfaceShape</code>. Can be one of <code>SurfacePolygon</code>,
     * <code>SurfaceQuad</code>, <code>SurfaceSquare</code>, <code>SurfaceEllipse</code> or <code>SurfaceCircle</code>.
     *
     * @param surfaceShape a <code>SurfaceShape</code> instance.
     */
    public void setMeasureShape(SurfaceShape surfaceShape)
    {
        if (surfaceShape == null)
        {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        setArmed(false);
        this.clear();

        // Clear and replace current surface shape
        if (this.surfaceShape != null)
        {
            this.shapeLayer.removeRenderable(this.surfaceShape);
        }
        if (this.line != null)
        {
            this.shapeLayer.removeRenderable(this.line);
            this.line = null;
        }
        this.surfaceShape = surfaceShape;
        this.shapeLayer.addRenderable(surfaceShape);
        this.setPathType(surfaceShape.getPathType());

        if (surfaceShape instanceof SurfaceQuad)
        {
            // Set measure shape type
            this.measureShapeType = surfaceShape instanceof SurfaceSquare ? SHAPE_SQUARE : SHAPE_QUAD;
            // Set regular shape properties
            SurfaceQuad shape = ((SurfaceQuad) surfaceShape);
            this.shapeCenterPosition = new Position(shape.getCenter(), 0);
            this.shapeRectangle = new Rectangle2D.Double(0, 0, shape.getWidth(), shape.getHeight());
            this.shapeOrientation = shape.getHeading();
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Extract positions from shape
            updatePositionsFromShape();
        }
        else if (surfaceShape instanceof SurfaceEllipse)
        {
            // Set measure shape type
            this.measureShapeType = surfaceShape instanceof SurfaceCircle ? SHAPE_CIRCLE : SHAPE_ELLIPSE;
            // Set regular shape properties
            SurfaceEllipse shape = ((SurfaceEllipse) surfaceShape);
            this.shapeCenterPosition = new Position(shape.getCenter(), 0);
            this.shapeRectangle = new Rectangle2D.Double(0, 0, shape.getMajorRadius() * 2,
                shape.getMinorRadius() * 2);
            this.shapeOrientation = shape.getHeading();
            // Create control points for regular shapes
            updateShapeControlPoints();
            // Extract positions from shape
            updatePositionsFromShape();
        }
        else // SurfacePolygon, SurfacePolyline, SurfaceSector, or some custom shape
        {
            // Set measure shape type
            this.measureShapeType = SHAPE_POLYGON;
            // Extract positions from shape
            updatePositionsFromShape();
            // Create control points for each position except the last that is the same as the first
            for (int i = 0; i < this.positions.size() - 1; i++)
            {
                addControlPoint(this.positions.get(i), "PositionIndex", i);
            }
        }

        this.firePropertyChange(EVENT_POSITION_REPLACE, null, null);
        this.wwd.redraw();
    }

    public boolean isRegularShape()
    {
        return isRegularShape(this.measureShapeType);
    }

    protected boolean isRegularShape(String shape)
    {
        return (shape.equals(SHAPE_CIRCLE)
            || shape.equals(SHAPE_ELLIPSE)
            || shape.equals(SHAPE_QUAD)
            || shape.equals(SHAPE_SQUARE));
    }

    public boolean isFollowTerrain()
    {
        return this.followTerrain;
    }

    public void setFollowTerrain(boolean followTerrain)
    {
        this.followTerrain = followTerrain;
        if (this.line != null)
        {
            this.line.setFollowTerrain(followTerrain);
        }
    }

    // *** Metric accessors ***

    public double getLength()
    {
        if (this.line != null)
            return this.line.getLength();

        Globe globe = this.wwd.getModel().getGlobe();

        if (this.surfaceShape != null)
            return this.surfaceShape.getPerimeter(globe);

        return -1;
    }

    public double getArea()
    {
        Globe globe = this.wwd.getModel().getGlobe();

        if (this.surfaceShape != null)
            return this.surfaceShape.getArea(globe, this.followTerrain);

        return -1;
    }

    public double getWidth()
    {
        if (this.shapeRectangle != null)
            return this.shapeRectangle.width;
        return -1;
    }

    public double getHeight()
    {
        if (this.shapeRectangle != null)
            return this.shapeRectangle.height;
        return -1;
    }

    public Angle getOrientation()
    {
        return this.shapeOrientation;
    }

    public Position getCenterPosition()
    {
        return this.shapeCenterPosition;
    }

    // *** Editing shapes ***

    /** Add a control point to the current measure shape at the cuurrent WorldWindow position. */
    public void addControlPoint()
    {
        Position curPos = this.wwd.getCurrentPosition();
        if (curPos == null)
            return;

        if (this.isRegularShape())
        {
            // Regular shapes are defined in two steps: 1. center, 2. east point.
            if (this.shapeCenterPosition == null)
            {
                this.shapeCenterPosition = curPos;
                updateShapeControlPoints();
            }
            else if (this.shapeRectangle == null)
            {
                // Compute shape rectangle and heading, curPos being east of center
                updateShapeProperties("East", curPos);
                // Update or create control points
                updateShapeControlPoints();
            }
        }
        else
        {
            if (!this.measureShapeType.equals(SHAPE_POLYGON) || this.positions.size() <= 1)
            {
                // Line, path or polygons with less then two points
                this.positions.add(curPos);
                addControlPoint(this.positions.get(this.positions.size() - 1), "PositionIndex",
                    this.positions.size() - 1);
                if (this.measureShapeType.equals(SHAPE_POLYGON) && this.positions.size() == 2)
                {
                    // Once we have two points of a polygon, add an extra position
                    // to loop back to the first position and have a closed shape
                    this.positions.add(this.positions.get(0));
                }
                if (this.measureShapeType.equals(SHAPE_LINE) && this.positions.size() > 1)
                {
                    // Two points on a line, update line heading info
                    this.shapeOrientation = LatLon.greatCircleAzimuth(this.positions.get(0), this.positions.get(1));
                }
            }
            else
            {
                // For polygons with more then 2 points, the last position is the same as the first, so insert before it
                this.positions.add(positions.size() - 1, curPos);
                addControlPoint(this.positions.get(this.positions.size() - 2), "PositionIndex",
                    this.positions.size() - 2);
            }
        }
        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(EVENT_POSITION_ADD, null, curPos);
        this.wwd.redraw();
    }

    /** Remove the last control point from the current measure shape. */
    public void removeControlPoint()
    {
        Position currentLastPosition = null;
        if (this.isRegularShape())
        {
            if (this.shapeRectangle != null)
            {
                this.shapeRectangle = null;
                this.shapeOrientation = null;
                this.positions.clear();
                // remove all control points except center which is first
                while (this.controlPoints.size() > 1)
                {
                    this.controlPoints.remove(1);
                }
            }
            else if (this.shapeCenterPosition != null)
            {
                this.shapeCenterPosition = null;
                this.controlPoints.clear();
            }
        }
        else
        {
            if (this.positions.size() == 0)
                return;

            if (!this.measureShapeType.equals(SHAPE_POLYGON) || this.positions.size() == 1)
            {
                currentLastPosition = this.positions.get(this.positions.size() - 1);
                this.positions.remove(this.positions.size() - 1);
            }
            else
            {
                // For polygons with more then 2 points, the last position is the same as the first, so remove before it
                currentLastPosition = this.positions.get(this.positions.size() - 2);
                this.positions.remove(this.positions.size() - 2);
                if (positions.size() == 2)
                    positions.remove(1); // remove last loop position when a polygon shrank to only two (same) positions
            }
            if (this.controlPoints.size() > 0)
                this.controlPoints.remove(this.controlPoints.size() - 1);
        }
        this.controlPointsLayer.setRenderables(this.controlPoints);
        // Update screen shapes
        updateMeasureShape();
        this.firePropertyChange(EVENT_POSITION_REMOVE, currentLastPosition, null);
        this.wwd.redraw();
    }

    /**
     * Update the current measure shape according to a given control point position.
     *
     * @param point one of the shape control points.
     */
    public void moveControlPoint(ControlPoint point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (point.getValue("Control") != null)
        {
            // Update shape properties
            updateShapeProperties((String) point.getValue("Control"), point.getPosition());
            updateShapeControlPoints();
            //positions = makeShapePositions();
        }

        if (point.getValue("PositionIndex") != null)
        {
            int positionIndex = (Integer) point.getValue("PositionIndex");
            // Update positions
            Position surfacePosition = computeSurfacePosition(point.getPosition());
            positions.set(positionIndex, surfacePosition);
            // Update last pos too if polygon and first pos changed
            if (measureShapeType.equals(SHAPE_POLYGON) && positions.size() > 2 && positionIndex == 0)
                positions.set(positions.size() - 1, surfacePosition);
            // Update heading for simple line
            if (measureShapeType.equals(SHAPE_LINE) && positions.size() > 1)
                shapeOrientation = LatLon.greatCircleAzimuth(positions.get(0), positions.get(1));
        }

        // Update rendered shapes
        updateMeasureShape();
    }

    /**
     * Move the current measure shape along a great circle arc at a given azimuth <code>Angle</code> for a given
     * distance <code>Angle</code>.
     *
     * @param azimuth  the azimuth <code>Angle</code>.
     * @param distance the distance <code>Angle</code>.
     */
    public void moveMeasureShape(Angle azimuth, Angle distance)
    {
        if (distance == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (azimuth == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.isRegularShape())
        {
            // Move regular shape center
            if (controlPoints.size() > 0)
            {
                ControlPoint point = (ControlPoint) controlPoints.get(0);
                point.setPosition(
                    new Position(LatLon.greatCircleEndPosition(point.getPosition(), azimuth, distance), 0));
                moveControlPoint(point);
            }
        }
        else
        {
            // Move all positions and control points
            for (int i = 0; i < positions.size(); i++)
            {
                Position newPos = computeSurfacePosition(
                    LatLon.greatCircleEndPosition(positions.get(i), azimuth, distance));
                positions.set(i, newPos);
                if (!this.measureShapeType.equals(SHAPE_POLYGON) || i < positions.size() - 1)
                    ((ControlPoint) controlPoints.get(i)).setPosition(new Position(newPos, 0));
            }
            // Update heading for simple line
            if (measureShapeType.equals(SHAPE_LINE) && positions.size() > 1)
                shapeOrientation = LatLon.greatCircleAzimuth(positions.get(0), positions.get(1));
            // Update rendered shapes
            updateMeasureShape();
        }
    }

    protected Position computeSurfacePosition(LatLon latLon)
    {
        Vec4 surfacePoint = wwd.getSceneController().getTerrain().getSurfacePoint(latLon.getLatitude(),
            latLon.getLongitude());
        if (surfacePoint != null)
            return wwd.getModel().getGlobe().computePositionFromPoint(surfacePoint);
        else
            return new Position(latLon, wwd.getModel().getGlobe().getElevation(latLon.getLatitude(),
                latLon.getLongitude()));
    }

    protected void updateShapeProperties(String control, Position newPosition)
    {
        if (control.equals("Center"))
        {
            // Update center position
            this.shapeCenterPosition = newPosition;
        }
        else
        {
            // Compute shape rectangle and heading
            double headingOffset = control.equals("East") ? 90
                : control.equals("South") ? 180
                    : control.equals("West") ? 270
                        : 0;
            this.shapeOrientation = LatLon.greatCircleAzimuth(this.shapeCenterPosition, newPosition)
                .subtractDegrees(headingOffset);
            // Compute distance - have a minimal distance to avoid zero sized shape
            Angle distanceAngle = LatLon.greatCircleDistance(this.shapeCenterPosition, newPosition);
            double distance = Math.max(distanceAngle.radians * wwd.getModel().getGlobe().getRadius(), .1);
            double width, height;
            if (control.equals("East") || control.equals("West"))
            {
                width = distance * 2;
                height = this.shapeRectangle != null ? this.shapeRectangle.height : width;
                if (this.measureShapeType.equals(SHAPE_CIRCLE) || this.measureShapeType.equals(SHAPE_SQUARE))
                    //noinspection SuspiciousNameCombination
                    height = width;
                else if (this.controller != null && this.controller.isActive())
                    height = width * .6;   // during shape creation
            }
            else
            {
                height = distance * 2;
                width = this.shapeRectangle != null ? this.shapeRectangle.width : height;
                if (this.measureShapeType.equals(SHAPE_CIRCLE) || this.measureShapeType.equals(SHAPE_SQUARE))
                    //noinspection SuspiciousNameCombination
                    width = height;
                else if (this.controller != null && this.controller.isActive())
                    width = height * 0.6;   // during shape creation
            }
            this.shapeRectangle = new Rectangle2D.Double(0, 0, width, height);
        }
    }

    protected void updateShapeControlPoints()
    {
        if (this.shapeCenterPosition != null && this.controlPoints.size() < 1)
        {
            // Create center control point
            addControlPoint(Position.ZERO, "Control", "Center");
        }

        if (this.shapeCenterPosition != null)
        {
            // Update center control point position
            ((ControlPoint) this.controlPoints.get(0)).setPosition(new Position(this.shapeCenterPosition, 0));
        }

        if (this.shapeRectangle != null && this.controlPoints.size() < 5)
        {
            // Add control points in four directions
            addControlPoint(Position.ZERO, "Control", "North");
            addControlPoint(Position.ZERO, "Control", "East");
            addControlPoint(Position.ZERO, "Control", "South");
            addControlPoint(Position.ZERO, "Control", "West");
        }

        if (this.shapeRectangle != null)
        {
            Angle halfWidthAngle = Angle.fromRadians(this.shapeRectangle.width / 2
                / wwd.getModel().getGlobe().getRadius());
            Angle halfHeightAngle = Angle.fromRadians(this.shapeRectangle.height / 2
                / wwd.getModel().getGlobe().getRadius());
            // Update control points positions in four directions
            Position controlPos;
            // North
            controlPos = new Position(LatLon.greatCircleEndPosition(
                this.shapeCenterPosition, this.shapeOrientation, halfHeightAngle), 0);
            ((ControlPoint) controlPoints.get(1)).setPosition(controlPos);
            // East
            controlPos = new Position(LatLon.greatCircleEndPosition(
                this.shapeCenterPosition, this.shapeOrientation.addDegrees(90), halfWidthAngle), 0);
            ((ControlPoint) controlPoints.get(2)).setPosition(controlPos);
            // South
            controlPos = new Position(LatLon.greatCircleEndPosition(
                this.shapeCenterPosition, this.shapeOrientation.addDegrees(180), halfHeightAngle), 0);
            ((ControlPoint) controlPoints.get(3)).setPosition(controlPos);
            // West
            controlPos = new Position(LatLon.greatCircleEndPosition(
                this.shapeCenterPosition, this.shapeOrientation.addDegrees(270), halfWidthAngle), 0);
            ((ControlPoint) controlPoints.get(4)).setPosition(controlPos);
        }
    }

    protected void updateMeasureShape()
    {
        // Update line
        if (this.measureShapeType.equals(SHAPE_LINE) || this.measureShapeType.equals(SHAPE_PATH))
        {
            if (this.positions.size() > 1 && this.line == null)
            {
                // Init polyline
                this.line = new Polyline();
                this.line.setFollowTerrain(this.isFollowTerrain());
                this.line.setLineWidth(this.getLineWidth());
                this.line.setColor(this.getLineColor());
                this.line.setPathType(polylinePathTypeFromKey(this.getPathType()));
                //this.line.setNumSubsegments(this.followTerrain ? 10 : 1);
                this.shapeLayer.addRenderable(this.line);
            }
            if (this.positions.size() < 2 && this.line != null)
            {
                // Remove line if less then 2 positions
                this.shapeLayer.removeRenderable(this.line);
                this.line = null;
            }
            // Update current line
            if (this.positions.size() > 1 && this.line != null)
                this.line.setPositions(this.positions);

            if (this.surfaceShape != null)
            {
                // Remove surface shape if necessary
                this.shapeLayer.removeRenderable(this.surfaceShape);
                this.surfaceShape = null;
            }
        }
        // Update polygon
        else if (this.measureShapeType.equals(SHAPE_POLYGON))
        {
            if (this.positions.size() >= 4 && this.surfaceShape == null)
            {
                // Init surface shape
                this.surfaceShape = new SurfacePolygon(this.positions);
                ShapeAttributes attr = this.surfaceShape.getAttributes();
                attr.setInteriorMaterial(new Material(this.getFillColor()));
                attr.setInteriorOpacity(this.getFillColor().getAlpha() / 255d);
                attr.setOutlineMaterial(new Material(this.getLineColor()));
                attr.setOutlineOpacity(this.getLineColor().getAlpha() / 255d);
                attr.setOutlineWidth(this.getLineWidth());
                this.surfaceShape.setAttributes(attr);
                this.shapeLayer.addRenderable(this.surfaceShape);
            }
            if (this.positions.size() <= 3 && this.surfaceShape != null)
            {
                // Remove surface shape if only three positions or less - last is same as first
                this.shapeLayer.removeRenderable(this.surfaceShape);
                this.surfaceShape = null;
            }
            if (this.surfaceShape != null)
            {
                // Update current shape
                ((SurfacePolygon) this.surfaceShape).setLocations(this.positions);
            }
            // Remove line if necessary
            if (this.line != null)
            {
                this.shapeLayer.removeRenderable(this.line);
                this.line = null;
            }
        }
        // Update regular shape
        else if (this.isRegularShape())
        {
            if (this.shapeCenterPosition != null && this.shapeRectangle != null && this.surfaceShape == null)
            {
                // Init surface shape
                if (this.measureShapeType.equals(SHAPE_QUAD))
                    this.surfaceShape = new SurfaceQuad(this.shapeCenterPosition,
                        this.shapeRectangle.width, this.shapeRectangle.height, this.shapeOrientation);
                else if (this.measureShapeType.equals(SHAPE_SQUARE))
                    this.surfaceShape = new SurfaceSquare(this.shapeCenterPosition,
                        this.shapeRectangle.width);
                else if (this.measureShapeType.equals(SHAPE_ELLIPSE))
                    this.surfaceShape = new SurfaceEllipse(this.shapeCenterPosition,
                        this.shapeRectangle.width / 2, this.shapeRectangle.height / 2, this.shapeOrientation,
                        this.shapeIntervals);
                else if (this.measureShapeType.equals(SHAPE_CIRCLE))
                    this.surfaceShape = new SurfaceCircle(this.shapeCenterPosition,
                        this.shapeRectangle.width / 2, this.shapeIntervals);

                ShapeAttributes attr = this.surfaceShape.getAttributes();
                attr.setInteriorMaterial(new Material(this.getFillColor()));
                attr.setInteriorOpacity(this.getFillColor().getAlpha() / 255d);
                attr.setOutlineMaterial(new Material(this.getLineColor()));
                attr.setOutlineOpacity(this.getLineColor().getAlpha() / 255d);
                attr.setOutlineWidth(this.getLineWidth());
                this.surfaceShape.setAttributes(attr);
                this.shapeLayer.addRenderable(this.surfaceShape);
            }
            if (this.shapeRectangle == null && this.surfaceShape != null)
            {
                // Remove surface shape if not defined
                this.shapeLayer.removeRenderable(this.surfaceShape);
                this.surfaceShape = null;
                this.positions.clear();
            }
            if (this.surfaceShape != null)
            {
                // Update current shape
                if (this.measureShapeType.equals(SHAPE_QUAD) || this.measureShapeType.equals(SHAPE_SQUARE))
                {
                    ((SurfaceQuad) this.surfaceShape).setCenter(this.shapeCenterPosition);
                    ((SurfaceQuad) this.surfaceShape).setSize(this.shapeRectangle.width, this.shapeRectangle.height);
                    ((SurfaceQuad) this.surfaceShape).setHeading(this.shapeOrientation);
                }
                if (this.measureShapeType.equals(SHAPE_ELLIPSE) || this.measureShapeType.equals(SHAPE_CIRCLE))
                {
                    ((SurfaceEllipse) this.surfaceShape).setCenter(this.shapeCenterPosition);
                    ((SurfaceEllipse) this.surfaceShape).setRadii(this.shapeRectangle.width / 2,
                        this.shapeRectangle.height / 2);
                    ((SurfaceEllipse) this.surfaceShape).setHeading(this.shapeOrientation);
                }
                // Update position from shape list with zero elevation
                updatePositionsFromShape();
            }
            // Remove line if necessary
            if (this.line != null)
            {
                this.shapeLayer.removeRenderable(this.line);
                this.line = null;
            }
        }
    }

    protected void updatePositionsFromShape()
    {
        Globe globe = this.wwd.getModel().getGlobe();

        this.positions.clear();
        for (LatLon latLon : this.surfaceShape.getLocations(globe))
        {
            this.positions.add(new Position(latLon, 0));
        }
    }

    public void dispose()
    {
        this.setController(null);
        if (this.applicationLayer != null)
            this.applicationLayer.removeRenderable(this.layer);
        else
            this.wwd.getModel().getLayers().remove(this.layer);
        this.layer.removeAllRenderables();
        this.shapeLayer.removeAllRenderables();
        this.controlPoints.clear();
//        this.controlPointsLayer.removeAllRenderables(); // TODO: why commented out? Are annotations being disposed?
    }

    // *** Control points ***

    public static class ControlPoint extends GlobeAnnotation
    {
        MeasureTool parent;

        public ControlPoint(Position position, AnnotationAttributes attributes, MeasureTool parent)
        {
            super("", position, attributes);
            this.parent = parent;
        }

        public MeasureTool getParent()
        {
            return this.parent;
        }
    }

    protected void addControlPoint(Position position, String key, Object value)
    {
        ControlPoint controlPoint = new ControlPoint(new Position(position, 0), this.controlPointsAttributes, this);
        controlPoint.setValue(key, value);
        this.controlPoints.add(controlPoint);
        this.controlPointsLayer.setRenderables(this.controlPoints);
    }

    public void updateAnnotation(Position pos)
    {
        if (pos == null)
        {
            this.annotation.getAttributes().setVisible(false);
            return;
        }

        String displayString = this.getDisplayString(pos);

        if (displayString == null)
        {
            this.annotation.getAttributes().setVisible(false);
            return;
        }

        this.annotation.setText(displayString);

        Vec4 screenPoint = this.computeAnnotationPosition(pos);
        if (screenPoint != null)
            this.annotation.setScreenPoint(new Point((int) screenPoint.x, (int) screenPoint.y));

        this.annotation.getAttributes().setVisible(true);
    }

    protected String getDisplayString(Position pos)
    {
        String displayString = null;

        if (pos != null)
        {
            if (this.measureShapeType.equals(SHAPE_CIRCLE) && this.shapeRectangle != null)
            {
                displayString = this.formatCircleMeasurements(pos);
            }
            else if (this.measureShapeType.equals(SHAPE_SQUARE) && this.shapeRectangle != null)
            {
                displayString = this.formatSquareMeasurements(pos);
            }
            else if (this.measureShapeType.equals(SHAPE_QUAD) && this.shapeRectangle != null)
            {
                displayString = this.formatQuadMeasurements(pos);
            }
            else if (this.measureShapeType.equals(SHAPE_ELLIPSE) && this.shapeRectangle != null)
            {
                displayString = this.formatEllipseMeasurements(pos);
            }
            else if (this.measureShapeType.equals(SHAPE_LINE) || this.measureShapeType.equals(SHAPE_PATH))
            {
                displayString = this.formatLineMeasurements(pos);
            }
            else if (this.measureShapeType.equals(SHAPE_POLYGON))
            {
                displayString = this.formatPolygonMeasurements(pos);
            }
        }

        return displayString;
    }

    protected Vec4 computeAnnotationPosition(Position pos)
    {
        Vec4 surfacePoint = this.wwd.getSceneController().getTerrain().getSurfacePoint(
            pos.getLatitude(), pos.getLongitude());
        if (surfacePoint == null)
        {
            Globe globe = this.wwd.getModel().getGlobe();
            surfacePoint = globe.computePointFromPosition(pos.getLatitude(), pos.getLongitude(),
                globe.getElevation(pos.getLatitude(), pos.getLongitude()));
        }

        return this.wwd.getView().project(surfacePoint);
    }

    protected String formatCircleMeasurements(Position pos)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null)
            sb.append(this.unitsFormat.lengthNL(this.getLabel(RADIUS_LABEL), this.shapeRectangle.width / 2d));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos))
        {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition()))
        {
            sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatEllipseMeasurements(Position pos)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null)
        {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MAJOR_AXIS_LABEL), this.shapeRectangle.width));
            sb.append(this.unitsFormat.lengthNL(this.getLabel(MINOR_AXIS_LABEL), this.shapeRectangle.height));
        }

        if (this.getOrientation() != null)
            sb.append(this.unitsFormat.angleNL(this.getLabel(HEADING_LABEL), this.getOrientation()));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos))
        {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition()))
        {
            sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatSquareMeasurements(Position pos)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null)
            sb.append(this.unitsFormat.lengthNL(this.getLabel(WIDTH_LABEL), this.shapeRectangle.width));

        if (this.getOrientation() != null)
            sb.append(this.unitsFormat.angleNL(this.getLabel(HEADING_LABEL), this.getOrientation()));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos))
        {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition()))
        {
            sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatQuadMeasurements(Position pos)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(PERIMETER_LABEL), this.getLength()));

        if (this.shapeRectangle != null)
        {
            sb.append(this.unitsFormat.lengthNL(this.getLabel(WIDTH_LABEL), this.shapeRectangle.width));
            sb.append(this.unitsFormat.lengthNL(this.getLabel(HEIGHT_LABEL), this.shapeRectangle.height));
        }

        if (this.getOrientation() != null)
            sb.append(this.unitsFormat.angleNL(this.getLabel(HEADING_LABEL), this.getOrientation()));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos))
        {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition()))
        {
            sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatPolygonMeasurements(Position pos)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.areaNL(this.getLabel(AREA_LABEL), this.getArea()));
        sb.append(this.unitsFormat.lengthNL(this.getLabel(PERIMETER_LABEL), this.getLength()));

        if (this.getCenterPosition() != null && areLocationsRedundant(this.getCenterPosition(), pos))
        {
            sb.append(
                this.unitsFormat.angleNL(this.getLabel(CENTER_LATITUDE_LABEL), this.getCenterPosition().getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(CENTER_LONGITUDE_LABEL),
                this.getCenterPosition().getLongitude()));
        }

        if (!this.areLocationsRedundant(pos, this.getCenterPosition()))
        {
            sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
            sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));
        }

        return sb.toString();
    }

    protected String formatLineMeasurements(Position pos)
    {
        // TODO: Compute the heading of individual path segments
        StringBuilder sb = new StringBuilder();

        sb.append(this.unitsFormat.lengthNL(this.getLabel(LENGTH_LABEL), this.getLength()));

        Double accumLength = this.computeAccumulatedLength(pos);
        if (accumLength != null && accumLength >= 1 && !lengthsEssentiallyEqual(this.getLength(), accumLength))
            sb.append(this.unitsFormat.lengthNL(this.getLabel(ACCUMULATED_LABEL), accumLength));

        if (this.getOrientation() != null)
            sb.append(this.unitsFormat.angleNL(this.getLabel(HEADING_LABEL), this.getOrientation()));

        sb.append(this.unitsFormat.angleNL(this.getLabel(LATITUDE_LABEL), pos.getLatitude()));
        sb.append(this.unitsFormat.angleNL(this.getLabel(LONGITUDE_LABEL), pos.getLongitude()));

        return sb.toString();
    }

    protected Double computeAccumulatedLength(LatLon pos)
    {
        if (this.positions.size() <= 2)
            return null;

        double radius = this.wwd.getModel().getGlobe().getRadius();
        double distanceFromStart = 0;
        int segmentIndex = 0;
        LatLon pos1 = this.positions.get(segmentIndex);
        for (int i = 1; i < this.positions.size(); i++)
        {
            LatLon pos2 = this.positions.get(i);
            double segmentLength = LatLon.greatCircleDistance(pos1, pos2).radians * radius;

            // Check whether the position is inside the segment
            double length1 = LatLon.greatCircleDistance(pos1, pos).radians * radius;
            double length2 = LatLon.greatCircleDistance(pos2, pos).radians * radius;
            if (length1 <= segmentLength && length2 <= segmentLength)
            {
                // Compute portion of segment length
                distanceFromStart += length1 / (length1 + length2) * segmentLength;
                break;
            }
            else
                distanceFromStart += segmentLength;
            pos1 = pos2;
        }

        double gcPathLength = this.computePathLength();

        return distanceFromStart < gcPathLength ? this.getLength() * (distanceFromStart / gcPathLength) : null;
    }

    protected double computePathLength()
    {
        double pathLengthRadians = 0;

        LatLon pos1 = null;
        for (LatLon pos2 : this.positions)
        {
            if (pos1 != null)
                pathLengthRadians += LatLon.greatCircleDistance(pos1, pos2).radians;
            pos1 = pos2;
        }

        return pathLengthRadians * this.wwd.getModel().getGlobe().getRadius();
    }

    protected Angle computeAngleBetween(LatLon a, LatLon b, LatLon c)
    {
        Vec4 v0 = new Vec4(
            b.getLatitude().radians - a.getLatitude().radians,
            b.getLongitude().radians - a.getLongitude().radians, 0);

        Vec4 v1 = new Vec4(
            c.getLatitude().radians - b.getLatitude().radians,
            c.getLongitude().radians - b.getLongitude().radians, 0);

        return v0.angleBetween3(v1);
    }

    protected boolean lengthsEssentiallyEqual(Double l1, Double l2)
    {
        return Math.abs(l1 - l2) / l1 < 0.001; // equal to within a milimeter
    }

    protected boolean areLocationsRedundant(LatLon locA, LatLon locB)
    {
        if (locA == null || locB == null)
            return false;

        String aLat = this.unitsFormat.angleNL("", locA.getLatitude());
        String bLat = this.unitsFormat.angleNL("", locB.getLatitude());

        if (!aLat.equals(bLat))
            return false;

        String aLon = this.unitsFormat.angleNL("", locA.getLongitude());
        String bLon = this.unitsFormat.angleNL("", locB.getLongitude());

        return aLon.equals(bLon);
    }
}
