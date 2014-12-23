/* Copyright (C) 2001, 2009 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.animation.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.util.ExtentVisibilitySupport;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.render.markers.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.orbit.OrbitView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * KeepingObjectsInView demonstrates keeping a set of scene elements visible by using the utility class {@link
 * gov.nasa.worldwind.examples.util.ExtentVisibilitySupport}. To run this demonstration, execute this class' main
 * method, then follow the on-screen instructions.
 * <p/>
 * The key functionality demonstrated by KeepingObjectsVisible is found in the internal classes {@link
 * KeepingObjectsInView.ViewController} and {@link gov.nasa.worldwind.examples.KeepingObjectsInView.ViewAnimator}.
 *
 * @author dcollins
 * @version $Id: KeepingObjectsInView.java 13005 2010-01-14 00:30:41Z dcollins $
 */
public class KeepingObjectsInView extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        protected Iterable<WWIcon> iconIterable;
        protected Iterable<MovableSphere> sphereIterable;
        protected ViewController viewController;
        protected RenderableLayer helpLayer;
        protected Annotation helpAnnotation;

        public AppFrame()
        {
            // Create collections of randomly located spheres and markers.
            this.iconIterable = createRandomIcons(5);
            this.sphereIterable = createRandomSpheres(5);
            // Set up a view controller to keep the spheres in view.
            this.viewController = new ViewController(this.getWwd());
            this.viewController.setIcons(this.iconIterable);
            this.viewController.setExtentHolders(this.sphereIterable);
            // Set up a layer to render the spheres.
            this.initLayers();
            // Set up swing components to toggle the view controller's behavior.
            this.initSwingComponents();

            // Enable feedback for each icon want the view to track.
            for (WWIcon icon : this.iconIterable)
            {
                icon.setValue(AVKey.FEEDBACK_ENABLED, Boolean.TRUE);
            }

            // Set up a one-shot timer to zoom to the spheres once the app launches.
            Timer timer = new Timer(1000, new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    enableHelpAnnotation();
                    viewController.gotoScene();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }

        protected void enableHelpAnnotation()
        {
            if (this.helpAnnotation != null)
                return;

            this.helpAnnotation = createHelpAnnotation(getWwd());
            this.helpLayer.addRenderable(helpAnnotation);
        }

        protected void disableHelpAnnotation()
        {
            if (this.helpAnnotation == null)
                return;

            this.helpLayer.removeRenderable(this.helpAnnotation);
            this.helpAnnotation = null;
        }

        protected void initLayers()
        {
            // Set up a layer to render the help annotation.
            this.helpLayer = new RenderableLayer();
            this.helpLayer.setName("Help Annotation");
            insertBeforePlacenames(this.getWwd(), this.helpLayer);

            // Set up a layer to render the icons. Disable WWIcon view clipping, since view tracking works best when an
            // icon's screen rectangle is known even when the icon is outside the view frustum.
            IconLayer iconLayer = new IconLayer();
            iconLayer.setViewClippingEnabled(false);
            iconLayer.setName("Random Icons");
            iconLayer.addIcons(this.iconIterable);
            insertBeforePlacenames(this.getWwd(), iconLayer);

            // Set up a layer to render the markers.
            RenderableLayer sphereLayer = new RenderableLayer();
            sphereLayer.setName("Random Spheres");
            sphereLayer.addRenderables(this.sphereIterable);
            insertBeforePlacenames(this.getWwd(), sphereLayer);

            this.getLayerPanel().update(this.getWwd());

            // Set up a SelectListener to drag the spheres.
            this.getWwd().addSelectListener(new SelectListener()
            {
                private BasicDragger dragger = new BasicDragger(getWwd());

                public void selected(SelectEvent event)
                {
                    // Delegate dragging computations to a dragger.
                    this.dragger.selected(event);

                    if (event.getEventAction().equals(SelectEvent.DRAG))
                    {
                        disableHelpAnnotation();
                        viewController.sceneChanged();
                    }
                }
            });
        }

        protected void initSwingComponents()
        {
            // Create a checkbox to enable/disable the view controller.
            JCheckBox checkBox = new JCheckBox("Enable view tracking", true);
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBox.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    boolean selected = ((AbstractButton) event.getSource()).isSelected();
                    viewController.setEnabled(selected);
                }
            });
            JButton button = new JButton("Go to objects");
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    viewController.gotoScene();
                }
            });
            Box box = Box.createVerticalBox();
            box.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30)); // top, left, bottom, right
            box.add(checkBox);
            box.add(Box.createVerticalStrut(5));
            box.add(button);

            this.getLayerPanel().add(box, BorderLayout.SOUTH);
        }
    }

    public static Iterable<WWIcon> createRandomIcons(int count)
    {
        Sector sector = Sector.fromDegrees(35, 45, -110, -100);

        ArrayList<WWIcon> icons = new ArrayList<WWIcon>();

        for (int i = 0; i < count; i++)
        {
            Angle lat = Angle.mix(Math.random(), sector.getMinLatitude(), sector.getMaxLatitude());
            Angle lon = Angle.mix(Math.random(), sector.getMinLongitude(), sector.getMaxLongitude());

            WWIcon icon = new UserFacingIcon("images/antenna.png", new Position(lat, lon, 0d));
            icon.setSize(new Dimension(64, 64));
            icons.add(icon);
        }

        return icons;
    }

    public static Iterable<MovableSphere> createRandomSpheres(int count)
    {
        Sector sector = Sector.fromDegrees(35, 45, -110, -100);

        ArrayList<MovableSphere> spheres = new ArrayList<MovableSphere>();

        for (int i = 0; i < count; i++)
        {
            Angle lat = Angle.mix(Math.random(), sector.getMinLatitude(), sector.getMaxLatitude());
            Angle lon = Angle.mix(Math.random(), sector.getMinLongitude(), sector.getMaxLongitude());

            spheres.add(new MovableSphere(new Position(lat, lon, 0d), 50000d));
        }

        return spheres;
    }

    public static Annotation createHelpAnnotation(WorldWindow wwd)
    {
        String text = "The view tracks the antenna icons and the <font color=\"#00DD00\">green</font> spheres. "
            + "Drag an icon or a sphere out of the window.";
        Rectangle viewport = ((Component) wwd).getBounds();
        Point screenPoint = new Point(viewport.width / 2, viewport.height / 2);

        AnnotationAttributes attr = new AnnotationAttributes();
        attr.setAdjustWidthToText(Annotation.SIZE_FIT_TEXT);
        attr.setFont(Font.decode("Arial-Bold-16"));
        attr.setTextAlign(AVKey.CENTER);
        attr.setTextColor(Color.WHITE);
        attr.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        attr.setBackgroundColor(new Color(0, 0, 0, 127)); // 50% transparent black
        attr.setBorderColor(Color.LIGHT_GRAY);
        attr.setLeader(FrameFactory.LEADER_NONE);
        attr.setCornerRadius(0);
        attr.setSize(new Dimension(350, 0));

        return new ScreenAnnotation(text, screenPoint, attr);
    }

    //**************************************************************//
    //********************  View Controller  ***********************//
    //**************************************************************//

    public static class ViewController
    {
        protected static final double SMOOTHING_FACTOR = 0.96;

        protected boolean enabled = true;
        protected WorldWindow wwd;
        protected ViewAnimator animator;
        protected Iterable<? extends WWIcon> iconIterable;
        protected Iterable<? extends ExtentHolder> extentHolderIterable;

        public ViewController(WorldWindow wwd)
        {
            this.wwd = wwd;
        }

        public boolean isEnabled()
        {
            return this.enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;

            if (this.animator != null)
            {
                this.animator.stop();
                this.animator = null;
            }
        }

        public Iterable<? extends WWIcon> getIcons()
        {
            return this.iconIterable;
        }

        public void setIcons(Iterable<? extends WWIcon> icons)
        {
            this.iconIterable = icons;
        }

        public Iterable<? extends ExtentHolder> getExtentHolders()
        {
            return this.extentHolderIterable;
        }

        public void setExtentHolders(Iterable<? extends ExtentHolder> extentHolders)
        {
            this.extentHolderIterable = extentHolders;
        }

        public boolean isSceneContained(View view)
        {
            ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
            this.addExtents(vs);

            return vs.areExtentsContained(view);
        }

        public Vec4[] computeViewLookAtForScene(View view)
        {
            Globe globe = this.wwd.getModel().getGlobe();
            double ve = this.wwd.getSceneController().getVerticalExaggeration();

            ExtentVisibilitySupport vs = new ExtentVisibilitySupport();
            this.addExtents(vs);

            return vs.computeViewLookAtContainingExtents(globe, ve, view);
        }

        public Position computePositionFromPoint(Vec4 point)
        {
            return this.wwd.getModel().getGlobe().computePositionFromPoint(point);
        }

        public void gotoScene()
        {
            Vec4[] lookAtPoints = this.computeViewLookAtForScene(this.wwd.getView());
            if (lookAtPoints == null || lookAtPoints.length != 3)
                return;

            Position centerPos = this.wwd.getModel().getGlobe().computePositionFromPoint(lookAtPoints[1]);
            double zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);

            this.wwd.getView().stopAnimations();
            this.wwd.getView().goTo(centerPos, zoom);
        }

        public void sceneChanged()
        {
            OrbitView view = (OrbitView) this.wwd.getView();

            if (!this.isEnabled())
                return;

            if (this.isSceneContained(view))
                return;

            if (this.animator == null || !this.animator.hasNext())
            {
                this.animator = new ViewAnimator(SMOOTHING_FACTOR, view, this);
                this.animator.start();
                view.stopAnimations();
                view.addAnimator(this.animator);
                view.firePropertyChange(AVKey.VIEW, null, view);
            }
        }

        protected void addExtents(ExtentVisibilitySupport vs)
        {
            // Compute screen extents for WWIcons which have feedback information from their IconRenderer.
            Iterable<? extends WWIcon> icons = this.getIcons();
            if (icons != null)
            {
                ArrayList<ExtentVisibilitySupport.ScreenExtent> screenExtents =
                    new ArrayList<ExtentVisibilitySupport.ScreenExtent>();

                for (WWIcon icon : icons)
                {
                    if (icon == null || icon.getValue(AVKey.FEEDBACK_ENABLED) == null ||
                        !icon.getValue(AVKey.FEEDBACK_ENABLED).equals(Boolean.TRUE)) 
                    {
                        continue;
                    }

                    screenExtents.add(new ExtentVisibilitySupport.ScreenExtent(
                        (Vec4) icon.getValue(AVKey.FEEDBACK_REFERENCE_POINT),
                        (Rectangle) icon.getValue(AVKey.FEEDBACK_SCREEN_BOUNDS)));
                }

                if (!screenExtents.isEmpty())
                    vs.setScreenExtents(screenExtents);
            }

            Iterable<? extends ExtentHolder> extentHolders = this.getExtentHolders();
            if (extentHolders != null)
            {
                Globe globe = this.wwd.getModel().getGlobe();
                double ve = this.wwd.getSceneController().getVerticalExaggeration();
                vs.setExtents(ExtentVisibilitySupport.extentsFromExtentHolders(extentHolders, globe, ve));
            }
        }
    }

    //**************************************************************//
    //********************  View Animator  *************************//
    //**************************************************************//

    public static class ViewAnimator extends BasicAnimator
    {
        protected static final double LOCATION_EPSILON = 1.0e-9;
        protected static final double ALTITUDE_EPSILON = 0.1;

        protected OrbitView view;
        protected ViewController viewController;
        protected boolean haveTargets;
        protected Position centerPosition;
        protected double zoom;

        public ViewAnimator(final double smoothing, OrbitView view, ViewController viewController)
        {
            super(new Interpolator()
            {
                public double nextInterpolant()
                {
                    return 1d - smoothing;
                }
            });

            this.view = view;
            this.viewController = viewController;
        }

        public void stop()
        {
            super.stop();
            this.haveTargets = false;
        }

        protected void setImpl(double interpolant)
        {
            this.updateTargetValues();

            if (!this.haveTargets)
            {
                this.stop();
                return;
            }

            if (this.valuesMeetCriteria(this.centerPosition, this.zoom))
            {
                this.view.setCenterPosition(this.centerPosition);
                this.view.setZoom(this.zoom);
                this.stop();
            }
            else
            {
                Position newCenterPos = Position.interpolateGreatCircle(interpolant, this.view.getCenterPosition(),
                    this.centerPosition);
                double newZoom = WWMath.mix(interpolant, this.view.getZoom(), this.zoom);
                this.view.setCenterPosition(newCenterPos);
                this.view.setZoom(newZoom);
            }

            this.view.firePropertyChange(AVKey.VIEW, null, this);
        }

        protected void updateTargetValues()
        {
            if (this.viewController.isSceneContained(this.view))
                return;

            Vec4[] lookAtPoints = this.viewController.computeViewLookAtForScene(this.view);
            if (lookAtPoints == null || lookAtPoints.length != 3)
                return;

            this.centerPosition = this.viewController.computePositionFromPoint(lookAtPoints[1]);
            this.zoom = lookAtPoints[0].distanceTo3(lookAtPoints[1]);
            if (this.zoom < view.getZoom())
                this.zoom = view.getZoom();

            this.haveTargets = true;
        }

        protected boolean valuesMeetCriteria(Position centerPos, double zoom)
        {
            Angle cd = LatLon.greatCircleDistance(this.view.getCenterPosition(), centerPos);
            double ed = Math.abs(this.view.getCenterPosition().getElevation() - centerPos.getElevation());
            double zd = Math.abs(this.view.getZoom() - zoom);

            return cd.degrees < LOCATION_EPSILON
                && ed < ALTITUDE_EPSILON
                && zd < ALTITUDE_EPSILON;
        }
    }

    //**************************************************************//
    //********************  Movable Marker  ************************//
    //**************************************************************//

    public static class MovableSphere extends BasicMarker implements Movable, ExtentHolder, Renderable
    {
        protected Position position;
        protected double radius;

        public MovableSphere(Position position, double radius)
        {
            super(position, new BasicMarkerAttributes(Material.GREEN, BasicMarkerShape.SPHERE, 1d));
            this.radius = radius;
        }

        public Position getReferencePosition()
        {
            return this.getPosition();
        }

        public void move(Position position)
        {
            if (position == null)
            {
                String message = Logging.getMessage("nullValue.PositionIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.moveTo(this.getPosition().add(position));
        }

        public void moveTo(Position position)
        {
            if (position == null)
            {
                String message = Logging.getMessage("nullValue.PositionIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            this.setPosition(position);
        }

        public Extent getExtent(Globe globe, double verticalExaggeration)
        {
            if (globe == null)
            {
                String message = Logging.getMessage("nullValue.GlobeIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            Vec4 centerPoint = this.computeCenterPoint(globe, verticalExaggeration);
            return new Sphere(centerPoint, this.radius);
        }

        public void render(DrawContext dc)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalArgumentException(message);
            }

            Extent extent = this.getExtent(dc.getGlobe(), dc.getVerticalExaggeration());
            if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
                return;

            Vec4 centerPoint = this.computeCenterPoint(dc.getGlobe(), dc.getVerticalExaggeration());

            OGLStateSupport ogss = new OGLStateSupport();
            ogss.setEnableColor(!dc.isPickingMode());
            ogss.setEnableBlending(!dc.isPickingMode());
            ogss.setEnableLighting(!dc.isPickingMode());
            ogss.setColorMode(OGLStateSupport.COLOR_NO_PREMULTIPLIED_ALPHA);
            ogss.setLightType(OGLStateSupport.LIGHT_DIRECTIONAL_FROM_VIEWER_POSITION);
            ogss.setLightPosition(new Vec4(1.0, 0.5, 1.0).normalize3());

            OGLStackHandler ogsh = new OGLStackHandler();
            ogsh.pushAttrib(dc.getGL(), ogss.getAttributeBits());
            ogsh.pushModelview(dc.getGL());

            try
            {
                ogss.apply(dc.getGL());
                this.getAttributes().apply(dc);
                this.render(dc, centerPoint, this.radius);
            }
            finally
            {
                ogsh.pop(dc.getGL());
            }
        }

        protected Vec4 computeCenterPoint(Globe globe, double verticalExaggeration)
        {
            Position pos = this.getPosition();
            double elev = globe.getElevation(pos.getLatitude(), pos.getLongitude());
            return globe.computePointFromPosition(pos.getLatitude(), pos.getLongitude(),
                (elev + pos.getElevation()) * verticalExaggeration);
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("Keeping Objects In View", AppFrame.class);
    }
}
