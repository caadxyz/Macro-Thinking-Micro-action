	package gov.nasa.worldwind.render.city;

	import gov.nasa.worldwind.render.Material;
	import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;

	import java.awt.BorderLayout;
	import java.awt.Color;
	import java.awt.Component;
	import java.awt.Dimension;
	import java.awt.GridLayout;
	import java.awt.event.ActionEvent;
	import java.awt.event.ActionListener;
	import java.beans.PropertyChangeEvent;
	import java.beans.PropertyChangeListener;

	import javax.swing.BorderFactory;
	import javax.swing.Box;
	import javax.swing.JButton;
	import javax.swing.JCheckBox;
	import javax.swing.JComboBox;
	import javax.swing.JLabel;
	import javax.swing.JPanel;
	import javax.swing.JScrollPane;
	import javax.swing.JSpinner;
	import javax.swing.JTable;
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
	public class EnvironmentBuilderPanel  extends JPanel{

	        private  JComboBox factoryComboBox;
		    private  JTable entryTable;
		    private  JSpinner sp;
		    private  PolylineBuilderModel  model;
		    private  CityBuilderController controller;
		    
		    private boolean ignoreSelectEvents = false;
		    
		    public EnvironmentBuilderPanel(PolylineBuilderModel model, CityBuilderController controller)
		    {   this.model=model;
		        this.controller=controller;
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

		    private void initComponents(PolylineBuilderModel model, final CityBuilderController controller)
		    {
		        final JCheckBox resizeNewShapesCheckBox;
		        final JCheckBox enableEditCheckBox;

		        JPanel newShapePanel = new JPanel();
		        TitledBorder b = BorderFactory.createTitledBorder("Environment Attributes");
		        newShapePanel.setBorder(new CompoundBorder(b, BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		        {
		            JButton newShapeButton = new JButton("New");
		            newShapeButton.setActionCommand(CityBuilder.NEW_ENVIRONMENT);
		            newShapeButton.addActionListener(controller);
		            newShapeButton.setToolTipText("Create a new street centered in the viewport");
	                String type[]={"Water","Green","Sport"};
		            this.factoryComboBox = new JComboBox(type);
		            this.factoryComboBox.setEditable(false);
		            this.factoryComboBox.setToolTipText("Choose environment type to create");

		            resizeNewShapesCheckBox = new JCheckBox("Fit new streets to viewport");
		            resizeNewShapesCheckBox.setActionCommand(CityBuilder.SIZE_NEW_SHAPES_TO_VIEWPORT);
		            resizeNewShapesCheckBox.addActionListener(controller);
		            resizeNewShapesCheckBox.setSelected(controller.isResizeNewShapesToViewport());
		            resizeNewShapesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		            resizeNewShapesCheckBox.setToolTipText("New streets are sized to fit the geographic viewport");

		            enableEditCheckBox = new JCheckBox("Enable street editing");
		            enableEditCheckBox.setActionCommand(CityBuilder.ENABLE_EDIT_ENVIRONMENT);
		            enableEditCheckBox.addActionListener(controller);
		            enableEditCheckBox.setSelected(controller.isPeEnableEdit());
		            enableEditCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		            enableEditCheckBox.setToolTipText("Allow modifications to shapes");

		            Box newShapeBox = Box.createHorizontalBox();
		            newShapeBox.add(newShapeButton);
		            newShapeBox.add(Box.createHorizontalStrut(5));
		            newShapeBox.add(this.factoryComboBox);
		            newShapeBox.setAlignmentX(Component.LEFT_ALIGNMENT);

		            JPanel gridPanel = new JPanel(new GridLayout(0, 1, 0, 5)); // rows, cols, hgap, vgap
		            gridPanel.add(newShapeBox);
		            gridPanel.add(resizeNewShapesCheckBox);
		            gridPanel.add(enableEditCheckBox);

		            
		            
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
		                	ShapeAttributes attributes;
		                	attributes=controller.getSelectedPeEntry().getAttributes();
		                	attributes.setOutlineWidth(Float.parseFloat((String) ((JSpinner) changeEvent.getSource()).getValue()));
		                    controller.getSelectedPeEntry().getPolyline().setAttributes(attributes);
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
		                      ShapeAttributes  attributes;
			                	attributes=controller.getSelectedPeEntry().getAttributes();
			                	attributes.setOutlineMaterial(new Material(color));
			                    controller.getSelectedPeEntry().getPolyline().setAttributes(attributes);
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
		                        controller.actionPerformed(new ActionEvent(e.getSource(), -1, CityBuilder.SELECTION_CHANGED_PE));
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
		        {
		            JButton delselectButton = new JButton("Deselect");
		            delselectButton.setActionCommand(CityBuilder.CLEAR_SELECTION_PE);
		            delselectButton.addActionListener(controller);
		            delselectButton.setToolTipText("Clear the selection");

		            JButton deleteButton = new JButton("Delete Selected");
		            deleteButton.setActionCommand(CityBuilder.REMOVE_SELECTED_PE);
		            deleteButton.addActionListener(controller);
		            deleteButton.setToolTipText("Delete selected shapes");

		            JPanel gridPanel = new JPanel(new GridLayout(0, 1, 0, 5)); // rows, cols, hgap, vgap
		            gridPanel.add(delselectButton);
		            gridPanel.add(deleteButton);

		            selectionPanel.setLayout(new BorderLayout());
		            selectionPanel.add(gridPanel, BorderLayout.NORTH);
		        }

		        this.setLayout(new BorderLayout(30, 0)); // hgap, vgap
		        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // top, left, bottom, right
		        this.add(newShapePanel, BorderLayout.NORTH);
		        this.add(entryPanel, BorderLayout.CENTER);
		        this.add(selectionPanel, BorderLayout.SOUTH);
		        //this.add(this.tabbedPane, BorderLayout.NORTH);

		        controller.addPropertyChangeListener(new PropertyChangeListener()
		        {
		            public void propertyChange(PropertyChangeEvent e)
		            {
		                //noinspection StringEquality
		                if (e.getPropertyName() == CityBuilder.SIZE_NEW_SHAPES_TO_VIEWPORT)
		                {
		                    resizeNewShapesCheckBox.setSelected(controller.isResizeNewShapesToViewport());
		                }
		                else //noinspection StringEquality
		                    if (e.getPropertyName() == CityBuilder.ENABLE_EDIT_ENVIRONMENT)
		                {
		                    enableEditCheckBox.setSelected(controller.isPeEnableEdit());
		                }
		            }
		        });
		    }
		}

