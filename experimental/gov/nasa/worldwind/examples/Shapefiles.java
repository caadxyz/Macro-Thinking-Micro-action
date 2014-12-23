/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.formats.shapefile.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;

/**
 * @author Patrick Murris
 * @version $Id: Shapefiles.java 12830 2009-11-26 15:39:49Z patrickmurris $
 */
public class Shapefiles extends ApplicationTemplate
{
    private static final Color[] colors = new Color[] {
        Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.ORANGE, Color.MAGENTA};

    private static class TextAndShapesLayer extends SurfaceShapeLayer
    {
        private ArrayList<GeographicText> labels = new ArrayList<GeographicText>();
        private GeographicTextRenderer textRenderer = new GeographicTextRenderer();

        public TextAndShapesLayer()
        {
            this.textRenderer.setCullTextEnabled(true);
            this.textRenderer.setCullTextMargin(2);
            this.textRenderer.setDistanceMaxScale(2);
            this.textRenderer.setDistanceMinScale(.5);
            this.textRenderer.setDistanceMinOpacity(.5);
            this.textRenderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        }

        public void addLabel(GeographicText label)
        {
            this.labels.add(label);
        }

        public void doRender(DrawContext dc)
        {
            super.doRender(dc);
            this.setActiveLabels(dc);
            this.textRenderer.render(dc, this.labels);
        }

        protected void setActiveLabels(DrawContext dc)
        {
            for (GeographicText label : this.labels)
                if (label instanceof Label)
                    if (((Label)label).isActive(dc))
                        label.setVisible(true);
                    else
                        label.setVisible(false);
        }
    }

    private static class Label extends UserFacingText
    {
        private double minActiveAltitude = -Double.MAX_VALUE;
        private double maxActiveAltitude = Double.MAX_VALUE;

        public Label(String text, Position position)
        {
            super(text, position);
        }

        public void setMinActiveAltitude(double altitude)
        {
            this.minActiveAltitude = altitude;
        }

        public void setMaxActiveAltitude(double altitude)
        {
            this.maxActiveAltitude = altitude;
        }

        public boolean isActive(DrawContext dc)
        {
            double eyeElevation = dc.getView().getEyePosition().getElevation();
            return this.minActiveAltitude <= eyeElevation && eyeElevation <= this.maxActiveAltitude;
        }
    }

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        JFileChooser fc = new JFileChooser(Configuration.getUserHomeDirectory());

        protected List<Layer> layers = new ArrayList<Layer>();
        protected SurfaceShape lastHighlitShape;
        protected ShapeAttributes lastShapeAttributes;
        protected BasicDragger dragger;
        private JCheckBox pickCheck, dragCheck;

        public AppFrame()
        {
            this.dragger = new BasicDragger(getWwd());

            // Add our control panel
            this.getLayerPanel().add(makeControlPanel(), BorderLayout.SOUTH);

            // Setup file chooser
            this.fc = new JFileChooser(Configuration.getUserHomeDirectory());
            this.fc.addChoosableFileFilter(new SHPFileFilter());

            // Add select listener for shapes dragging
            this.setupSelectListener();
        }

        protected JPanel makeControlPanel()
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4), new TitledBorder("Shapefiles")));

            // Open shapefile button
            JPanel buttonPanel = new JPanel(new GridLayout(1, 1, 0, 0)); // nrows, ncols, hgap, vgap
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            JButton button = new JButton("Open Shapefile");
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    showOpenDialog();
                }
            });
            buttonPanel.add(button);
            panel.add(buttonPanel);

            // Picking and dragging checkboxes
            JPanel pickPanel = new JPanel(new GridLayout(1, 1, 10, 10)); // nrows, ncols, hgap, vgap
            pickPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // top, left, bottom, right
            this.pickCheck = new JCheckBox("Allow picking");
            this.pickCheck.setSelected(true);
            this.pickCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                    enablePicking(((JCheckBox)actionEvent.getSource()).isSelected());
                }
            });
            pickPanel.add(this.pickCheck);

            this.dragCheck = new JCheckBox("Allow dragging");
            this.dragCheck.setSelected(false);
            this.dragCheck.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent actionEvent)
                {
                }
            });
            pickPanel.add(this.dragCheck);

            panel.add(pickPanel);

            return panel;
        }

        protected void setupSelectListener()
        {
            this.getWwd().addSelectListener(new SelectListener()
            {
                public void selected(SelectEvent event)
                {
                    if (lastHighlitShape != null
                        && (event.getTopObject() == null || !event.getTopObject().equals(lastHighlitShape)))
                    {
                        lastHighlitShape.setAttributes(lastShapeAttributes);
                        lastHighlitShape = null;
                    }

                    // Have rollover events highlight the rolled-over object.
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !dragger.isDragging())
                    {
                        AppFrame.this.highlight(event.getTopObject());
                    }

                    // Have drag events drag the selected object.
                    else if (dragCheck.isSelected() && (event.getEventAction().equals(SelectEvent.DRAG_END)
                        || event.getEventAction().equals(SelectEvent.DRAG)))
                    {
                        // Delegate dragging computations to a dragger.
                        dragger.selected(event);

                        // We missed any roll-over events while dragging, so highlight any under the cursor now,
                        // or de-highlight the dragged shape if it's no longer under the cursor.
                        if (event.getEventAction().equals(SelectEvent.DRAG_END))
                        {
                            PickedObjectList pol = getWwd().getObjectsAtCurrentPosition();
                            if (pol != null)
                            {
                                AppFrame.this.highlight(pol.getTopObject());
                                AppFrame.this.getWwd().repaint();
                            }
                        }
                    }
                }
            });
        }

        protected void highlight(Object o)
        {
            // Same shape selected.
            if (o == this.lastHighlitShape)
                return;

            if (this.lastHighlitShape == null && o instanceof AbstractSurfaceShape)
            {
                this.lastHighlitShape = (AbstractSurfaceShape) o;
                this.lastShapeAttributes = this.lastHighlitShape.getAttributes();
                ShapeAttributes selectedAttributes = this.getHighlightAttributes(this.lastShapeAttributes);
                this.lastHighlitShape.setAttributes(selectedAttributes);
            }
        }

        protected ShapeAttributes getHighlightAttributes(ShapeAttributes attributes)
        {
            ShapeAttributes selectedAttributes = new BasicShapeAttributes(attributes);

            if (selectedAttributes.isDrawInterior())
            {
                selectedAttributes.setInteriorMaterial(Material.WHITE);
                selectedAttributes.setInteriorImageSource(null);
            }
            else if (selectedAttributes.isDrawOutline())
            {
                selectedAttributes.setOutlineMaterial(Material.WHITE);
            }

            return selectedAttributes;
        }

        protected void enablePicking(boolean enabled)
        {
            this.dragCheck.setEnabled(enabled);
            for (Layer layer : this.layers)
                layer.setPickEnabled(enabled);
        }

        public void showOpenDialog()
        {
            int retVal = this.fc.showOpenDialog(this);
            if (retVal != JFileChooser.APPROVE_OPTION)
                return;

            File file = this.fc.getSelectedFile();
            this.addShapefileLayer(file);
        }

        public void addShapefileLayer(File file)
        {
            Layer layer = makeShapefileLayer(file);
            if (layer != null)
            {
                layer.setPickEnabled(this.pickCheck.isSelected());
                layer.setName(file.getName());
                insertBeforePlacenames(this.getWwd(), layer);
                this.getLayerPanel().update(this.getWwd());
                this.layers.add(layer);
            }
        }

        protected Layer makeShapefileLayer(File file)
        {
            Shapefile shp = new Shapefile(file);

            String shapeType = shp.getShapeType();
            if (file.getName().equals("places.shp"))
            {
                return this.makeOSMPlacesLayer(shp);  // Test record selection on specific shapefile
            }
            else if (shapeType.equals(Shapefile.SHAPE_POINT))
            {
                return makePointLayer(shp);
            }
            else if (shapeType.equals(Shapefile.SHAPE_POLYLINE))
            {
                return makePolylineLayer2(shp);
            }
            else if (shapeType.equals(Shapefile.SHAPE_POLYGON))
            {
                return makePolylineLayer2(shp);
            }
            return null;
        }

        // Generic point layer
        protected Layer makePointLayer(Shapefile shp)
        {
            TextAndShapesLayer layer = new TextAndShapesLayer();
            Color color = colors[getWwd().getModel().getLayers().size() % colors.length];
            shp.getRecords(); // load records at least once
            
            int totPoints = this.addPoints(layer, shp.getBuffer(), color, 1, 0);
            System.out.println("Tot points: " + totPoints);

            return layer;
        }

        // Specific point layer for OSM places.
        protected Layer makeOSMPlacesLayer(Shapefile shp)
        {
            TextAndShapesLayer layer = new TextAndShapesLayer();
            List<ShapefileRecord> records = shp.getRecords();

            // Filter records for a particular sector
            records = getRecordsSubset(records, Sector.fromDegrees(43, 45, 5, 8));

            // Add points with different rendering attribute for different subsets
            int totPoints = 0;
            totPoints += this.addPoints(layer, getRecordsSubset(records, "type", "hamlet"), Color.BLACK, .3, 30e3);
            totPoints += this.addPoints(layer, getRecordsSubset(records, "type", "village"), Color.GREEN, .5, 100e3);
            totPoints += this.addPoints(layer, getRecordsSubset(records, "type", "town"), Color.CYAN, 1, 500e3);
            totPoints += this.addPoints(layer, getRecordsSubset(records, "type", "city"), Color.YELLOW, 2, 3000e3);

            System.out.println("Tot points: " + totPoints);

            return layer;
        }

        protected List<ShapefileRecord> getRecordsSubset(List<ShapefileRecord> records, String attributeName, Object value)
        {
            ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>(records);
            return ShapefileUtils.selectRecords(recordList, attributeName, value, false);
        }

        protected List<ShapefileRecord> getRecordsSubset(List<ShapefileRecord> records, Sector sector)
        {
            ArrayList<ShapefileRecord> recordList = new ArrayList<ShapefileRecord>(records);
            return ShapefileUtils.selectRecords(recordList, sector);
        }

        protected int addPoints(TextAndShapesLayer layer, List<ShapefileRecord> records, Color color,
            double scale, double labelMaxAltitude)
        {
            if (records == null)
                return 0;

            Font font = new Font("Arial", Font.BOLD, 10 + (int)(3 * scale));
            Color background = WWUtil.computeContrastingColor(color);

            // Gather point locations
            ArrayList<LatLon> locations = new ArrayList<LatLon>();
            for (ShapefileRecord rec : records)
            {
                if (rec == null || !rec.getShapeType().equals(Shapefile.SHAPE_POINT))
                    continue;

                ShapefileRecordPoint point = (ShapefileRecordPoint)rec;
                // Note: points are stored in the buffer as a sequence of X and Y with X = longitude, Y = latitude.
                double[] pointCoords = point.getPoint();
                LatLon location = LatLon.fromDegrees(pointCoords[1], pointCoords[0]);
                locations.add(location);

                // Add label
                if (labelMaxAltitude > 0)
                {
                    Label label = getRecordLabel(rec);
                    if (label != null)
                    {
                        label.setFont(font);
                        label.setColor(color);
                        label.setBackgroundColor(background);
                        label.setMaxActiveAltitude(labelMaxAltitude);
                        label.setPriority(labelMaxAltitude);
                        layer.addLabel(label);
                    }
                }
            }

            // Use one SurfaceIcons instance for all points
            BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .8f, color);
            SurfaceIcons sis = new SurfaceIcons(image, locations);
            sis.setMaxSize(4e3 * scale); // 4km
            sis.setMinSize(100);  // 100m
            sis.setScale(scale);
            sis.setOpacity(.8);
            layer.addRenderable(sis);

            return records.size();
        }

        protected int addPoints(TextAndShapesLayer layer, CompoundVecBuffer buffer, Color color,
            double scale, double labelMaxAltitude)
        {
            // Use one SurfaceIcons instance for all points
            BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_CIRCLE, .8f, color);
            SurfaceIcons sis = new SurfaceIcons(image, buffer.getLocations());
            sis.setMaxSize(4e3 * scale); // 4km
            sis.setMinSize(100);  // 100m
            sis.setScale(scale);
            sis.setOpacity(.8);
            layer.addRenderable(sis);

            return buffer.getTotalBufferSize();
        }

        // Handles polylines and polygons with a CompoundVecBuffer
        protected Layer makePolylineLayer(Shapefile shp)
        {
            TextAndShapesLayer layer = new TextAndShapesLayer();

            // Create surface shape
            List<ShapefileRecord> records = shp.getRecords();
            Sector sector = Sector.fromDegrees(shp.getBoundingRectangle());
            CompoundVecBuffer buffer = shp.getBuffer();
            boolean filled = shp.getShapeType().equals(Shapefile.SHAPE_POLYGON);
            SurfaceShape sp;
            if (filled)
            {
                sp = new SurfacePolygons(sector, buffer);
                // Set polygon group for each record
                int[] polygonGroups = new int[records.size()];
                for (int i = 0; i < records.size(); i++)
                    polygonGroups[i] = records.get(i).getFirstPartNumber();

                ((SurfacePolygons)sp).setPolygonRingGroups(polygonGroups);
            }
            else
            {
                sp = new SurfacePolylines(sector, buffer);
            }

            // Set rendering attributes
            this.setShapeAttributes(sp);

            layer.addRenderable(sp);
            return layer;
        }

        // Handles polygons with a CompoundVecBuffer one record per shape
        protected Layer makePolylineLayer2(Shapefile shp)
        {
            TextAndShapesLayer layer = new TextAndShapesLayer();

            // Create surface shape
            List<ShapefileRecord> records = shp.getRecords();
            Sector sector = Sector.fromDegrees(shp.getBoundingRectangle());
            CompoundVecBuffer buffer = shp.getBuffer();
            boolean filled = shp.getShapeType().equals(Shapefile.SHAPE_POLYGON);

            if (filled)
            {
                for (ShapefileRecord record : records)
                {
                    sector = Sector.fromDegrees(((ShapefileRecordPolygon)record).getBoundingRectangle());
                    // Use a subset from the shapefile buffer
                    CompoundVecBuffer subsetBuffer = CompoundVecBuffer.fromSubset(shp.getBuffer(),
                        record.getFirstPartNumber(), record.getNumberOfParts());
                    SurfacePolygons sp = new SurfacePolygons(sector, subsetBuffer);
                    // Set one polygon group starting at first sub-buffer
                    sp.setPolygonRingGroups(new int[] {0});
                    // Set rendering attributes
                    this.setShapeAttributes(sp);
                    layer.addRenderable(sp);
                }
            }
            else
            {
                SurfacePolylines sp = new SurfacePolylines(sector, buffer);
                // Set rendering attributes
                this.setShapeAttributes(sp);
                layer.addRenderable(sp);
            }

            return layer;
        }

        protected CompoundVecBuffer makeBuffer(List<? extends LatLon> locations)
        {
            int numPoints = locations.size();
            int numParts = 1;
            // Create buffers
            VecBuffer pointBuffer = new VecBuffer(2, numPoints, new BufferFactory.DoubleBufferFactory());
            IntBuffer offsetBuffer = BufferUtil.newIntBuffer(numParts);
            IntBuffer lengthBuffer = BufferUtil.newIntBuffer(numParts);
            // Feed buffers
            pointBuffer.putLocations(0, locations);
            offsetBuffer.put(0);
            lengthBuffer.put(numPoints);
            offsetBuffer.rewind();
            lengthBuffer.rewind();
            // Assemble compound buffer
            return new CompoundVecBuffer(pointBuffer, offsetBuffer, lengthBuffer, numParts,
                new BufferFactory.DoubleBufferFactory());
        }

        int colorIndex = 0;
        protected void setShapeAttributes(SurfaceShape shape)
        {
            Color color = colors[colorIndex++ % colors.length];
            ShapeAttributes attrs = new BasicShapeAttributes();
            attrs.setDrawOutline(true);
            attrs.setDrawInterior(shape instanceof SurfacePolygon || shape instanceof SurfacePolygons);
            attrs.setOutlineMaterial(new Material(color));
            attrs.setInteriorMaterial(new Material(color.brighter()));
            attrs.setOutlineOpacity(1);
            attrs.setOutlineWidth(1.2);
            attrs.setInteriorOpacity(.5);
            shape.setAttributes(attrs);

        }

        protected Label getRecordLabel(ShapefileRecord record)
        {
            String text = getRecordLabelText(record);
            if (text == null || text.length() == 0)
                return null;

            Position position = this.getRecordLabelPosition(record);
            if (position == null)
                return null;

            return new Label(text, position);
        }

        protected String getRecordLabelText(ShapefileRecord record)
        {
            AVList attr = record.getAttributes();
            if (attr.getEntries() == null || attr.getEntries().size() == 0)
                return null;

            for (Map.Entry entry : attr.getEntries())
            {
                if (((String)entry.getKey()).toUpperCase().equals("NAME"))
                    return (String)entry.getValue();
            }

            return null;
        }

        protected Position getRecordLabelPosition(ShapefileRecord record)
        {
            Position position = null;
            if (record.getShapeType().equals(Shapefile.SHAPE_POINT))
            {
                double[] point = ((ShapefileRecordPoint)record).getPoint();
                position = Position.fromDegrees(point[1], point[0], 0);
            }
            else if (record.getShapeType().equals(Shapefile.SHAPE_POLYLINE)
                || record.getShapeType().equals(Shapefile.SHAPE_POLYGON))
            {
                Sector boundingSector = Sector.fromDegrees(((ShapefileRecordPolyline)record).getBoundingRectangle());
                position = new Position(boundingSector.getCentroid(), 0);
            }

            return position;
        }

    }


    public static class SHPFileFilter extends FileFilter
    {
        public boolean accept(File file)
        {
            if (file == null)
            {
                String message = Logging.getMessage("nullValue.FileIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            return file.isDirectory() || file.getName().toLowerCase().endsWith(".shp");
        }

        public String getDescription()
        {
            return "ESRI Shapefiles (shp)";
        }
    }

    public static void main(String[] args)
    {
        start("World Wind Shapefiles", AppFrame.class);
    }
}
