	package gov.nasa.worldwind.render.city;

	import gov.nasa.worldwind.render.Material;
	import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.city.StreetBuilderPanel.ControlerAction;

	import java.awt.BorderLayout;
	import java.awt.Color;
	import java.awt.Component;
	import java.awt.Dimension;
	import java.awt.GridLayout;
import java.awt.Toolkit;
	import java.awt.event.ActionEvent;
	import java.awt.event.ActionListener;
	import java.beans.PropertyChangeEvent;
	import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
	import javax.swing.BorderFactory;
	import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
	import javax.swing.JButton;
	import javax.swing.JCheckBox;
	import javax.swing.JComboBox;
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
	//******************** Architecture Builder Panel  ****************//
	//**************************************************************//
	public class ArchitectureBuilderPanel  extends JPanel{

	        private  JComboBox factoryComboBox;
		    public  JTable entryTable;
		    private  JSpinner sp;
		    private  PolygonBuilderModel model;
		    private  CityBuilderController controller;
		    private  ArchitectureBuildController architectureController;
		    
		    public   ControlerAction resizeAction ;
		    public   ImageIcon iconResize;
		    public   ImageIcon iconResize_g;
		    public   ControlerAction modifyAction ;
		    public   ImageIcon iconModify;
		    public   ImageIcon iconModify_g;
		    
		    private boolean ignoreSelectEvents = false;
		    
		    public ArchitectureBuilderPanel(PolygonBuilderModel model, CityBuilderController controller,  ArchitectureBuildController architectureController)
		    {   this.model=model;
		        this.controller=controller;
		        this.architectureController=architectureController;
		        this.initComponents(model, controller);
		  
		    }
		    
		public int[] getSelectedIndices()
		    {
		        return this.entryTable.getSelectedRows();
		    }

		    public void setSelectedIndices(int[] indices)
		    {
		        this.ignoreSelectEvents = true;

		        if (indices != null && indices.length != 0)
		        {
		            for (int index : indices)
		            {
		                this.entryTable.setRowSelectionInterval(index, index);
		            }
		        }
		        else
		        {
		            this.entryTable.clearSelection();
		        }

		        this.ignoreSelectEvents = false;
		    }

		    public String getSelectedFactory()
		    {  
		        return (String) this.factoryComboBox.getSelectedItem();
		    }

		    public void setSelectedFactory(PolylineFactory factory)
		    {
		        this.factoryComboBox.setSelectedItem(factory);
		    }

		    private void initComponents(PolygonBuilderModel model, final CityBuilderController controller)
		    {

		        JPanel newShapePanel = new JPanel();
		        TitledBorder b = BorderFactory.createTitledBorder("Architecture Attributes");
		        newShapePanel.setBorder(new CompoundBorder(b, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		        {   String type[]={"House","Factory","School"};
		            this.factoryComboBox = new JComboBox(type);
		            this.factoryComboBox.setEditable(false);
		            this.factoryComboBox.setToolTipText("Choose street type to create");

		            

		            Box newShapeBox = Box.createHorizontalBox();
		            newShapeBox.add(Box.createHorizontalStrut(5));
		            newShapeBox.add(this.factoryComboBox);
		            newShapeBox.setAlignmentX(Component.LEFT_ALIGNMENT);

		            JPanel gridPanel = new JPanel(new GridLayout(0, 1, 0, 5)); // rows, cols, hgap, vgap
		            gridPanel.add(newShapeBox);

		            
		            
		            JPanel outerPanel = new JPanel();
			        GridLayout nameLayout = new GridLayout(0, 1, 6, 6);
		            JPanel namePanel = new JPanel(nameLayout);

		            GridLayout valueLayout = new GridLayout(0, 1, 6, 6);
		            JPanel valuePanel = new JPanel(valueLayout);
		            
		            
		            namePanel.add(new JLabel("Width"));
		            sp = new JSpinner(
		                new SpinnerListModel(new String[] {"1.0", "1.25", "1.5", "2.0", "3.0", "4.0", "5.0", "10.0"}));
		            sp.addChangeListener(new ChangeListener()
		            {
		                public void stateChanged(ChangeEvent changeEvent)
		                {   
		                	AirspaceAttributes attributes;
		                	attributes=controller.getSelectedPgEntry().getAttributes();
		                	attributes.setOutlineWidth(Float.parseFloat((String) ((JSpinner) changeEvent.getSource()).getValue()));
		                    controller.getSelectedPgEntry().getPolygon().setAttributes(attributes);
		                    controller.getApp().getWwd().redraw();
		                }
		            });
		            valuePanel.add(sp);

		            namePanel.add(new JLabel("Color"));
		            
		              
		            
		            
		            JComboBox cb2 = new JComboBox(new String[] {"Red", "Green", "Blue", "Yellow"});
		             cb2.addActionListener(new ActionListener()
		            {
		                public void actionPerformed(ActionEvent actionEvent)
		                {     String selectColor=(String) ((JComboBox) actionEvent.getSource()).getSelectedItem();
		                	  Color color = null;
		                      if (selectColor.equals("Yellow"))
		                          color = new Color(1f, 1f, 0f);
		                      else if (selectColor.equals("Red"))
		                          color = new Color(1f, 0f, 0f);
		                      else if (selectColor.equals("Green"))
		                          color = new Color(0f, 1f, 0f);
		                      else if (selectColor.equals("Blue"))
		                          color = new Color(0f, 0f, 1f);
		                      AirspaceAttributes attributes;
			                	attributes=controller.getSelectedPgEntry().getAttributes();
			                	attributes.setOutlineMaterial(new Material(color));
			                    controller.getSelectedPgEntry().getPolygon().setAttributes(attributes);
			                    controller.getApp().getWwd().redraw();
		                     
		                }
		            });
		            valuePanel.add(cb2);

		            outerPanel.add(namePanel);
		            outerPanel.add(valuePanel);
		            
		            
		            
		            
		            
		            
		            newShapePanel.setLayout(new BorderLayout());
		            newShapePanel.add(gridPanel, BorderLayout.SOUTH);
		            newShapePanel.add(outerPanel,BorderLayout.CENTER);
		        }
		        
		      

		        JPanel entryPanel = new JPanel();
		        {
		            this.entryTable = new JTable(model);
		            this.entryTable.setColumnSelectionAllowed(false);
		            this.entryTable.setRowSelectionAllowed(true);
		            this.entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		            this.entryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		            {
		                public void valueChanged(ListSelectionEvent e)
		                {
		                    if (!ignoreSelectEvents)
		                    {
		                        controller.actionPerformed(new ActionEvent(e.getSource(), -1, CityBuilder.SELECTION_CHANGED_PG));
		                    }
		                }
		            });
		            this.entryTable.setToolTipText("<html>Click to select<br>Double-Click to rename</html>");

		            JScrollPane tablePane = new JScrollPane(this.entryTable);
		            tablePane.setPreferredSize(new Dimension(200, 100));

		            entryPanel.setLayout(new BorderLayout(0, 0)); // hgap, vgap
		            entryPanel.add(tablePane, BorderLayout.CENTER);
		        }

		        JPanel selectionPanel = new JPanel();
	            selectionPanel.setLayout(new BorderLayout());
		       
				//////////////////////////////////////////////////////////////////////////////////////////
				//tool bars
				JPanel toolbarPanel = new JPanel();
				{

					JToolBar toolBar = new JToolBar("architecture controler");

					ControlerAction openAction = new ControlerAction("open",
							new ImageIcon("./resources/open.gif "),
							"open street database", 'o', this.architectureController);
					toolBar.add(openAction);
					
					ControlerAction newAction = new ControlerAction("new",
							new ImageIcon("./resources/new.gif "),
							"new street database", 'n', this.architectureController);
					toolBar.add(newAction);
					ControlerAction saveAction = new ControlerAction("save",
							new ImageIcon("./resources/save.gif "),
							"save street database", 's', this.architectureController);
					toolBar.add(saveAction);
					
					ControlerAction deleteAction = new ControlerAction("delete",
							new ImageIcon("./resources/delete.gif "),
							"delete street database", 'd',this.architectureController);
					toolBar.add(deleteAction);
					ControlerAction stopAction = new ControlerAction("stop",
							new ImageIcon("./resources/stop.gif "),
							"stop drawing street database", 's', this.architectureController);
					toolBar.add(stopAction);
					ControlerAction sqlAction = new ControlerAction("sql",
							new ImageIcon("./resources/sql.gif "),
							"sql drawing street database", 'q', this.architectureController);
					toolBar.add(sqlAction);
					
					toolBar.addSeparator();
					
					iconResize=new ImageIcon("./resources/resize.gif ");
					iconResize_g=new ImageIcon("./resources/resize_g.gif ");
					resizeAction = new ControlerAction("resize",iconResize,
							"resize drawing street database", 'r', this.architectureController);
					toolBar.add(resizeAction);
					
					iconModify=new ImageIcon("./resources/modify.gif ");
					iconModify_g=new ImageIcon("./resources/modify_g.gif ");
					modifyAction = new ControlerAction("modify",iconModify,
							"modify drawing street database", 'm', this.architectureController);
					toolBar.add(modifyAction);
					
					
					

					toolbarPanel.setLayout(new BorderLayout());
					toolbarPanel.add(toolBar, BorderLayout.SOUTH);
				}
		        
				selectionPanel.add(toolbarPanel, BorderLayout.SOUTH);
              /////////////////////////////////////////////////////////////////////////////////////////////
		        this.setLayout(new BorderLayout(30, 0)); // hgap, vgap
		        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // top, left, bottom, right
		        this.add(newShapePanel, BorderLayout.NORTH);
		        this.add(entryPanel, BorderLayout.CENTER);
		        this.add(selectionPanel, BorderLayout.SOUTH);
		        //this.add(this.tabbedPane, BorderLayout.NORTH);


		    }
		
		    public static class   ControlerAction   extends   AbstractAction   {
	            public ArchitectureBuildController architectureController;
	            public   ControlerAction(String text,Icon icon, String description,char accelerator,ArchitectureBuildController architectureController )   {
	                super(text,   icon);
	                this.architectureController=architectureController;
	                putValue(ACCELERATOR_KEY,   KeyStroke.getKeyStroke(accelerator,
	                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	                putValue(SHORT_DESCRIPTION,   description);
	            }
	            
	            public   void   actionPerformed(ActionEvent   e)   {
	            	if(getValue(NAME)=="open")  { architectureController.openArchitectureData();} 
	            	if(getValue(NAME)=="new")   { architectureController.newArchitectureDate();}
	            	if(getValue(NAME)=="save")  { architectureController.saveArchitectureDate();}
	            	if(getValue(NAME)=="delete"){ architectureController.deleteArchitectureDate();}
	            	if(getValue(NAME)=="stop")  { architectureController.deSelectArchitectureDate();}
	            	if(getValue(NAME)=="resize"){architectureController.resizeArchitectureDate();}
	            	if(getValue(NAME)=="modify"){architectureController.modifyArchitectureDate();}
	            	if(getValue(NAME)=="sql")    {architectureController.sqlArchitectureDate();}
	            }
	        }
	
	
	
	
	
	
	}

