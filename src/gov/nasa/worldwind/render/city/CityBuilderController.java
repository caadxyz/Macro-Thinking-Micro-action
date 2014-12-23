package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceShape;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.editor.AirspaceEditEvent;
import gov.nasa.worldwind.render.airspaces.editor.AirspaceEditListener;
import gov.nasa.worldwind.render.airspaces.editor.AirspaceEditorController;
import gov.nasa.worldwind.render.airspaces.editor.CityPolygonEditor;
import gov.nasa.worldwind.render.airspaces.editor.PolylineEditor;
import gov.nasa.worldwind.render.airspaces.editor.SurfaceEditEvent;
import gov.nasa.worldwind.render.airspaces.editor.SurfaceEditListener;
import gov.nasa.worldwind.render.airspaces.editor.SurfaceEditorController;
import gov.nasa.worldwind.render.city.CityBuilder.AppFrame;
import gov.nasa.worldwind.render.city.editor.MarkerEditEvent;
import gov.nasa.worldwind.render.city.editor.MarkerEditListener;
import gov.nasa.worldwind.render.city.editor.MarkerEditorController;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import org.postgis.Geometry;
import org.postgis.MultiLineString;
import org.postgis.PGbox3d;
import org.postgis.PGgeometry;
import org.postgis.Point;

//**************************************************************//
//********************  Airspace Builder Controller  ***********//
//**************************************************************//

public class CityBuilderController extends WWObjectImpl implements ActionListener, MouseListener,
    SurfaceEditListener, AirspaceEditListener, MarkerEditListener
{
    private CityBuilder.AppFrame app;    
    private PolylineBuilderModel plModel;
    private PolygonBuilderModel  pgModel;
    private PolylineBuilderModel peModel;
    private MarkerBuilderModel   ppModel;
   
    private PolylineEntry  selectedPlEntry;
    private PolygonEntry   selectedPgEntry;
    private PolylineEntry  selectedPeEntry;
    private MarkerEntry    selectedPpEntry;
    
    private StreetBuilderPanel streetView;
    private ArchitectureBuilderPanel architectureView;
    private EnvironmentBuilderPanel environmentView;
    private ActivityBuilderPanel activityView;
    
    private SurfaceEditorController  plEditorController;
    private AirspaceEditorController pgEditorController;
    private SurfaceEditorController  peEditorController;
    private MarkerEditorController  ppEditorController;
    
    
    
    private boolean enabled = true;
    
    private boolean plEnableEdit = true;
    private boolean pgEnableEdit = true;
    private boolean peEnableEdit = true;
    private boolean ppEnableEdit = true;
    
    
    private boolean resizeNewShapes;
    
    // UI components.
    private JFileChooser fileChooser;

    public CityBuilderController(CityBuilder.AppFrame app)
    {
        this.app = app;
        this.plEditorController = new SurfaceEditorController();
        this.pgEditorController = new AirspaceEditorController();
        this.peEditorController = new SurfaceEditorController();
        this.ppEditorController = new MarkerEditorController();;

        // The ordering is important here; we want first pass at mouse events.
        this.plEditorController.setWorldWindow(this.app.getWwd());
        this.pgEditorController.setWorldWindow(this.app.getWwd());
        this.peEditorController.setWorldWindow(this.app.getWwd());
        this.ppEditorController.setWorldWindow(this.app.getWwd());
        
        this.app.getWwd().getInputHandler().addMouseListener(this);
        
        
    }

    public AppFrame getApp()
    {
        return this.app;
    }

    public PolylineBuilderModel getPlModel()
    {
        return this.plModel;
    }
    public PolygonBuilderModel getPgModel()
    {
        return this.pgModel;
    }
    public PolylineBuilderModel getPeModel()
    {
        return this.peModel;
    }
    public MarkerBuilderModel getPpModel()
    {
        return this.ppModel;
    }

    public void setModel(PolylineBuilderModel plModel,PolygonBuilderModel pgModel,PolylineBuilderModel peModel,MarkerBuilderModel ppModel)
    {
        this.plModel = plModel;
        this.pgModel = pgModel;
        this.peModel = peModel;
        this.ppModel = ppModel;
    }

   public StreetBuilderPanel getStreetView()
   {
      return this.streetView;
      }

    public void setStreetView(StreetBuilderPanel view)
    {
      this.streetView = view;
      
    }
    
    public ArchitectureBuilderPanel getArchitectureView()
    {
       return this.architectureView;
       }
    public void setArchitectureView(ArchitectureBuilderPanel view) {
    	this.architectureView = view;
	}
    
    public EnvironmentBuilderPanel getEnvironmentView()
    {
       return this.environmentView;
       }
    public void setEnvironmentView(EnvironmentBuilderPanel view)
    {
      this.environmentView = view;
    }
    
    public ActivityBuilderPanel getActivityView()
    {
       return this.activityView;
       }
    public void setActivityView(ActivityBuilderPanel view)
    {
      this.activityView = view;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }
  
   public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        getStreetView().setEnabled(enabled);
        getArchitectureView().setEnabled(enabled);
        getEnvironmentView().setEnabled(enabled);
        getActivityView().setEnabled(enabled);
        getApp().setEnabled(enabled);
    }
 
    
    
    public boolean isPlEnableEdit()
    {
        return this.plEnableEdit;
    }
    public boolean isPgEnableEdit()
    {
        return this.pgEnableEdit;
    }
    public boolean isPeEnableEdit()
    {
        return this.peEnableEdit;
    }
    public boolean isPpEnableEdit()
    {
        return this.ppEnableEdit;
    }

    public void setPlEnableEdit(boolean enable)
    {
        this.plEnableEdit = enable;
        this.handlePlEnableEdit(enable);
        this.firePropertyChange(CityBuilder.ENABLE_EDIT_STREET, null, enable);
    }
    
    
   public void setPgEnableEdit(boolean enable)
    {
        this.pgEnableEdit = enable;
        this.handlePgEnableEdit(enable);
        this.firePropertyChange(CityBuilder.ENABLE_EDIT_ARCHITECTURE, null, enable);
    }
    
   public void setPeEnableEdit(boolean enable)
   {
       this.peEnableEdit = enable;
       this.handlePeEnableEdit(enable);
       this.firePropertyChange(CityBuilder.ENABLE_EDIT_ENVIRONMENT, null, enable);
   }
   public void setPpEnableEdit(boolean enable)
   {
       this.ppEnableEdit = enable;
       this.handlePpEnableEdit(enable);
       this.firePropertyChange(CityBuilder.ENABLE_EDIT_ACTIVITY, null, enable);
   }
   

    public boolean isResizeNewShapesToViewport()
    {
        return this.resizeNewShapes;
    }

    public void setResizeNewShapesToViewport(boolean resize)
    {
        this.resizeNewShapes = resize;
        this.firePropertyChange(CityBuilder.SIZE_NEW_SHAPES_TO_VIEWPORT, null, resize);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (!this.isEnabled())
        {
            return;
        }

        //noinspection StringEquality
        
       if (e.getActionCommand() == CityBuilder.NEW_ENVIRONMENT)
        {   
        	if (this.getEnvironmentView().getSelectedFactory().equals("Water")) this.createNewPeEntry(CityBuilder.defaultEnvironmentFactories[0]);
        	if (this.getEnvironmentView().getSelectedFactory().equals("Green")) this.createNewPeEntry(CityBuilder.defaultEnvironmentFactories[1]);
        	if (this.getEnvironmentView().getSelectedFactory().equals("Sport")) this.createNewPeEntry(CityBuilder.defaultEnvironmentFactories[2]);
        }
        else if (e.getActionCommand() == CityBuilder.NEW_ACTIVITY)
        {   
        	if (this.getActivityView().getSelectedFactory().equals("People")) this.createNewPpEntry(CityBuilder.defaultActivityFactories[0]);
        	if (this.getActivityView().getSelectedFactory().equals("Goods"))  this.createNewPpEntry(CityBuilder.defaultActivityFactories[1]);
        	if (this.getActivityView().getSelectedFactory().equals("Events")) this.createNewPpEntry(CityBuilder.defaultActivityFactories[2]);
        }       
        
        
        
        
        
        /*
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.CLEAR_SELECTION_PL)
        {   
            	
            this.selectPlEntry(null, true);
        }
        */
       /*else //noinspection StringEquality
                if (e.getActionCommand() == CityBuilder.CLEAR_SELECTION_PG)
            {   
                	
                this.selectPgEntry(null, true);
            }
            */
        else //noinspection StringEquality
                    if (e.getActionCommand() == CityBuilder.CLEAR_SELECTION_PE)
                {   
                    	
                    this.selectPeEntry(null, true);
                }
       else //noinspection StringEquality
                        if (e.getActionCommand() == CityBuilder.CLEAR_SELECTION_PP)
                    {   
                        	
                        this.selectPpEntry(null, true);
                    }
        
        
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.SIZE_NEW_SHAPES_TO_VIEWPORT)
        {
            if (e.getSource() instanceof AbstractButton)
            {
                boolean selected = ((AbstractButton) e.getSource()).isSelected();
                this.setResizeNewShapesToViewport(selected);
            }
        }
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.ENABLE_EDIT_STREET)
        {
            if (e.getSource() instanceof AbstractButton)
            {
                boolean selected = ((AbstractButton) e.getSource()).isSelected();
                this.setPlEnableEdit(selected);
            }
        }
       else //noinspection StringEquality
                if (e.getActionCommand() == CityBuilder.ENABLE_EDIT_ARCHITECTURE)
            {
                if (e.getSource() instanceof AbstractButton)
                {
                    boolean selected = ((AbstractButton) e.getSource()).isSelected();
                    this.setPgEnableEdit(selected);
                }
            }       
                else //noinspection StringEquality
                    if (e.getActionCommand() == CityBuilder.ENABLE_EDIT_ENVIRONMENT)
                {
                    if (e.getSource() instanceof AbstractButton)
                    {
                        boolean selected = ((AbstractButton) e.getSource()).isSelected();
                        this.setPeEnableEdit(selected);
                    }
                }
        
                    else //noinspection StringEquality
                        if (e.getActionCommand() == CityBuilder.ENABLE_EDIT_ACTIVITY)
                    {
                        if (e.getSource() instanceof AbstractButton)
                        {
                            boolean selected = ((AbstractButton) e.getSource()).isSelected();
                            this.setPpEnableEdit(selected);
                        }
                    }
        
        
        
        
        
        
        
        
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.OPEN)
        {
            this.openFromFile();
        }
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.OPEN_URL)
        {
            this.openFromURL();
        }
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.OPEN_DEMO_AIRSPACES)
        {
            
        }
       
            //else //noinspection StringEquality
           // if (e.getActionCommand() == CityBuilder.REMOVE_SELECTED_PL)
        //{
          //  this.removePlEntries(Arrays.asList(this.getPlSelectedEntries()));
        //}
        
        
       // else //noinspection StringEquality
         //       if (e.getActionCommand() == CityBuilder.REMOVE_SELECTED_PG)
           // {
             //      
               // 	this.removePgEntries(Arrays.asList(this.getPgSelectedEntries()));
                	
            //}
          else //noinspection StringEquality
                    if (e.getActionCommand() == CityBuilder.REMOVE_SELECTED_PE)
                {
                    this.removePeEntries(Arrays.asList(this.getPeSelectedEntries()));
                }
           else //noinspection StringEquality
                   if (e.getActionCommand() == CityBuilder.REMOVE_SELECTED_PP)
                    {   

                    
                        this.removePpEntries(Arrays.asList(this.getPpSelectedEntries()));
                    }
        
        
        
        
        
        
        
        
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.SAVE)
        {
            this.saveToFile();
        }
        else //noinspection StringEquality
            if (e.getActionCommand() == CityBuilder.SELECTION_CHANGED_PL)
        {
            this.viewSelectionPlChanged();
        }
         else //noinspection StringEquality
                if (e.getActionCommand() == CityBuilder.SELECTION_CHANGED_PG)
            {
                this.viewSelectionPgChanged();
                //System.out.println(this.selectedPgEntry.getValue(AVKey.DISPLAY_NAME));
                
            }
        else //noinspection StringEquality
                    if (e.getActionCommand() == CityBuilder.SELECTION_CHANGED_PE)
                {
                    this.viewSelectionPeChanged();
                } 
       else //noinspection StringEquality
                        if (e.getActionCommand() == CityBuilder.SELECTION_CHANGED_PP)
                    {
                        this.viewSelectionPpChanged();
                    }        
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        if (e == null || e.isConsumed())
        {
            return;
        }

        if (!this.isEnabled())
        {
            return;
        }

        //noinspection StringEquality
        if (e.getButton() == MouseEvent.BUTTON1)
        {
            this.handleSelect();
        }
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void surfaceMoved(SurfaceEditEvent e)
    {
       
    }

    public void surfaceResized(SurfaceEditEvent e)
    {
     
    }
    
    
    
    

    public void controlPointAdded(SurfaceEditEvent e)
    {
    }

    public void controlPointRemoved(SurfaceEditEvent e)
    {
    }

    public void controlPointChanged(SurfaceEditEvent e)
    {
    }

    protected void handleSelect()
    {
        // If the picked object is null or something other than an airspace, then ignore the mouse click. If we
        // deselect the current entry at this point, the user cannot easily navigate without loosing the selection.

        PickedObjectList pickedObjects = this.getApp().getWwd().getObjectsAtCurrentPosition();

        Object topObject = pickedObjects.getTopObject();
        
        if (!(topObject instanceof SurfaceShape) && !(topObject instanceof Airspace) && !(topObject instanceof Marker))
            return;
       
        
        if(topObject instanceof SurfaceShape){
        	
        	
        PolylineEntry pickedEntry = this.getEntryFor((CityPolyline) topObject);
        
        if (pickedEntry == null)
            return;

        if (pickedEntry.getPolyline().closed){
        if (this.getSelectedPlEntry() != pickedEntry)
        {
            this.selectPeEntry(pickedEntry, true);
        }
        }
        else {
        	{
            this.selectPlEntry(pickedEntry, true);
           
        }}
        
        
        
        }
        
        
        if(topObject instanceof Airspace){
            PolygonEntry pickedEntry = this.getEntryFor((CityPolygon) topObject);
            
            if (pickedEntry == null)
                return;

            if (this.getSelectedPgEntry() != pickedEntry)
            {
                this.selectPgEntry(pickedEntry, true);
            }
            }
        if(topObject instanceof ActivityMarker){
        	MarkerEntry pickedEntry = this.getEntryFor((ActivityMarker) topObject);
            if (pickedEntry == null)
                return;

            if (this.getSelectedPpEntry() != pickedEntry)
            {
                this.selectPpEntry(pickedEntry, true);
            }
            }
        
        
    }

   

	//protected void handleSelectHover(SelectEvent e)
    //{
    //    ToolTip toolTip = this.getApp().getWorldWindToolTip();
    //    if (toolTip != null)
    //    {
    //        this.getApp().setWorldWindToolTip(null);
    //        this.getApp().getWwd().redraw();
    //    }
    //
    //    // Don't show any tool tips if the editor controller is engaged in some action.
    //    if (this.getModel().getEditorController().isActive())
    //        return;
    //
    //    if (e.hasObjects())
    //    {
    //        toolTip = this.createToolTip(e.getTopObject(), e.getPickPoint());
    //        this.getApp().setWorldWindToolTip(toolTip);
    //        this.getApp().getWwd().redraw();
    //    }
    //}

    protected void handlePlEnableEdit(boolean enable)
    {
        if (this.getSelectedPlEntry() == null)
            return;

        if (this.isPlSelectionEditing() != enable)
            this.setPlSelectionEditing(enable);
    }

    protected void handlePgEnableEdit(boolean enable)
    {
        if (this.getSelectedPgEntry() == null)
            return;

        if (this.isPgSelectionEditing() != enable)
            this.setPgSelectionEditing(enable);
    }
    protected void handlePeEnableEdit(boolean enable)
    {
        if (this.getSelectedPeEntry() == null)
            return;

        if (this.isPeSelectionEditing() != enable)
            this.setPeSelectionEditing(enable);
    }
    protected void handlePpEnableEdit(boolean enable)
    {
        if (this.getSelectedPpEntry() == null)
            return;

        if (this.isPpSelectionEditing() != enable)
            this.setPpSelectionEditing(enable);
    }


   
    
    
    
    
    protected Vec4 getSurfacePoint(LatLon latlon, double elevation)
    {
        Vec4 point = null;

        SceneController sc = this.getApp().getWwd().getSceneController();
        Globe globe = this.getApp().getWwd().getModel().getGlobe();

        if (sc.getTerrain() != null)
        {
            point = sc.getTerrain().getSurfacePoint(
                latlon.getLatitude(), latlon.getLongitude(), elevation * sc.getVerticalExaggeration());
        }

        if (point == null)
        {
            double e = globe.getElevation(latlon.getLatitude(), latlon.getLongitude());
            point = globe.computePointFromPosition(
                latlon.getLatitude(), latlon.getLongitude(), (e + elevation) * sc.getVerticalExaggeration());
        }

        return point;
    }

    protected Vec4 getPoint(LatLon latlon, double elevation)
    {
        SceneController sc = this.getApp().getWwd().getSceneController();            
        Globe globe = this.getApp().getWwd().getModel().getGlobe();
        double e = globe.getElevation(latlon.getLatitude(), latlon.getLongitude());
        return globe.computePointFromPosition(
            latlon.getLatitude(), latlon.getLongitude(), (e + elevation) * sc.getVerticalExaggeration());
    }

   /* 
    public void createNewPlEntry(PolylineFactory factory)
    {
    	CityPolyline cityPolyline = factory.createPolyline(this.getApp().getWwd(), this.isResizeNewShapesToViewport());
       
    	cityPolyline.setClosed(false);
    	cityPolyline.setDrawInterior=false;
    	
    	PolylineEditor editor = factory.createEditor(cityPolyline);
        PolylineEntry entry = new PolylineEntry(cityPolyline, editor);
        
        
        this.addPlEntry(entry);
        this.selectPlEntry(entry, true);
    }
    */
    
    /*
    public void createNewPgEntry(PolygonFactory factory)
    {
    	CityPolygon cityPolygon = factory.createPolygon(this.getApp().getWwd(), this.isResizeNewShapesToViewport());
        CityPolygonEditor editor = factory.createEditor(cityPolygon);
        PolygonEntry entry = new PolygonEntry(cityPolygon, editor);
        this.addPgEntry(entry);
        this.selectPgEntry(entry, true);
    }
    
    */
    
    public void createNewPeEntry(PolylineFactory factory)
    {   
    	
    	CityPolyline cityPolyline = factory.createPolyline(this.getApp().getWwd(), this.isResizeNewShapesToViewport());
        cityPolyline.setClosed(true);;
        ShapeAttributes attr;
        attr=cityPolyline.getAttributes();
        attr.setInteriorMaterial(Material.RED);
        attr.setInteriorOpacity(0.8);
        
        cityPolyline.setAttributes(attr);
    	
        
        PolylineEditor editor = factory.createEditor(cityPolyline);
        PolylineEntry entry = new PolylineEntry(cityPolyline, editor);
        
        this.addPeEntry(entry);
        this.selectPeEntry(entry, true);

    } 
    
    public void createNewPpEntry(ActivityFactory factory)
    {   
    	
    	ActivityMarker marker = factory.createMarker(this.getApp().getWwd(), this.isResizeNewShapesToViewport());
        ActivityMarkerEditor editor = factory.createEditor(marker);
        MarkerEntry entry = new MarkerEntry(marker, editor);
        
        this.addPpEntry(entry);
        this.selectPpEntry(entry, true);

    } 
    
    
    
    

    public void removePlEntries(Iterable<? extends PolylineEntry> entries)
    {
        if (entries != null)
        {
            for (PolylineEntry entry : entries)
            {
                this.removePlEntry(entry);
            }
        }
    }
    public void removePgEntries(Iterable<? extends PolygonEntry> entries)
    {
        if (entries != null)
        {
            for (PolygonEntry entry : entries)
            {
                this.removeEntry(entry);
            }
        }
    }
    public void removePeEntries(Iterable<? extends PolylineEntry> entries)
    {
        if (entries != null)
        {
            for (PolylineEntry entry : entries)
            {
                this.removePeEntry(entry);
            }
        }
    }
    public void removePpEntries(Iterable<? extends MarkerEntry> entries)
    {
        
    	if (entries != null)
        {
            for (MarkerEntry entry : entries)
            {
                this.removeEntry(entry);
            }
        }
    }

    public void addPlEntry(PolylineEntry entry)
    {
        entry.getEditor().addEditListener(this);
        this.getPlModel().addEntry(entry);
        this.getApp().getSurfaceShapeLayer().addRenderable(entry.getPolyline());
        this.getApp().getWwd().redraw();
    }
    public void addPgEntry(PolygonEntry entry)
    {
        entry.getEditor().addEditListener(this);
        entry.setValue("cityPolygon.length","hello world");
        this.getPgModel().addEntry(entry);
        this.getApp().getAirspaceLayer().addAirspace(entry.getPolygon());
        this.getApp().getWwd().redraw();
    }
    public void addPeEntry(PolylineEntry entry)
    {
        entry.getEditor().addEditListener(this);
        entry.setValue("cityPolyline.length","hello world");
        this.getPeModel().addEntry(entry);
        this.getApp().getPeShapeLayer().addRenderable(entry.getPolyline());
        this.getApp().getWwd().redraw();
    }  
    
    
    public void addPpEntry(MarkerEntry entry)
    {
        entry.getEditor().addEditListener(this);
        
        entry.setValue("cityPolyline.length","hello world");
        this.getPpModel().addEntry(entry);
        this.getApp().getMarkerLayer().addMarker(entry.getMarker());
        this.getApp().getWwd().redraw();
    }  
    
    

    public void removePlEntry(PolylineEntry entry)
    {
        entry.getEditor().removeEditListener(this);

        if (this.getSelectedPlEntry() == entry)
        {
            this.selectPlEntry(null, true);
            this.getStreetView().setSelectedIndices(new int[0]);
        }

        this.getPlModel().removeEntry(entry);
        this.getApp().getSurfaceShapeLayer().removeRenderable(entry.getPolyline());
        this.getApp().getWwd().redraw();
    }
    
    public void removePeEntry(PolylineEntry entry)
    {
        entry.getEditor().removeEditListener(this);

        if (this.getSelectedPeEntry() == entry)
        {
            this.selectPeEntry(null, true);
            this.getEnvironmentView().setSelectedIndices(new int[0]);
        }

        this.getPeModel().removeEntry(entry);
        this.getApp().getPeShapeLayer().removeRenderable(entry.getPolyline());
        this.getApp().getWwd().redraw();
    }
    
    
    public void removeEntry(PolygonEntry entry)
    {
        entry.getEditor().removeEditListener(this);

        if (this.getSelectedPgEntry() == entry)
        {
            this.selectPgEntry(null, true);
        	this.getArchitectureView().setSelectedIndices(new int[0]);
            
        }

        this.getPgModel().removeEntry(entry);
        this.getApp().getAirspaceLayer().removeAirspace(entry.getPolygon());
        this.getApp().getWwd().redraw();
    }
    

    public void removeEntry(MarkerEntry entry)
    {
    	entry.getEditor().removeEditListener(this);

        if (this.getSelectedPpEntry() == entry)
        {
            this.selectPpEntry(null, true);
        	this.getActivityView().setSelectedIndices(new int[0]);
            
        }

        this.getPpModel().removeEntry(entry);
        this.getApp().getMarkerLayer().removeMarker(entry.getMarker());
        this.getApp().getWwd().redraw();
    }
    
    
    
    

    public PolylineEntry getSelectedPlEntry()
    {
        return this.selectedPlEntry;
    }
    
    public PolygonEntry getSelectedPgEntry()
    {
        return this.selectedPgEntry;
    }
    public PolylineEntry getSelectedPeEntry()
    {
        return this.selectedPeEntry;
    }
    public MarkerEntry getSelectedPpEntry()
    {
        return this.selectedPpEntry;
    }
    
    
    
    
    
    
    

    
    public void selectPlEntry(PolylineEntry entry, boolean updateView)
    {
        this.setPlSelectedEntry(entry);

        if (updateView)
        {
            if (entry != null)
            {
                int index = this.getPlModel().getIndexForEntry(entry);
                this.getStreetView().setSelectedIndices(new int[] {index});
            }
            else
            {
                this.getStreetView().setSelectedIndices(new int[0]);
            }
        }

        if (this.isPlEnableEdit())
        {
            if (this.getSelectedPlEntry() != null && !this.isPlSelectionEditing())
            {
                this.setPlSelectionEditing(true);
            }
        }
        this.getApp().getWwd().redraw();
    }
    
    
    public void selectPgEntry(PolygonEntry entry, boolean updateView)
    {   
        this.setPgSelectedEntry(entry);
        
        if (updateView)
        {
            if (entry != null)
            {   
                int index = this.getPgModel().getIndexForEntry(entry);
                this.getArchitectureView().setSelectedIndices(new int[] {index});
            }
            else
            {
                this.getArchitectureView().setSelectedIndices(new int[0]);
            }
        }
        
        
        if (this.isPgEnableEdit())
        {
            if (this.getSelectedPgEntry() != null && !this.isPgSelectionEditing())
            {    
                this.setPgSelectionEditing(true);
            }
        }
        this.getApp().getWwd().redraw();
      
    
    }
    
    public void selectPeEntry(PolylineEntry entry, boolean updateView)
    { 
        this.setPeSelectedEntry(entry);
        
        if (updateView)
        { 
            if (entry != null)
            {   
                int index = this.getPeModel().getIndexForEntry(entry);
                
                
                this.getEnvironmentView().setSelectedIndices(new int[] {index});
               
            }
            else
            {
                this.getEnvironmentView().setSelectedIndices(new int[0]);
            }
        }
        
        if (this.isPeEnableEdit())
        {  
            if (this.getSelectedPeEntry() != null && !this.isPeSelectionEditing())
            {
                this.setPeSelectionEditing(true);
            }
        }
        
        this.getApp().getWwd().redraw();
    }
    

    public void selectPpEntry(MarkerEntry entry, boolean updateView)
    {
    	 this.setPpSelectedEntry(entry);
         
         if (updateView)
         { 
             if (entry != null)
             {   
                 int index = this.getPpModel().getIndexForEntry(entry);
                 
                 
                 this.getActivityView().setSelectedIndices(new int[] {index});
                
             }
             else
             {
                 this.getActivityView().setSelectedIndices(new int[0]);
             }
         }
         
         if (this.isPpEnableEdit())
         {  
             if (this.getSelectedPpEntry() != null && !this.isPpSelectionEditing())
             {
                 this.setPpSelectionEditing(true);
             }
         }
         
         this.getApp().getWwd().redraw();

    }
    
    
    

    protected void setPlSelectedEntry(PolylineEntry entry)
    {
        if (this.selectedPlEntry != null)
        {
            if (this.selectedPlEntry != entry && this.selectedPlEntry.isEditing())
            {
                this.setPlSelectionEditing(false);
            }

            this.selectedPlEntry.setSelected(false);
            
        }

        this.selectedPlEntry = entry;

        if (this.selectedPlEntry != null)
        {
            this.selectedPlEntry.setSelected(true);
            this.getStreetView().cb2.setSelectedItem(entry.getValue("cityPline.color").toString().trim());
            this.getStreetView().factoryComboBox.setSelectedItem(entry.getValue("cityPline.type").toString().trim());    
            this.getStreetView().sp.setValue(entry.getValue("cityPline.width").toString().trim());
            
            
        }
    
       
        
    }
    
    
    protected void setPgSelectedEntry(PolygonEntry entry)
    {  
    	
        if (this.selectedPgEntry != null)
        {
            if (this.selectedPgEntry != entry && this.selectedPgEntry.isEditing())
            {
                this.setPgSelectionEditing(false);
            }

            this.selectedPgEntry.setSelected(false);
        }

        this.selectedPgEntry = entry;

        if (this.selectedPgEntry != null)
        {
            this.selectedPgEntry.setSelected(true);
        }
    }
    
    protected void setPeSelectedEntry(PolylineEntry entry)
    {   
        if (this.selectedPeEntry != null)
        {
            if (this.selectedPeEntry != entry && this.selectedPeEntry.isEditing())
            {
                this.setPeSelectionEditing(false);
            }

            this.selectedPeEntry.setSelected(false);
        }
        
        this.selectedPeEntry = entry;

        if (this.selectedPeEntry != null)
        {
            this.selectedPeEntry.setSelected(true);
        }
       
    }
    
    protected void setPpSelectedEntry(MarkerEntry entry)
    {
        if (this.selectedPpEntry != null)
        {
            if (this.selectedPpEntry != entry && this.selectedPpEntry.isEditing())
            {
                this.setPpSelectionEditing(false);
            }

            this.selectedPpEntry.setSelected(false);
        }

        this.selectedPpEntry = entry;

        if (this.selectedPpEntry != null)
        {
            this.selectedPpEntry.setSelected(true);
        }
    
    }
    
    
    
    

    protected boolean isPlSelectionEditing()
    {
        return this.selectedPlEntry != null && this.selectedPlEntry.isEditing();
    }
    protected boolean isPgSelectionEditing()
    {
        return this.selectedPgEntry != null && this.selectedPgEntry.isEditing();
    }
    protected boolean isPeSelectionEditing()
    {
        return this.selectedPeEntry != null && this.selectedPeEntry.isEditing();
    }
    protected boolean isPpSelectionEditing()
    {
        return this.selectedPpEntry != null && this.selectedPpEntry.isEditing();
    }
    

    
    protected void setPlSelectionEditing(boolean editing)
    {
        if (this.selectedPlEntry == null)
        {
            throw new IllegalStateException();
        }

        
        if (this.selectedPlEntry.isEditing() == editing)
        {
            throw new IllegalStateException();
        }

        this.selectedPlEntry.setEditing(editing);

        PolylineEditor editor = this.selectedPlEntry.getEditor();
        editor.setArmed(editing);

        if (editing)
        {
            this.plEditorController.setEditor(editor);
            CityBuilder.insertBeforePlacenames(this.getApp().getWwd(), editor);
        }
        else
        {
            this.plEditorController.setEditor(null);
            this.getApp().getWwd().getModel().getLayers().remove(editor);
        }

        int index = this.getPlModel().getIndexForEntry(this.selectedPlEntry);
        this.getPlModel().fireTableRowsUpdated(index, index);
    
 }
    
    
    protected void setPgSelectionEditing(boolean editing)
    { 
        if (this.selectedPgEntry == null)
        {
            throw new IllegalStateException();
        }

       
        if (this.selectedPgEntry.isEditing() == editing)
        {
            throw new IllegalStateException();
        }
        
        this.selectedPgEntry.setEditing(editing);

        CityPolygonEditor editor = this.selectedPgEntry.getEditor();
        editor.setArmed(editing);
       
        if (editing)
        {
            this.pgEditorController.setEditor(editor);
            CityBuilder.insertBeforePlacenames(this.getApp().getWwd(), editor);
        }
        else
        {
            this.pgEditorController.setEditor(null);
            this.getApp().getWwd().getModel().getLayers().remove(editor);
        }
        int index = this.getPgModel().getIndexForEntry(this.selectedPgEntry);
        this.getPgModel().fireTableRowsUpdated(index, index);
    
 }
    protected void setPeSelectionEditing(boolean editing)
    {
        if (this.selectedPeEntry == null)
        {
            throw new IllegalStateException();
        }

        
        if (this.selectedPeEntry.isEditing() == editing)
        {
            throw new IllegalStateException();
        }

        this.selectedPeEntry.setEditing(editing);

        PolylineEditor editor = this.selectedPeEntry.getEditor();
        editor.setArmed(editing);

        if (editing)
        {
            this.peEditorController.setEditor(editor);
            CityBuilder.insertBeforePlacenames(this.getApp().getWwd(), editor);
        }
        else
        {
            this.peEditorController.setEditor(null);
            this.getApp().getWwd().getModel().getLayers().remove(editor);
        }

        int index = this.getPeModel().getIndexForEntry(this.selectedPeEntry);
        this.getPeModel().fireTableRowsUpdated(index, index);
    
 }
    

    protected void setPpSelectionEditing(boolean editing)
    {
    	
        if (this.selectedPpEntry == null)
        {
            throw new IllegalStateException();
        }

        
        if (this.selectedPpEntry.isEditing() == editing)
        {
            throw new IllegalStateException();
        }

        this.selectedPpEntry.setEditing(editing);
       
        ActivityMarkerEditor editor = this.selectedPpEntry.getEditor();
        editor.setArmed(editing);

        if (editing)
        {    
        
            this.ppEditorController.setEditor(editor);
            CityBuilder.insertBeforePlacenames(this.getApp().getWwd(), editor);
        }
        else
        {
            this.ppEditorController.setEditor(null);
            this.getApp().getWwd().getModel().getLayers().remove(editor);
        }

        int index = this.getPpModel().getIndexForEntry(this.selectedPpEntry);
        this.getPpModel().fireTableRowsUpdated(index, index);
    	
    	
    }    
    
    
    
    

    protected void viewSelectionPlChanged()
    {
        int[] indices = this.getStreetView().getSelectedIndices();
        
        
        if (indices != null)
        {
            for (PolylineEntry entry : this.getPlEntriesFor(indices))
            {
                this.selectPlEntry(entry, false);
               
            }
        }
        
        
        this.getApp().getWwd().redraw();
    }
    protected void viewSelectionPgChanged()
    {
        int[] indices = this.getArchitectureView().getSelectedIndices();
        if (indices != null)
        {
            for (PolygonEntry entry : this.getPgEntriesFor(indices))
            {
                this.selectPgEntry(entry, false);
               
            }
        }
  
        
        this.getApp().getWwd().redraw();
    }
    protected void viewSelectionPeChanged()
    {
        int[] indices = this.getEnvironmentView().getSelectedIndices();
        if (indices != null)
        {
            for (PolylineEntry entry : this.getPeEntriesFor(indices))
            {
                this.selectPeEntry(entry, false);
            }
        }
        
        this.getApp().getWwd().redraw();
    } 
    
 
    protected void viewSelectionPpChanged()
    {
    	 int[] indices = this.getActivityView().getSelectedIndices();
         if (indices != null)
         {
             for (MarkerEntry entry : this.getPpEntriesFor(indices))
             {
                 this.selectPpEntry(entry, false);
             }
         }
         
         this.getApp().getWwd().redraw();
    }   
    
    
    

    protected PolylineEntry[] getPlSelectedEntries()
    {
        int[] indices = this.getStreetView().getSelectedIndices();
        if (indices != null)
        {
            return this.getPlEntriesFor(indices);
        }

        return new PolylineEntry[0];
    }
    
    protected PolygonEntry[] getPgSelectedEntries()
    {
        int[] indices = this.getArchitectureView().getSelectedIndices();
        if (indices != null)
        {
            return this.getPgEntriesFor(indices);
        }
        
        return new PolygonEntry[0];
    } 
    

    protected PolylineEntry[] getPeSelectedEntries()
    {
        int[] indices = this.getEnvironmentView().getSelectedIndices();
     
        if (indices != null)
        {
            return this.getPeEntriesFor(indices);
        }

        return new PolylineEntry[0];
    }
    protected MarkerEntry[] getPpSelectedEntries()
    {
        int[] indices = this.getActivityView().getSelectedIndices();
     
        if (indices != null)
        {   
            return this.getPpEntriesFor(indices);
        }

        return new MarkerEntry[0];
    }
    
    
    
    

    protected PolylineEntry[] getPlEntriesFor(int[] indices)
    {
        PolylineEntry[] entries = new PolylineEntry[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            entries[i] = this.getPlModel().getEntry(indices[i]);
        }
        return entries;
    }
    
    protected PolygonEntry[] getPgEntriesFor(int[] indices)
    {
        PolygonEntry[] entries = new PolygonEntry[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            entries[i] = this.getPgModel().getEntry(indices[i]);
        }
        return entries;
    } 
    
    protected PolylineEntry[] getPeEntriesFor(int[] indices)
    {
        PolylineEntry[] entries = new PolylineEntry[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            entries[i] = this.getPeModel().getEntry(indices[i]);
        }
        return entries;
    } 
    
    
    

    
    protected MarkerEntry[] getPpEntriesFor(int[] indices)
    {
    	MarkerEntry[] entries = new MarkerEntry[indices.length];
        for (int i = 0; i < indices.length; i++)
        {  
           entries[i] = this.getPpModel().getEntry(indices[i]);
        }
        return entries;
    
    
    } 
    
    
    
    
    
    protected PolylineEntry getEntryFor(CityPolyline polyline)
    {   
    	if(polyline.isClosed()){
        for (PolylineEntry entry : this.getPeModel().getEntries())
        {
            if (entry.getPolyline() == polyline)
            {
                return entry;
            }
        }
    	}
    	else {
    		for (PolylineEntry entry : this.getPlModel().getEntries())
            {
                if (entry.getPolyline() == polyline)
                {
                    return entry;
                }
            }	
    		
    		
    	}
    	
    	
        return null;
    }
    
    
    
    protected PolygonEntry getEntryFor(CityPolygon polygon)
    {
        for (PolygonEntry entry : this.getPgModel().getEntries())
        {
            if (entry.getPolygon() == polygon)
            {
                return entry;
            }
        }
        return null;
    }
    
    protected MarkerEntry getEntryFor(ActivityMarker marker)
    {
        for (MarkerEntry entry : this.getPpModel().getEntries())
        {
            if (entry.getMarker() == marker)
            {
                return entry;
            }
        }
        return null;
    }

    //protected ToolTip createToolTip(Object object, Point pickPoint)
    //{
    //    AirspaceEntry pickedEntry = null;
    //
    //    if (object instanceof Airspace)
    //    {
    //        pickedEntry = this.getEntryFor((Airspace) object);
    //    }
    //    else if (object instanceof AirspaceControlPoint)
    //    {
    //        pickedEntry = this.getEntryFor(((AirspaceControlPoint) object).getAirspace());
    //    }
    //
    //    if (pickedEntry == null)
    //    {
    //        return null;
    //    }
    //
    //    StringBuilder sb = new StringBuilder();
    //    sb.append("<html>");
    //
    //    if (object instanceof Airspace)
    //    {
    //        if (pickedEntry.toString() != null)
    //        {
    //            sb.append(pickedEntry.toString());
    //        }
    //
    //        if (pickedEntry.isEditing())
    //        {
    //            if (sb.length() > 0)
    //            {
    //                sb.append("<br/>");
    //            }
    //
    //            sb.append("<b>Click & Drag to move</b>");
    //            sb.append("<br/>");
    //            sb.append("<b>Shift + Click & Drag to raise/lower</b>");
    //
    //            if (pickedEntry.getAirspace() instanceof Polygon)
    //            {
    //                sb.append("<br/>");
    //                sb.append("<b>Alt + Click to add points</b>");
    //            }
    //        }
    //    }
    //    else //noinspection ConstantConditions
    //        if (object instanceof AirspaceControlPoint)
    //    {
    //        sb.append("<b>Click & Drag to adjust</b>");
    //        sb.append("<br/>");
    //        sb.append("<b>Shift + Click & Drag to resize</b>");
    //        if (pickedEntry.getAirspace() instanceof Polygon)
    //        {
    //            sb.append("<br/>");
    //            sb.append("<b>Ctrl + Click to delete</b>");
    //        }
    //    }
    //
    //    sb.append("</html>");
    //
    //    int canvasHeight = this.getApp().getWwd().getHeight();
    //    Point wwPoint = new Point(pickPoint.x, canvasHeight - pickPoint.y - 1);
    //
    //    return new ToolTip(sb.toString(), wwPoint, 1.0);
    //}

    protected void zoomTo(LatLon latLon, Angle heading, Angle pitch, double zoom)
    {
        BasicOrbitView view = (BasicOrbitView) this.getApp().getWwd().getView();
        view.addPanToAnimator(
            new Position(latLon, 0), heading, pitch, zoom, true);

        view.stopMovement();
    }

    protected void openFromURL() {
    	 final ArrayList<CityPolyline> polylines = new ArrayList<CityPolyline>();
         java.sql.Connection conn; 
         try { 
    	    /* 
    	    * Load the JDBC driver and establish a connection. 
    	    */
    	    Class.forName("org.postgresql.Driver"); 
    	    String url = "jdbc:postgresql://localhost:5432/beijing"; 
    	    conn = DriverManager.getConnection(url, "postgres", "004017"); 
    	    /* 
    	    * Add the geometry types to the connection. Note that you 
    	    * must cast the connection to the pgsql-specific connection 
    	    * implementation before calling the addDataType() method. 
    	    */
    	    ((org.postgresql.PGConnection)conn).addDataType("geometry",PGgeometry.class);
    	    ((org.postgresql.PGConnection)conn).addDataType("box3d",PGbox3d.class);
    	    /* 
    	    * Create a statement and execute a select query. 
    	    */ 
    	    Statement s = conn.createStatement(); 
    	    ResultSet r = s.executeQuery("select the_geom from local_path"); 
    	    
    	    //ResultSet r = s.executeQuery("select ST_AsText(geom) as geom,id from gtest"); 
    	    int ii=0;
    	    while( r.next() ) { 
    	      /* 
    	      * Retrieve the geometry as an object then cast it to the geometry type. 
    	      * Print things out. 
    	      */ 
    	        ii++;
    	        ArrayList<LatLon> positions= new ArrayList<LatLon>();
    	    	PGgeometry geom = (PGgeometry)r.getObject(1); 
    	        if(geom.getGeometry().getType()== Geometry.MULTILINESTRING) { 
    	    	MultiLineString pl = (MultiLineString)geom.getGeometry(); 
    	    	
    	    	LatLon latlon;
    	    	
    	    	for( int i = 0; i < pl.numPoints(); i++) { 
    	    	    Point point = pl.getPoint(i); 
    	    	    latlon= LatLon.fromDegrees(point.y, point.x);
    	    	    positions.add(latlon);
    	    	    
    	    	    
    	            } 
    	    	}
                       	        
    	        CityPolyline polyline =new CityPolyline(positions);
                polylines.add(polyline);
                polyline.setValue(AVKey.DISPLAY_NAME, ii);
                ShapeAttributes attributes = new BasicShapeAttributes();
                attributes.setOutlineMaterial(Material.RED);
                attributes.setDrawOutline(true);
                attributes.setOutlineOpacity(0.8);
                attributes.setOutlineWidth(2);
                polyline.setAttributes(attributes);
          
    	    } 
    	     s.close(); 
    	    conn.close(); 
    	  } 
    	 catch( Exception e ) { 
    	  e.printStackTrace(); 
    	  } 
          setPolylines(polylines);
          setEnabled(true);
          getApp().setCursor(null);
          getApp().getWwd().redraw();
    
    }

    protected void openFromURL(final URL url){}

    protected void loadPolylinesFromURL(URL url, Collection<Airspace> airspaces)
    {
    }

    protected void openFromFile()
    {
        if (this.fileChooser == null)
        {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setCurrentDirectory(new File(Configuration.getUserHomeDirectory()));
        }

        this.fileChooser.setDialogTitle("Choose Polyline File Directory");
        this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.fileChooser.setMultiSelectionEnabled(false);
        int status = this.fileChooser.showOpenDialog(null);
        if (status != JFileChooser.APPROVE_OPTION)
            return;

        final File dir = this.fileChooser.getSelectedFile();
        if (dir == null)
            return;

        Thread t = new Thread(new Runnable()
        {  
        	 
            public void run()
            {   
                final ArrayList<CityPolyline> polylines = new ArrayList<CityPolyline>();
                try
                {   
                    File[] files = dir.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.startsWith("gov.nasa.worldwind.render.city.CityPolyline") && name.endsWith(".xml");
                        }
                    });
                    
                    System.out.println(files.length);
                    for (File file : files)
                    {
                        String[] name = file.getName().split("-");
                        try
                        {   
                            Class c = Class.forName(name[0]);
                            CityPolyline polyline = (CityPolyline) c.newInstance();
                            BufferedReader input = new BufferedReader(new FileReader(file));
                            String s = input.readLine();
                            polyline.restoreState(s);
                            polylines.add(polyline);

                            if (name.length >= 2)
                            {
                            	polyline.setValue(AVKey.DISPLAY_NAME, name[1]);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                finally
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            setPolylines(polylines);
                            setEnabled(true);
                            getApp().setCursor(null);
                            getApp().getWwd().redraw();
                        }
                    });
                }
            }
        });
        this.setEnabled(false);
        getApp().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        t.start();
    }

    protected void saveToFile()
    {
        if (this.fileChooser == null)
        {
            this.fileChooser = new JFileChooser();
            this.fileChooser.setCurrentDirectory(new File(Configuration.getUserHomeDirectory()));
        }

        this.fileChooser.setDialogTitle("Choose Directory to Place Polylines");
        this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.fileChooser.setMultiSelectionEnabled(false);
        int status = this.fileChooser.showSaveDialog(null);
        if (status != JFileChooser.APPROVE_OPTION)
            return;

        final File dir = this.fileChooser.getSelectedFile();
        if (dir == null)
            return;

        if (dir.exists())
        {
            try
            {
                WWIO.deleteDirectory(dir);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        if (!dir.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        final Iterable<PolylineEntry> entries = this.getPlModel().getEntries();

        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    java.text.DecimalFormat f = new java.text.DecimalFormat("####");
                    f.setMinimumIntegerDigits(4);
                    int counter = 0;

                    for (PolylineEntry entry : entries)
                    {
                    	CityPolyline a = entry.getPolyline();
                                           
                        String xmlString = a.getRestorableState();
                        if (xmlString != null)
                        {
                            try
                            {
                                PrintWriter of = new PrintWriter(new File(dir,
                                    a.getClass().getName() + "-" + entry.getName() + "-" + f.format(counter++) + ".xml"));
                                of.write(xmlString);
                                of.flush();
                                of.close();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                finally
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            setEnabled(true);
                            getApp().setCursor(null);
                            getApp().getWwd().redraw();
                        }
                    });
                }
            }
        });
        this.setEnabled(false);
        getApp().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        t.start();
    }

    public void setPolylines(Iterable<? extends CityPolyline> polylines)
    {
        ArrayList<PolylineEntry> entryList = new ArrayList<PolylineEntry>(this.getPlModel().getEntries());
        this.removePlEntries(entryList);

        for (CityPolyline polyline : polylines)
        {
            PolylineEntry entry = new PolylineEntry(polyline, CityBuilder.getEditorFor(polyline));
            this.addPlEntry(entry);
        }
    }
    
    public void setPolygons(Iterable<? extends CityPolygon> polygons)
    {
        ArrayList<PolygonEntry> entryList = new ArrayList<PolygonEntry>(this.getPgModel().getEntries());
        this.removePgEntries(entryList);

        for (CityPolygon polygon : polygons)
        {
            PolygonEntry entry = new PolygonEntry(polygon, CityBuilder.getEditorFor(polygon));
            this.addPgEntry(entry);
        }
    }
    

    private String getFileName(String s)
    {
        int index = s.lastIndexOf("/");
        if (index == -1)
            index = s.lastIndexOf("\\");
        if (index != -1 && index < s.length())
            return s.substring(index + 1, s.length());
        return s;
    }

	@Override
	public void SurfaceMoved(SurfaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void SurfaceResized(SurfaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void airspaceMoved(AirspaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void airspaceResized(AirspaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointAdded(AirspaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointChanged(AirspaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointRemoved(AirspaceEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void MarkerMoved(MarkerEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void MarkerResized(MarkerEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointAdded(MarkerEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointChanged(MarkerEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void controlPointRemoved(MarkerEditEvent e) {
		// TODO Auto-generated method stub
		
	}

	
}