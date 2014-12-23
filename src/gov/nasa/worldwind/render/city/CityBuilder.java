/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.ActivityMarkerLayer;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.MarkerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceShapeLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.BasicAirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.editor.*;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.nasa.worldwind.render.markers.MarkerAttributes;
import gov.nasa.worldwind.terrain.ZeroElevationModel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.ArrayList;
import java.util.Arrays;

public class CityBuilder extends ApplicationTemplate
{   private static final String  BLACK_BACKGROUND = "E:/java_learning/worldwind13007/src/images/black.png";
    private static final String  GREY_BACKGROUND = "E:/java_learning/worldwind13007/src/images/grey.png";
    protected static final String PL_LAYER_NAME = "Street";
    protected static final String PG_LAYER_NAME = "Architecture";
    protected static final String PE_LAYER_NAME = "Envirenment";
    protected static final String PP_LAYER_NAME = "Activity";    
    protected static final String STREET_TYPE_L = "L";
    protected static final String STREET_TYPE_M = "M";
    protected static final String STREET_TYPE_S = "S";
    
    protected static final String ARCHITECTURE_TYPE_L = "L";
    protected static final String ARCHITECTURE_TYPE_M = "M";
    protected static final String ARCHITECTURE_TYPE_S = "S";
    
    protected static final String CLEAR_SELECTION_PL = "StreetBuilder.ClearSelection";
    protected static final String CLEAR_SELECTION_PG = "ArchitectureBuilder.ClearSelection";
    protected static final String CLEAR_SELECTION_PE = "EnvironmentBuilder.ClearSelection";
    protected static final String CLEAR_SELECTION_PP = "ActivityBuilder.ClearSelection";
    protected static final String SIZE_NEW_SHAPES_TO_VIEWPORT = "PolylineBuilder.SizeNewShapesToViewport";
    protected static final String ENABLE_EDIT_STREET = "StreetBuilder.EnableEdit";
    protected static final String ENABLE_EDIT_ARCHITECTURE = "ArchitectureBuilder.EnableEdit";
    protected static final String ENABLE_EDIT_ENVIRONMENT = "EnvironmentBuilder.EnableEdit";
    protected static final String ENABLE_EDIT_ACTIVITY = "Activity.EnableEdit";
    protected static final String OPEN = "PolylineBuilder.Open";
    protected static final String OPEN_URL = "PolylineBuilder.OpenUrl";
    protected static final String OPEN_DEMO_AIRSPACES = "PolylineBuilder.OpenDemoAirspaces";
    protected static final String NEW_STREET = "CityBuilder.NewStreet";
    protected static final String NEW_ARCHITECTURE= "CityBuilder.NewArchitecture";
    protected static final String NEW_ENVIRONMENT= "CityBuilder.NewEnvironment";
    protected static final String NEW_ACTIVITY= "CityBuilder.NewActivity";
    protected static final String REMOVE_SELECTED_PL = "StreetBuilder.RemoveSelected";
    protected static final String REMOVE_SELECTED_PG = "ArchitectureBuilder.RemoveSelected";
    protected static final String REMOVE_SELECTED_PE = "EnvironmentBuilder.RemoveSelected";
    protected static final String REMOVE_SELECTED_PP = "ActivityBuilder.RemoveSelected";
    protected static final String SAVE = "PolylineBuilder.Save";
    public static final String SELECTION_CHANGED_PL = "StreetBuilder.SelectionChanged";
    public static final String SELECTION_CHANGED_PG = "ArchitectureBuilder.SelectionChanged";
    public static final String SELECTION_CHANGED_PE = "EnvironmentBuilder.SelectionChanged";
    public static final String SELECTION_CHANGED_PP = "ActivityBuilder.SelectionChanged";
    
    public static final String  OPEN_STREET_DATA="StreetBuilder.OpenDatabase";
    public static final String  SAVE_STREET_DATA="StreetBuilder.SaveDatabase";
    public static final String  MODIFY_STREET_DATA="StreetBuilder.ModifyDatabase";
   
 
	protected static PolylineFactory[] defaultStreetFactories = new PolylineFactory[] {
			new PolylineFactory("L", false), 
			new PolylineFactory("M", false),
			new PolylineFactory("S", false) };

	protected static PolygonFactory[] defaultArchitectureFactories = new PolygonFactory[] {
			new PolygonFactory("House"),
			new PolygonFactory("Factory"),
			new PolygonFactory("Shool") };

	protected static PolylineFactory[] defaultEnvironmentFactories = new PolylineFactory[] {
			new PolylineFactory("WATER", true),
			new PolylineFactory("GREEN", true),
			new PolylineFactory("SPORT", true) };
	
	protected static ActivityFactory[] defaultActivityFactories = new ActivityFactory[] {
		new    ActivityFactory("People"),
		new    ActivityFactory("Goods"),
		new    ActivityFactory("Events") };
    
    

    protected static final double DEFAULT_SHAPE_SIZE_METERS = 200000.0; // 200 km
    
    public static ShapeAttributes getDefaultPlAttributes()
    {
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(Material.RED);
        attributes.setDrawOutline(true);
        attributes.setOutlineOpacity(.95);
        attributes.setOutlineWidth(5);
        return attributes;
    }

    public static ShapeAttributes getSelectionPlAttributes()
    {
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(Material.BLACK);
        attributes.setDrawOutline(true);
        attributes.setOutlineOpacity(0.8);
        attributes.setOutlineWidth(10);
        return attributes;
    }
    
    
       public static AirspaceAttributes getDefaultPgAttributes()
    {
        AirspaceAttributes attributes = new BasicAirspaceAttributes();
        attributes.setMaterial(new Material(Color.BLACK, Color.LIGHT_GRAY, Color.DARK_GRAY, Color.BLACK, 0.0f));
        attributes.setOutlineMaterial(Material.DARK_GRAY);
        attributes.setDrawOutline(true);
        attributes.setOpacity(0.95);
        attributes.setOutlineOpacity(.95);
        attributes.setOutlineWidth(2);
        return attributes;
    }

    public static AirspaceAttributes getSelectionPgAttributes()
    {
        AirspaceAttributes attributes = new BasicAirspaceAttributes();
        attributes.setMaterial(Material.WHITE);
        attributes.setOutlineMaterial(Material.BLACK);
        attributes.setDrawOutline(true);
        attributes.setOpacity(0.8);
        attributes.setOutlineOpacity(0.8);
        attributes.setOutlineWidth(2);
        return attributes;
    }
    
  
    public static ShapeAttributes getDefaultPeAttributes()
    {
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(Material.RED);
        attributes.setDrawOutline(true);
        attributes.setOutlineOpacity(.95);
        attributes.setOutlineWidth(5);
        attributes.setDrawInterior(true);
        return attributes;
    }

    public static ShapeAttributes getSelectionPeAttributes()
    {
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(Material.BLACK);
        attributes.setDrawOutline(true);
        attributes.setOutlineOpacity(0.8);
        attributes.setOutlineWidth(10);
        return attributes;
    }
    
    
    public static MarkerAttributes getDefaultPpAttributes()
    {   
    	BasicMarkerAttributes attributes = new BasicMarkerAttributes();
    	attributes.setShapeType(BasicMarkerShape.SPHERE);
    	attributes.setMaterial(Material.RED);
        return attributes;
    }

    public static MarkerAttributes getSelectionPpAttributes()
    {
    	BasicMarkerAttributes attributes = new BasicMarkerAttributes();
    	attributes.setShapeType(BasicMarkerShape.SPHERE);
    	attributes.setMaterial(Material.BLACK);
        return attributes;
    }
    
    
    
public static PolylineEditor getEditorFor(SurfaceShape surfaceShape)
    {
        if (surfaceShape instanceof CityPolyline)
        {
            PolylineEditor editor = new PolylineEditor();
            editor.setPolyline((CityPolyline) surfaceShape);
            return editor;
        }
       

        return null;
    }
    
    public static CityPolygonEditor getEditorFor(Airspace airspace)
    {
        if (airspace instanceof CityPolygon)
        {
            CityPolygonEditor editor = new CityPolygonEditor();
            editor.setPolygon((CityPolygon) airspace);
            return editor;
        }
       

        return null;
    }
    
    public static ActivityMarkerEditor getEditorFor(ActivityMarker marker)
    {
        
    	ActivityMarkerEditor editor = new  ActivityMarkerEditor();
    	    editor.setMarker(marker);
            return editor;

    }   
    
    
    public static void setPgEditorAttributes(CityPolygonEditor editor)
    {
        editor.setUseRubberBand(true);
        editor.setKeepControlPointsAboveTerrain(true);
    }
    
    
    
    
    public static String getNextName(String base)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(base);
        sb.append(nextEntryNumber++);
        return sb.toString();
    }

    private static long nextEntryNumber = 1;




    //**************************************************************//
    //********************  Main  **********************************//
    //**************************************************************//

    //protected static class ToolTip implements Renderable
    //{
    //    private String text;
    //    private Point point;
    //    private ScreenAnnotation annotation;
    //
    //    private static final Font toolTipFont = UIManager.getFont("ToolTip.font");
    //    private static final Color toolTipFg = UIManager.getColor("ToolTip.foreground");
    //    private static final Color toolTipBg = UIManager.getColor("ToolTip.background");
    //    private static final Border toolTipBorder = UIManager.getBorder("ToolTip.border");
    //
    //    public ToolTip(String text, Point point, double opacity)
    //    {
    //        this.text = text;
    //        this.point = point;
    //        this.annotation = this.createAnnotation(text, point);
    //        this.annotation.getAttributes().setOpacity(opacity);
    //    }
    //
    //    public String getText()
    //    {
    //        return this.text;
    //    }
    //
    //    public Point getPoint()
    //    {
    //        return this.point;
    //    }
    //
    //    public void render(DrawContext dc)
    //    {
    //        this.annotation.render(dc);
    //    }
    //
    //    protected ScreenAnnotation createAnnotation(String text, Point point)
    //    {
    //        AnnotationAttributes attributes = new AnnotationAttributes();
    //        attributes.setFont(toolTipFont);
    //        attributes.setTextColor(toolTipFg);
    //        attributes.setBorderColor(toolTipFg);
    //        attributes.setBackgroundColor(toolTipBg);
    //        attributes.setBorderWidth(1.0);
    //        attributes.setCornerRadius(0);
    //        attributes.setInsets(toolTipBorder.getBorderInsets(null));
    //        attributes.setEffect(MultiLineTextRenderer.EFFECT_NONE);
    //        attributes.setAntiAliasHint(Annotation.ANTIALIAS_DONT_CARE);
    //        attributes.setAdjustWidthToText(Annotation.SIZE_FIT_TEXT);
    //        attributes.setSize(new Dimension(300, 0));
    //
    //        ScreenAnnotation sa = new ScreenAnnotation(text, point, attributes);
    //        sa.setAlwaysOnTop(true);
    //
    //        return sa;
    //    }
    //}

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        // Airspace layer and editor UI components.
        public SurfaceShapeLayer surfaceShapeLayer;
        public AirspaceLayer airspaceLayer;
        public SurfaceShapeLayer peShapeLayer;
        public ActivityMarkerLayer markerLayer;
        
        
        
        
        public PolylineBuilderModel  plBuilderModel;
        public PolygonBuilderModel   pgBuilderModel;
        public PolylineBuilderModel  peBuilderModel;
        public MarkerBuilderModel    ppBuilderModel;
        
        public CityBuilderController builderController;
        public StreetBuildController streetController;
        public ArchitectureBuildController architectureController;
        
        
        public java.sql.Connection conn; 
        public DatabaseConn cityInterface; 
        // Tool tip layer.
        //private ToolTip toolTip;
        //private RenderableLayer toolTipLayer;

        public AppFrame()
        {   
        	this.layerPanel.setVisible(false);
        	
        	this.surfaceShapeLayer = new SurfaceShapeLayer();
            this.surfaceShapeLayer.setName(PL_LAYER_NAME);
            
            this.airspaceLayer = new AirspaceLayer();
            this.airspaceLayer .setName(PG_LAYER_NAME);
            
            this.peShapeLayer = new SurfaceShapeLayer();
            this.peShapeLayer.setName(PE_LAYER_NAME);
            
            this.markerLayer= new ActivityMarkerLayer();
            this.markerLayer.setOverrideMarkerElevation(true);
            this.markerLayer.setKeepSeparated(false);
            this.markerLayer.setName(PP_LAYER_NAME);
            
             LayerList layerList;
	         layerList=this.getWwd().getModel().getLayers();
	         for (Layer layer : layerList){layer.setEnabled(false);}
            
            
            try
            {
            	    
                    SurfaceImage background = new SurfaceImage(GREY_BACKGROUND, new ArrayList<LatLon>(Arrays.asList(
                    LatLon.fromDegrees(-90, -180),
                    LatLon.fromDegrees(-90, 180),
                    LatLon.fromDegrees(90, 180),
                    LatLon.fromDegrees(90, -180)
                    )));
                RenderableLayer backgroundLayer = new RenderableLayer();
                backgroundLayer.setName("background");
                backgroundLayer.setPickEnabled(false);
                   
                backgroundLayer.addRenderable(background);                
                insertBeforeLayerName(this.getWwd(),backgroundLayer,"Stars");
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            
            insertBeforePlacenames(this.getWwd(), this.surfaceShapeLayer);
            insertBeforePlacenames(this.getWwd(), this.peShapeLayer);
            insertBeforePlacenames(this.getWwd(), this.airspaceLayer);
            insertBeforePlacenames(this.getWwd(), this.markerLayer);
             // this.getWwd().getModel().setShowWireframeExterior(true);
           
            this.getLayerPanel().update(this.getWwd());
            
            View view = this.getWwd().getView();
	    	LatLon latlon=LatLon.fromDegrees(39.85, 116.4);
	    	Position position=new Position(latlon,10000);	  
	    	view.setEyePosition(position);
	    	//this.getWwd().getModel().getGlobe().setElevationModel(new ZeroElevationModel());    	
            this.plBuilderModel = new PolylineBuilderModel();
            this.pgBuilderModel = new PolygonBuilderModel();
            this.peBuilderModel = new PolylineBuilderModel();
            this.ppBuilderModel = new MarkerBuilderModel();
            
            this.builderController = new CityBuilderController(this);
            this.builderController.setModel(this.plBuilderModel,this.pgBuilderModel,this.peBuilderModel,this.ppBuilderModel);
            this.streetController=new StreetBuildController(this.builderController); 
            this.architectureController=new ArchitectureBuildController(this.builderController); 
            this.cityInterface=new DatabaseConn(this);
            

            //this.toolTipLayer = new RenderableLayer();
            //this.toolTipLayer.setPickEnabled(false); // Don't want to pick the tool tip
            //insertBeforeCompass(this.getWwd(), this.toolTipLayer);
            makeToolBar(this, this.builderController);
            makeMenuBar(this, this.builderController, this.streetController,this.architectureController);
           
        }

 

        public SurfaceShapeLayer getSurfaceShapeLayer()
        {
            return this.surfaceShapeLayer;
        }
        
        public AirspaceLayer getAirspaceLayer()
        {
            return this.airspaceLayer;
        }
        
        public SurfaceShapeLayer getPeShapeLayer()
        {
            return this.peShapeLayer;
        }
        
        public ActivityMarkerLayer getMarkerLayer()
        {
            return this.markerLayer;
        }
        
        
        

        //public ToolTip getWorldWindToolTip()
        //{
        //    return this.toolTip;
        //}
        //
        //public void setWorldWindToolTip(ToolTip toolTip)
        //{
        //    this.toolTip = toolTip;
        //    this.toolTipLayer.removeAllRenderables();
        //    if (this.toolTip != null)
        //        this.toolTipLayer.addRenderable(this.toolTip);
        //}

        public static void makeToolBar(AppFrame frame, final CityBuilderController controller)
        { JToolBar toolBar = new JToolBar("controler");
        
        ControlerAction D_LayerAction=new ControlerAction( 
        		"D_Layer", 
        		new ImageIcon("./resources/layer.gif "), 
        		"display layer", 
        		'L',
        		frame);
        
        ControlerAction H_LayerAction=new ControlerAction( 
        		"H_Layer", 
        		new ImageIcon("./resources/layer_h.gif "), 
        		"hide layer",
        		'H', 
        		frame);
       
        ControlerAction fullJustifyAction   =   new   ControlerAction( 
        		"Contraler",
        		new   ImageIcon("./resources/builder_pln.gif "),   
        		"Full   justify   text ",   
        		'F', 
        		frame);
        ControlerAction   datebaseAction   =   new   ControlerAction( 
        		"Datebase",   
        		new   ImageIcon("./resources/conn.gif "),   
        		"datebase action",   
        		'D', 
        		frame);
        
        
        toolBar.add(D_LayerAction);
        toolBar.add(H_LayerAction);
        toolBar.add(fullJustifyAction);
        toolBar.add(datebaseAction);
        
        
        frame.getContentPane().add(toolBar,   BorderLayout.NORTH);
        }
        
        public static void makeMenuBar(JFrame frame, final CityBuilderController controller, final StreetBuildController streetController,
        		final ArchitectureBuildController architectureController)
        {
            JMenuBar menuBar = new JMenuBar();
           
            final JCheckBoxMenuItem resizeNewShapesItem;
            final JCheckBoxMenuItem enableEditItem01;
            final JCheckBoxMenuItem enableEditItem02;
            final JCheckBoxMenuItem enableEditItem03;
            final JCheckBoxMenuItem enableEditItem04;

            

            JMenu menu = new JMenu("File");
            {
                JMenuItem item = new JMenuItem("Open...");
                item.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(OPEN);
                item.addActionListener(controller);
                menu.add(item);

                item = new JMenuItem("Open URL...");
                item.setActionCommand(OPEN_URL);
                item.addActionListener(controller);
                menu.add(item);

                item = new JMenuItem("Save...");
                item.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(SAVE);
                item.addActionListener(controller);
                menu.add(item);
                menu.addSeparator();

            }
            menuBar.add(menu);

            menu = new JMenu("Shape");
            {
                JMenu subMenu_01 = new JMenu("Street");
                for (final PolylineFactory factory : defaultStreetFactories)
                {
                    JMenuItem item = new JMenuItem(factory.toString(factory.type));
                    item.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            streetController.createNewPlEntry(factory);
                        }
                    });
                    subMenu_01.add(item);
                }
                
                
                JMenu subMenu_02 = new JMenu("Architecture");
                for (final PolygonFactory factory :defaultArchitectureFactories)
                {
                    JMenuItem item = new JMenuItem(factory.toString(factory.type));
                    item.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            architectureController.createNewPgEntry(factory);
                        }
                    });
                    subMenu_02.add(item);
                }
                
                JMenu subMenu_03 = new JMenu("Environment");
                for (final PolylineFactory factory :defaultEnvironmentFactories)
                {
                    JMenuItem item = new JMenuItem(factory.toString(factory.type));
                    item.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                        	
                        	controller.createNewPeEntry(factory);
                        }
                    });
                    subMenu_03.add(item);
                }
                
                
                JMenu subMenu_04 = new JMenu("Activity");
                for (final ActivityFactory factory :defaultActivityFactories)
                {
                    JMenuItem item = new JMenuItem(factory.toString());
                    item.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            //controller.createNewPlEntry(factory);
                        }
                    });
                    subMenu_03.add(item);
                }
                
                menu.add(subMenu_01);
                menu.add(subMenu_02);
                menu.add(subMenu_03);
                menu.add(subMenu_04);
                

                resizeNewShapesItem = new JCheckBoxMenuItem("Fit new shapes to viewport");
                resizeNewShapesItem.setActionCommand(SIZE_NEW_SHAPES_TO_VIEWPORT);
                resizeNewShapesItem.addActionListener(controller);
                resizeNewShapesItem.setState(controller.isResizeNewShapesToViewport());
                menu.add(resizeNewShapesItem);

                enableEditItem01 = new JCheckBoxMenuItem("Enable street editing");
                enableEditItem01.setActionCommand(ENABLE_EDIT_STREET);
                enableEditItem01.addActionListener(controller);
                enableEditItem01.setState(controller.isPlEnableEdit());
                menu.add(enableEditItem01);
                
                enableEditItem02 = new JCheckBoxMenuItem("Enable architecture editing");
                enableEditItem02.setActionCommand(ENABLE_EDIT_ARCHITECTURE);
                enableEditItem02.addActionListener(controller);
                enableEditItem02.setState(controller.isPgEnableEdit());
                menu.add(enableEditItem02);
                enableEditItem03 = new JCheckBoxMenuItem("Enable environment editing");
                enableEditItem03.setActionCommand(ENABLE_EDIT_ENVIRONMENT);
                enableEditItem03.addActionListener(controller);
                enableEditItem03.setState(controller.isPeEnableEdit());
                menu.add(enableEditItem03);
                enableEditItem04 = new JCheckBoxMenuItem("Enable activity editing");
                enableEditItem04.setActionCommand(ENABLE_EDIT_ACTIVITY);
                enableEditItem04.addActionListener(controller);
                enableEditItem04.setState(controller.isPpEnableEdit());
                menu.add(enableEditItem04);
                
                
                
            }
            menuBar.add(menu);
            menu = new JMenu("Selection");
            {
                //JMenuItem item = new JMenuItem("Zoom To Selection");
                //item.setActionCommand(ZOOM_TO_SELECTED);
                //item.addActionListener(controller);
                //menu.add(item);

                JMenuItem item = new JMenuItem("Deselect_PL");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(CLEAR_SELECTION_PL);
                item.addActionListener(controller);
                menu.add(item);
                
                item = new JMenuItem("Deselect_PG");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(CLEAR_SELECTION_PG);
                item.addActionListener(controller);
                menu.add(item);
                
                item = new JMenuItem("Deselect_PE");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(CLEAR_SELECTION_PE);
                item.addActionListener(controller);
                menu.add(item);
                
                item = new JMenuItem("Deselect_PP");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                item.setActionCommand(CLEAR_SELECTION_PP);
                item.addActionListener(controller);
                menu.add(item);
                
                item = new JMenuItem("Delete_PL");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                item.setActionCommand(REMOVE_SELECTED_PL);
                item.addActionListener(controller);
                menu.add(item);
                item = new JMenuItem("Delete_PG");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                item.setActionCommand(REMOVE_SELECTED_PG);
                item.addActionListener(controller);
                menu.add(item);
                item = new JMenuItem("Delete_PE");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                item.setActionCommand(REMOVE_SELECTED_PE);
                item.addActionListener(controller);
                item = new JMenuItem("Delete_PP");
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
                item.setActionCommand(REMOVE_SELECTED_PP);
                item.addActionListener(controller);
                menu.add(item);
            }
            menuBar.add(menu);

            frame.setJMenuBar(menuBar);

            controller.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    //noinspection StringEquality
                    if (e.getPropertyName() == SIZE_NEW_SHAPES_TO_VIEWPORT)
                    {
                        resizeNewShapesItem.setSelected(controller.isResizeNewShapesToViewport());
                    }
                    else //noinspection StringEquality
                        if (e.getPropertyName() == ENABLE_EDIT_STREET)
                    {
                        enableEditItem01.setSelected(controller.isPlEnableEdit());
                    }
                    else //noinspection StringEquality
                            if (e.getPropertyName() == ENABLE_EDIT_ARCHITECTURE)
                        {
                            enableEditItem02.setSelected(controller.isPgEnableEdit());
                        }  
                    else //noinspection StringEquality
                                if (e.getPropertyName() == ENABLE_EDIT_ENVIRONMENT)
                            {
                                enableEditItem03.setSelected(controller.isPeEnableEdit());
                            } 
                   else //noinspection StringEquality
                                    if (e.getPropertyName() == ENABLE_EDIT_ACTIVITY)
                                {
                                    enableEditItem04.setSelected(controller.isPpEnableEdit());
                                }
                }
            });
        }
        
        
        public static class   ControlerAction   extends   AbstractAction   {
            public AppFrame appFrame;
            public   ControlerAction(String text,Icon icon, String description,char accelerator,AppFrame appFrame )   {
                super(text,   icon);
                this.appFrame=appFrame;
                putValue(ACCELERATOR_KEY,   KeyStroke.getKeyStroke(accelerator,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                putValue(SHORT_DESCRIPTION,   description);
            }

            public   void   actionPerformed(ActionEvent   e)   {
            	if(getValue(NAME)=="D_Layer"){ 
            		appFrame.layerPanel.setVisible(true);} 
            	if(getValue(NAME)=="H_Layer"){
            		appFrame.layerPanel.setVisible(false);}
                if(getValue(NAME)=="Contraler"){
                	 JFrame controlFrame = new JFrame();
                     controlFrame.getContentPane().add(new StreetBuilderPanel(appFrame.plBuilderModel,  appFrame.builderController, appFrame.streetController));
                     controlFrame.pack();
                     controlFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                     controlFrame.setVisible(true);
                     controlFrame.setTitle("controler");
                     controlFrame.setAlwaysOnTop(true);
                     };
                 if(getValue(NAME)=="Datebase"){
                    	appFrame.cityInterface.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                    	appFrame.cityInterface.setVisible(true);
                    	appFrame.cityInterface.setAlwaysOnTop(true);
                         };
            	
            }
        }
        
    }
    
    public static void main(String[] args)
    {
        ApplicationTemplate.start("Micro Thinking & Micro Action", AppFrame.class);
    }
}




/*
 * 做了一个简单的程序，不知道会否对你有帮助。

public class a {
   public static void main(String[]args){

       int a[]={3,9,8};//这个是数组的静态初始化.

       Date days[]={new Date(1,4,2994),new Date(2,4,2004),new Date(2,5,2005)};
       // 创建了3个Date对象放在days[]数组里。

      //这里还有种写法。你可以先定义个数组，然后动态的进行付值。

     //这样写可能烦了点，你也可以用for循环来进行动态赋值。

     //列：Date days[]; 

     //       days=new Date[3];

     //       days[0]=new Date(1,2,3);

     //       days[1]=new Date(1,2,3);

     //       days[2]=new Date(1,2,3);

       for(int i=0;i<days.length;i++){

      //循环数组里的对象
        System.out.println(days[i].a);
      //将对象中的a属性打印输出。          

}
}
}

class Date{
int a,b,c;
Date(int x,int y,int z){
a=x;
b=y;
z=c;
}
}
 * 
 */
