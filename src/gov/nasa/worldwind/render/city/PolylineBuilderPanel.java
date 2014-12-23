package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.render.city.CityBuilderController;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//**************************************************************//
//********************  Airspace Builder Panel  ****************//
//**************************************************************//

public class PolylineBuilderPanel extends JPanel
{
    private JComboBox factoryComboBox;
    private JTable entryTable;
    private boolean ignoreSelectEvents = false;
    
    public PolylineBuilderPanel(PolylineBuilderModel model, CityBuilderController controller)
    {
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

    public PolylineFactory getSelectedFactory()
    {
        return (PolylineFactory) this.factoryComboBox.getSelectedItem();
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
        {
            JButton newShapeButton = new JButton("New shape");
            newShapeButton.setActionCommand(CityBuilder.NEW_STREET);
            newShapeButton.addActionListener(controller);
            newShapeButton.setToolTipText("Create a new shape centered in the viewport");

            this.factoryComboBox = new JComboBox(CityBuilder.defaultStreetFactories);
            this.factoryComboBox.setEditable(false);
            this.factoryComboBox.setToolTipText("Choose shape type to create");

            resizeNewShapesCheckBox = new JCheckBox("Fit new shapes to viewport");
            resizeNewShapesCheckBox.setActionCommand(CityBuilder.SIZE_NEW_SHAPES_TO_VIEWPORT);
            resizeNewShapesCheckBox.addActionListener(controller);
            resizeNewShapesCheckBox.setSelected(controller.isResizeNewShapesToViewport());
            resizeNewShapesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            resizeNewShapesCheckBox.setToolTipText("New shapes are sized to fit the geographic viewport");

            enableEditCheckBox = new JCheckBox("Enable shape editing");
            enableEditCheckBox.setActionCommand(CityBuilder.ENABLE_EDIT_STREET);
            enableEditCheckBox.addActionListener(controller);
            enableEditCheckBox.setSelected(controller.isPlEnableEdit());
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

            newShapePanel.setLayout(new BorderLayout());
            newShapePanel.add(gridPanel, BorderLayout.NORTH);
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
                        controller.actionPerformed(new ActionEvent(e.getSource(), -1, CityBuilder.SELECTION_CHANGED_PL));
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
            delselectButton.setActionCommand(CityBuilder.CLEAR_SELECTION_PL);
            delselectButton.addActionListener(controller);
            delselectButton.setToolTipText("Clear the selection");

            JButton deleteButton = new JButton("Delete Selected");
            deleteButton.setActionCommand(CityBuilder.REMOVE_SELECTED_PL);
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
        this.add(newShapePanel, BorderLayout.WEST);
        this.add(entryPanel, BorderLayout.CENTER);
        this.add(selectionPanel, BorderLayout.EAST);
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
                    if (e.getPropertyName() == CityBuilder.ENABLE_EDIT_STREET)
                {
                    enableEditCheckBox.setSelected(controller.isPlEnableEdit());
                }
            }
        });
    }
}