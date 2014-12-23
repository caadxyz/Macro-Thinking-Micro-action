package mhd_world;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.examples.ApplicationTemplate;
import gov.nasa.worldwind.examples.LayerPanel;
import gov.nasa.worldwind.examples.util.SectorSelector;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ContourLinePolygon;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.WWMath;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;

import javax.swing.*;

import mhd_world.analytics.*;

class G_City_Frame {
	private WorldWindow wwd;
	private SectorSelector selector;
	private RenderableLayer renderableLayer;
	private RenderableLayer grid_Layer;
	private RenderableLayer contour_Layer;
	private RenderableLayer analytic_Layer;
	private PolisField field;
	

	G_City_Frame(WorldWindow wwd,LayerPanel layerpanel) {
		this.wwd = wwd;		
		
		// Add a renderable layer1
		renderableLayer = new RenderableLayer();
		renderableLayer.setName("G CITY");
		renderableLayer.setPickEnabled(false);
		ApplicationTemplate.insertBeforePlacenames(wwd, renderableLayer);
		
		// Add a renderable layer2
		grid_Layer = new RenderableLayer();
		grid_Layer.setName("GRID CITY");
		grid_Layer.setPickEnabled(false);
		ApplicationTemplate.insertBeforePlacenames(wwd, grid_Layer);
		
		// Add a renderable layer3
		contour_Layer = new RenderableLayer();
		contour_Layer.setName("CONTOUR LINE");
		contour_Layer.setPickEnabled(false);
		ApplicationTemplate.insertBeforePlacenames(wwd, contour_Layer);
		
		
		// Add a renderable layer4
		analytic_Layer = new RenderableLayer();
		analytic_Layer.setName("ANALYTIC SURFACE");
		analytic_Layer.setPickEnabled(false);
		ApplicationTemplate.insertBeforePlacenames(wwd, analytic_Layer);
		
		
		
		layerpanel.update(wwd);
	  
		// select a city region
		this.selector = new SectorSelector(this.wwd);
		this.selector.setInteriorColor(new Color(1f, 1f, 1f, 0.1f));
		this.selector.setBorderColor(new Color(1f, 0f, 0f, 0.5f));
		this.selector.setBorderWidth(3);
		this.selector.addPropertyChangeListener(SectorSelector.SECTOR_PROPERTY,
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						Sector sector = (Sector) evt.getNewValue();
						System.out.println(selector.getSector() != null ? selector
										.getSector()
										: "no sector");
					}
				});
		
		Sector sector;
		sector=Sector.fromDegrees(0, 1, 0, 1);
		this.field= new PolisField(50, sector, this.wwd.getModel().getGlobe());

	}

	void createAndShowGUI() {
		// Create and set up the window.
		JFrame frame = new JFrame("City Tools");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		
		JLabel label = new JLabel("   icons in G CITY, ETHZ");
		label.setPreferredSize(new Dimension(175, 100));
		
		
		JButton btn4 = new JButton(new Dispaly_field_grid());
		btn4.setToolTipText("dispaly a grid");
		
		
		JPanel p0 = new JPanel(new BorderLayout(5, 5));
		p0.add(label,BorderLayout.NORTH);
		p0.add(btn4,BorderLayout.SOUTH);
		frame.getContentPane().add(p0, BorderLayout.NORTH);

		

		
		JButton btn1 = new JButton(new EnableSelectorAction());
		btn1.setToolTipText("select a region");
		
	    JButton btn2 = new JButton(new Contour_Lines());
		btn2.setToolTipText("create a region of continue lines");
		
		JButton btn3 = new JButton(new AnalyticS());
		btn3.setToolTipText("create a region of surface");
		
        JPanel p1 = new JPanel(new BorderLayout(5, 5));
		
		p1.add(btn1,BorderLayout.NORTH);
		p1.add(btn2,BorderLayout.CENTER);
		p1.add(btn3,BorderLayout.SOUTH);
		frame.getContentPane().add(p1, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
		
		
		

		
	}

	

	

	
	
	
	
private class EnableSelectorAction extends AbstractAction {
		public EnableSelectorAction() {
			super("Select a Region");
		}

		public void actionPerformed(ActionEvent e) {
			
			((JButton) e.getSource()).setAction(new DisableSelectorAction());
             selector.enable();
		}
	}


private class DisableSelectorAction extends AbstractAction
{
    public DisableSelectorAction()
    {
        super("Create a City Site");
    }

    public void actionPerformed(ActionEvent e)
    {
    	if(selector.getSector() !=null) field.setSector(selector.getSector());
    	field.initial();
    	field.display_grid(grid_Layer);
    	selector.disable();
    	((JButton) e.getSource()).setAction(new EnableSelectorAction());
    }
}



	
private class Contour_Lines extends AbstractAction {
		public Contour_Lines() {
			super("create contour lines");
		}

		public void actionPerformed(ActionEvent e) {

			field.display_contour(contour_Layer);
			selector.disable();

		}
	}
	


private class  Dispaly_field_grid extends AbstractAction{
	
	public Dispaly_field_grid() {
		super("function");
	}

	public void actionPerformed(ActionEvent e) {			
	
	field.display_grid(grid_Layer);
	selector.disable();
	}
	
}
	

private class AnalyticS extends AbstractAction{
		
		public AnalyticS () {
			super("create AnalyticSurface");
		}	
		public void actionPerformed(ActionEvent e) {
			
			
			//field.setSectorSelector(field.sector);	
			field.display_Analytic(analytic_Layer);			
	        selector.disable();
			
			
		}
		
	
		
		
}
	
	
	
	
	
	
	
	
	
	
	
	

}