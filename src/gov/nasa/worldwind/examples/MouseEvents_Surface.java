package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.*;
import mhd_world.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

public class MouseEvents_Surface extends AVListImpl {
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	    private final WorldWindow wwd;
	    private boolean armed_line = false;
	    private boolean armed_field= false;
	    
	    
	    private ArrayList<Position> positions = new ArrayList<Position>();
	    
	    
	    private final RenderableLayer layer;// the layer to display lines and the field.
	    
	    private final Polyline line;
	    private PolisField  field;
	    
	    private boolean active = false;
	    
     public MouseEvents_Surface(final WorldWindow wwd, RenderableLayer layer, Polyline polyline ,PolisField  field )
	    {
	        this.wwd = wwd;

	        if (polyline != null)
	        {
	            this.line = polyline;
	        }
	        else
	        {
	            this.line = new Polyline();
	            this.line.setFollowTerrain(true);
	           
	        }
	        
	        if ( field != null)
	        {
	            this.field = field;
	        }
	        else
	        {
	            this.field = new PolisField(50,wwd.getModel().getGlobe());
	         } 
	      
	        
	        this.layer = layer != null ? layer : new RenderableLayer();
	        this.layer.addRenderable(this.line);
	        this.field.display_grid(this.layer);
	        this.wwd.getModel().getLayers().add(this.layer);

	        
	        
	        
	        this.wwd.getInputHandler().addMouseListener(new MouseAdapter()
	        {
	           
	        	//begin of mouse listener
	        	
	        	// add a mouse listener of mousePress
	        	public void mousePressed(MouseEvent mouseEvent)
	            {
	                if (armed_line && mouseEvent.getButton() == MouseEvent.BUTTON1)
	                {
	                    if (armed_line && (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
	                    {
	                        if (!mouseEvent.isControlDown())
	                        {
	                            active = true;
	                            addPosition();
	                        }
	                    }
	                    mouseEvent.consume();
	                }
	            
	                //add the action of the polisfield
	            
	            
	            }

	        	// add a mouse listener of mouseRelease
	        	public void mouseReleased(MouseEvent mouseEvent)
	            {
	                if (armed_line && mouseEvent.getButton() == MouseEvent.BUTTON1)
	                {
	                    if (positions.size() == 1)
	                        removePosition();
	                    active = false;
	                    mouseEvent.consume();
	                }
	            
	            
	            
	              //add the action of the polisfield
	            
	            
	            }

	        	// add a mouse listener of mouseClick
	        	public void mouseClicked(MouseEvent mouseEvent)
	            {
	                
	        		
	        		if (armed_line && mouseEvent.getButton() == MouseEvent.BUTTON1)
	                {
	                    armed_field=false;	     
	        			if (mouseEvent.isControlDown())
	                        removePosition();
	                    mouseEvent.consume();
	                }
	                
	        		
	        		//add the action of the polisfield
	        		if(armed_field && mouseEvent.getButton() == MouseEvent.BUTTON1){
	        			 armed_line= false;
	        			 Position  curPos =wwd.getCurrentPosition();		                    
	                	 createField(curPos); 
	                	 mouseEvent.consume();
	                	 
	                	 }
	            
	              
	            
	            }
	        
	           //end of mouse listener
	        
	        
	        });

	        
	        this.wwd.getInputHandler().addMouseMotionListener(new MouseMotionAdapter()
	        {
	            
	        	 //begin of mouse motion listener
	        	public void mouseDragged(MouseEvent mouseEvent)
	            {
	                if (armed_line && (mouseEvent.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0)
	                {
	                    // Don't update the polyline here because the wwd current cursor position will not
	                    // have been updated to reflect the current mouse position. Wait to update in the
	                    // position listener, but consume the event so the view doesn't respond to it.
	                    if (active)
	                        mouseEvent.consume();
	                }
	                
	            	//add the action of the polisfield
	        		if(armed_field && (mouseEvent.getModifiers()==16) ){
	        			 armed_line= false;
	        			 System.out.println("drag");	        			 	        			 
	        			 mouseEvent.consume();
	                	 
	                	 }
	                
	            }
	        	//end of mouse motion listener
	        	
	        });

	        
	        
	        this.wwd.addPositionListener(new PositionListener()
	        {
	            public void moved(PositionEvent event)
	            {
	                if (!active) return;
	                if (positions.size() == 1) addPosition();
	                else replacePosition();
	            }
	        });
	    }

	   
	    
	    public RenderableLayer getLayer()
	    {
	        return this.layer;
	    }

	    public Polyline getLine()
	    {
	        return this.line;
	    }

	    public void clear()
	    {
	        while (this.positions.size() > 0)
	            this.removePosition();
	    }

	    public boolean isArmed()
	    {
	        return this.armed_line;
	    }
	    
	    

	    /**
	     * Arms and disarms the line builder. When armed, the line builder monitors user input and builds the polyline in
	     * response to the actions mentioned in the overview above. When disarmed, the line builder ignores all user input.
	     *
	     * @param armed true to arm the line builder, false to disarm it.
	     */
	    
	    //set the field function
	    public void createField(Position curPos){		    	
	    if (curPos == null)
	            return;		    
	    this.field.setSector(curPos);
	    this.field.initial();
	    this.field.display_grid(layer);	 
	    }
	    
	    
	    
	    public void setArmed_line(boolean armed)
	    {
	        this.armed_line = armed;
	    }
	    
	    public void setArmed_field(boolean armed)
	    {
	        this.armed_field = armed;
	    }

	    private void addPosition()
	    {
	        Position curPos = this.wwd.getCurrentPosition();
	        if (curPos == null)
	            return;

	        this.positions.add(curPos);
	        this.line.setPositions(this.positions);
	        this.firePropertyChange("LineBuilder.AddPosition", null, curPos);
	        this.wwd.redraw();
	    }

	    private void replacePosition()
	    {
	        Position curPos = this.wwd.getCurrentPosition();
	        if (curPos == null)
	            return;

	        int index = this.positions.size() - 1;
	        if (index < 0)
	            index = 0;

	        Position currentLastPosition = this.positions.get(index);
	        this.positions.set(index, curPos);
	        this.line.setPositions(this.positions);
	        this.firePropertyChange("LineBuilder.ReplacePosition", currentLastPosition, curPos);
	        this.wwd.redraw();
	    }

	    private void removePosition()
	    {
	        if (this.positions.size() == 0)
	            return;

	        Position currentLastPosition = this.positions.get(this.positions.size() - 1);
	        this.positions.remove(this.positions.size() - 1);
	        this.line.setPositions(this.positions);
	        this.firePropertyChange("LineBuilder.RemovePosition", currentLastPosition, null);
	        this.wwd.redraw();
	    }

	    
	    
	    
	    
      //set up the panel for this mouse events action
	    private static class LinePanel extends JPanel
	    {
	        private final WorldWindow wwd;
	        private final MouseEvents_Surface shapeBuilder;
	        
	        private JButton newButton;
	        private JButton pauseButton;
	        private JButton endButton;
	        private JButton fieldButton;	        
	        private JLabel[] pointLabels;

	        public LinePanel(WorldWindow wwd, MouseEvents_Surface shapeBuilder)
	        {
	            super(new BorderLayout());
	            this.wwd = wwd;
	            this.shapeBuilder = shapeBuilder;
	            this.makePanel(new Dimension(200, 400));
	            shapeBuilder.addPropertyChangeListener(new PropertyChangeListener()
	            {
	                public void propertyChange(PropertyChangeEvent propertyChangeEvent)
	                {
	                    fillPointsPanel();
	                }
	            });
	        }

	        private void makePanel(Dimension size)
	        {
	            JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 5));
	            
	            
	            
	            newButton = new JButton("New");
	            newButton.addActionListener(new ActionListener()
	            {
	                public void actionPerformed(ActionEvent actionEvent)
	                {
	                    shapeBuilder.clear();
	                    shapeBuilder.setArmed_line(true);
	                    pauseButton.setText("Pause");
	                    pauseButton.setEnabled(true);
	                    endButton.setEnabled(true);
	                    newButton.setEnabled(false);
	                    ((Component) wwd).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	                }
	            });
	            buttonPanel.add(newButton);
	            newButton.setEnabled(true);

	            
	            pauseButton = new JButton("Pause");
	            pauseButton.addActionListener(new ActionListener()
	            {
	                public void actionPerformed(ActionEvent actionEvent)
	                {
	                    shapeBuilder.setArmed_line(!shapeBuilder.isArmed());
	                    pauseButton.setText(!shapeBuilder.isArmed() ? "Resume" : "Pause");
	                    ((Component) wwd).setCursor(Cursor.getDefaultCursor());
	                }
	            });
	            buttonPanel.add(pauseButton);
	            pauseButton.setEnabled(false);

	            endButton = new JButton("End");
	            endButton.addActionListener(new ActionListener()
	            {
	                public void actionPerformed(ActionEvent actionEvent)
	                {
	                    shapeBuilder.setArmed_line(false);
	                    newButton.setEnabled(true);
	                    pauseButton.setEnabled(false);
	                    pauseButton.setText("Pause");
	                    endButton.setEnabled(false);
	                    ((Component) wwd).setCursor(Cursor.getDefaultCursor());
	                }
	            });
	            buttonPanel.add(endButton);
	            endButton.setEnabled(false);
	            
	            
	            
	            fieldButton = new JButton("FIELD");
	            fieldButton.addActionListener(new ActionListener()
	            {
	                public void actionPerformed(ActionEvent actionEvent)
	                {
	                   shapeBuilder.setArmed_field(true);        
		               ((Component) wwd).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		                    
		              
	                   
	                }
	            });
	            buttonPanel.add(fieldButton);
	            //fieldButton.setEnabled(false);
	            
	            
	            
	            
	            

	            
	            
	            
	            
	            
	            JPanel pointPanel = new JPanel(new GridLayout(0, 1, 0, 10));
	            pointPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	            this.pointLabels = new JLabel[20];
	            for (int i = 0; i < this.pointLabels.length; i++)
	            {
	                this.pointLabels[i] = new JLabel("");
	                pointPanel.add(this.pointLabels[i]);
	            }

	            // Put the point panel in a container to prevent scroll panel from stretching the vertical spacing.
	            JPanel dummyPanel = new JPanel(new BorderLayout());
	            dummyPanel.add(pointPanel, BorderLayout.NORTH);

	            // Put the point panel in a scroll bar.
	            JScrollPane scrollPane = new JScrollPane(dummyPanel);
	            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	            if (size != null)
	                scrollPane.setPreferredSize(size);

	            // Add the buttons, scroll bar and inner panel to a titled panel that will resize with the main window.
	            JPanel outerPanel = new JPanel(new BorderLayout());
	            outerPanel.setBorder(
	                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Line")));
	            outerPanel.setToolTipText("Line control and info");
	            outerPanel.add(buttonPanel, BorderLayout.NORTH);
	            outerPanel.add(scrollPane, BorderLayout.CENTER);
	            this.add(outerPanel, BorderLayout.CENTER);
	        }

	        private void fillPointsPanel()
	        {
	            int i = 0;
	            for (Position pos : shapeBuilder.getLine().getPositions())
	            {
	                if (i == this.pointLabels.length)
	                    break;

	                String las = String.format("Lat %7.4f\u00B0", pos.getLatitude().getDegrees());
	                String los = String.format("Lon %7.4f\u00B0", pos.getLongitude().getDegrees());
	                pointLabels[i++].setText(las + "  " + los);
	            }
	            for (; i < this.pointLabels.length; i++)
	                pointLabels[i++].setText("");
	        }
	    }

	  
	    
	    
	    
	    static class  AppFrame extends ApplicationTemplate.AppFrame
	    {
	        public AppFrame()
	        {
	            super(true, false, false);

	            MouseEvents_Surface lineBuilder = new MouseEvents_Surface(this.getWwd(), null, null,null);
	            this.getContentPane().add(new LinePanel(this.getWwd(), lineBuilder), BorderLayout.WEST);
	        }
	    }

	  
	    public static void main(String[] args)
	    {
	        ApplicationTemplate.start("World Wind Line Builder", MouseEvents_Surface.AppFrame.class);
	    }


}
