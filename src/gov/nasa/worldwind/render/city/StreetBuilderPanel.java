package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.city.CityBuilder.AppFrame;
import gov.nasa.worldwind.render.city.CityBuilder.AppFrame.ControlerAction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.border.CompoundBorder;

     //create by Haidong Ma
	//**************************************************************//
	//******************** Street Builder Panel  ****************//
	//**************************************************************//
public class StreetBuilderPanel  extends JPanel{
        public  JComboBox factoryComboBox;
	    public   JTable entryTable;
	    public   JSpinner sp;
	    private  PolylineBuilderModel model;
	    private  CityBuilderController controller;
	    private  StreetBuildController streetController;
	    public   JComboBox cb2;
	    
	    TextArea textArea_sql;
	    
	    public   ControlerAction resizeAction ;
	    public   ImageIcon iconResize;
	    public   ImageIcon iconResize_g;
	    public   ControlerAction modifyAction ;
	    public   ImageIcon iconModify;
	    public   ImageIcon iconModify_g;
	    
	    private boolean ignoreSelectEvents = false;
	    
	    public StreetBuilderPanel(PolylineBuilderModel model, CityBuilderController controller, StreetBuildController streetController)
	    {   this.model=model;
	        this.controller=controller;
	        this.streetController=streetController;
	        this.initComponents(model, controller);
	  
	    }
	    
	public int[] getSelectedIndices() {
		return this.entryTable.getSelectedRows();
	}

	public void setSelectedIndices(int[] indices) {
		this.ignoreSelectEvents = true;

		if (indices != null && indices.length != 0) {
			for (int index : indices) {
				this.entryTable.setRowSelectionInterval(index, index);
			}
		} else {
			this.entryTable.clearSelection();
		}

		this.ignoreSelectEvents = false;
	}

	public String getSelectedFactory() {
		return (String) this.factoryComboBox.getSelectedItem();
	}

	public void setSelectedFactory(PolylineFactory factory) {
		this.factoryComboBox.setSelectedItem(factory);
	}

	private void initComponents(PolylineBuilderModel model, final CityBuilderController controller) {

		////////////////////////////////////////////////////////////////////////////////////
		//setup shape attributes of street elements 
		JPanel ShapePanel = new JPanel();
		TitledBorder b = BorderFactory.createTitledBorder("Street Attributes");
        ShapePanel.setBorder(new CompoundBorder(b, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		{

			JPanel outerPanel = new JPanel();
			
			GridLayout nameLayout = new GridLayout(0, 1, 6, 6);
			JPanel namePanel = new JPanel(nameLayout);
			
            GridLayout valueLayout = new GridLayout(0, 1, 6, 6);
			JPanel valuePanel = new JPanel(valueLayout);

			namePanel.add(new JLabel("Width"));
			sp = new JSpinner(new SpinnerListModel(new String[] { "1.0",
					"1.25", "1.5", "2.0", "3.0", "4.0", "5.0", "10.0" }));
			sp.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent changeEvent) {
					if(sp.isFocusable()&& (controller.getSelectedPlEntry() != null) ){
					ShapeAttributes attributes;
					attributes = controller.getSelectedPlEntry()
							.getAttributes();
					attributes.setOutlineWidth(Float
							.parseFloat((String) ((JSpinner) changeEvent
									.getSource()).getValue()));
					controller.getSelectedPlEntry().getPolyline()
							.setAttributes(attributes);
					controller.getApp().getWwd().redraw();
					controller.getSelectedPlEntry().getPolyline().setValue("cityPline.width", ((String) ((JSpinner) changeEvent
							.getSource()).getValue()).trim());	
					String string;
					PolylineEntry plineEntry;
					plineEntry = controller.getSelectedPlEntry();
					int index_geom;
					if (plineEntry != null) {
						index_geom = controller.getPlModel().getIndexForEntry(
								plineEntry);
						String gid = controller.getPlModel().getValueAt(
								index_geom, 0).toString();
						if (gid != null) {
							string = "UPDATE local_path SET width= '" + ((String) ((JSpinner) changeEvent
									.getSource()).getValue()).trim()
									+ "'  WHERE gid = '" + gid + "';";
							
							try {
								Statement s = controller.getApp().conn.createStatement();
								s.executeUpdate(string);
								s.close();
							} catch (Exception ee) {
								ee.printStackTrace();
							}
						}
					}
					}
					
					
					
					
				}
			});
			valuePanel.add(sp);

			namePanel.add(new JLabel("Color"));
			this.cb2 = new JComboBox(new String[] { "Red", "Green", "Blue", "Yellow" });
			cb2.addActionListener(streetController);
			valuePanel.add(cb2);
			
			namePanel.add(new JLabel("Type"));
			this.factoryComboBox = new JComboBox(new String[] { "L", "M", "S"});
			factoryComboBox.addActionListener(streetController);
			//this.factoryComboBox.setToolTipText("Choose street type to create");
			valuePanel.add(this.factoryComboBox);			

			outerPanel.add(namePanel);
			outerPanel.add(valuePanel);
			ShapePanel.setLayout(new BorderLayout());
			ShapePanel.add(outerPanel, BorderLayout.CENTER);
		}

		
		/////////////////////////////////////////////////////////////////////////////////
		//set the talbe of street database
		JPanel entryPanel = new JPanel();
		{
			this.entryTable = new JTable(model);
			this.entryTable.setColumnSelectionAllowed(false);
			this.entryTable.setRowSelectionAllowed(true);
			this.entryTable
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.entryTable.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) {
							if (!ignoreSelectEvents) {
								controller.actionPerformed(new ActionEvent(e
										.getSource(), -1,
										CityBuilder.SELECTION_CHANGED_PL));
							}
						}
					});
			this.entryTable
					.setToolTipText("<html>Click to select<br>Double-Click to rename</html>");

			JScrollPane tablePane = new JScrollPane(this.entryTable);
			tablePane.setPreferredSize(new Dimension(200, 100));
			
			textArea_sql= new TextArea();			

			entryPanel.setLayout(new BorderLayout(0, 0)); // hgap, vgap
			entryPanel.add(tablePane, BorderLayout.CENTER);
			entryPanel.add(textArea_sql, BorderLayout.SOUTH);

		}

		
		//////////////////////////////////////////////////////////////////////////////////////////
		//tool bars
		JPanel toolbarPanel = new JPanel();
		{

			JToolBar toolBar = new JToolBar("street controler");

			ControlerAction openAction = new ControlerAction("open",
					new ImageIcon("./resources/open.gif "),
					"open street database", 'o', this.streetController);
			toolBar.add(openAction);
			ControlerAction newAction = new ControlerAction("new",
					new ImageIcon("./resources/new.gif "),
					"new street database", 'n', this.streetController);
			toolBar.add(newAction);
			ControlerAction saveAction = new ControlerAction("save",
					new ImageIcon("./resources/save.gif "),
					"save street database", 's', this.streetController);
			toolBar.add(saveAction);
			
			ControlerAction deleteAction = new ControlerAction("delete",
					new ImageIcon("./resources/delete.gif "),
					"delete street database", 'd', this.streetController);
			toolBar.add(deleteAction);
			ControlerAction stopAction = new ControlerAction("stop",
					new ImageIcon("./resources/stop.gif "),
					"stop drawing street database", 's', this.streetController);
			toolBar.add(stopAction);
			ControlerAction sqlAction = new ControlerAction("sql",
					new ImageIcon("./resources/sql.gif "),
					"sql drawing street database", 'q', this.streetController);
			toolBar.add(sqlAction);
			
			toolBar.addSeparator();
			
			iconResize=new ImageIcon("./resources/resize.gif ");
			iconResize_g=new ImageIcon("./resources/resize_g.gif ");
			resizeAction = new ControlerAction("resize",iconResize,
					"resize drawing street database", 'r', this.streetController);
			toolBar.add(resizeAction);
			
			iconModify=new ImageIcon("./resources/modify.gif ");
			iconModify_g=new ImageIcon("./resources/modify_g.gif ");
			modifyAction = new ControlerAction("modify",iconModify,
					"modify drawing street database", 'm', this.streetController);
			toolBar.add(modifyAction);
			
			
			

			toolbarPanel.setLayout(new BorderLayout());
			toolbarPanel.add(toolBar, BorderLayout.SOUTH);
		}

		
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		this.setLayout(new BorderLayout(30, 0)); // hgap, vgap
		this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // top,
																			// left,
																			// bottom,
																			// right
		this.add(ShapePanel, BorderLayout.NORTH);
		this.add(entryPanel, BorderLayout.CENTER);
		this.add(toolbarPanel, BorderLayout.SOUTH);

	}
	    
	    
	    public static class   ControlerAction   extends   AbstractAction   {
            public StreetBuildController streetController;
            public   ControlerAction(String text,Icon icon, String description,char accelerator,StreetBuildController streetController )   {
                super(text,   icon);
                this.streetController=streetController;
                putValue(ACCELERATOR_KEY,   KeyStroke.getKeyStroke(accelerator,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                putValue(SHORT_DESCRIPTION,   description);
            }
            
            public   void   actionPerformed(ActionEvent   e)   {
            	if(getValue(NAME)=="open"){ streetController.openStreetData();} 
            	if(getValue(NAME)=="new"){ streetController.newStreetDate();}
            	if(getValue(NAME)=="save"){ streetController.saveStreetDate();}
            	if(getValue(NAME)=="delete"){ streetController.deleteStreetDate();}
            	if(getValue(NAME)=="stop")  { streetController.deSelectStreetDate();}
            	if(getValue(NAME)=="resize"){streetController.resizeStreetDate();}
            	if(getValue(NAME)=="modify"){streetController.modifyStreetDate();}
            	if(getValue(NAME)=="sql"){streetController.sqlStreetDate();}
            }
        }
	}
