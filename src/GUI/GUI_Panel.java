package GUI;
import gov.nasa.worldwind.render.city.Graphy;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToolBar;
public class GUI_Panel extends JPanel {
	    Graphy papplet; 
	    GUI_Panel (){
	    this.setLayout(new BorderLayout());
		Dimension dimension=new Dimension(800,600);
		this.setSize(dimension);
	    this.setLocation(200, 0);	    
	    this.papplet =new Graphy();
   	    papplet.init();
   	    this.add(papplet,BorderLayout.CENTER);	
   	    //this.update(getGraphics());
   	    makeToolBar(this);
	}
	    
	    
	    
	public static void makeToolBar(GUI_Panel frame) {
			JToolBar toolBar = new JToolBar("controler");

			ControlerAction D_LayerAction = new ControlerAction("Button",
					new ImageIcon("./resources/layer.gif "), "Button", frame);

			ControlerAction H_LayerAction = new ControlerAction("Connection",
					new ImageIcon("./resources/layer_h.gif "), "Connection", frame);

			ControlerAction fullJustifyAction = new ControlerAction("Contraler",
					new ImageIcon("./resources/builder_pln.gif "),
					"Full   justify   text ", frame);
			ControlerAction datebaseAction = new ControlerAction("Datebase",
					new ImageIcon("./resources/conn.gif "), "datebase action",frame);

			toolBar.add(D_LayerAction);
			toolBar.add(H_LayerAction);
			toolBar.add(fullJustifyAction);
			toolBar.add(datebaseAction);
			frame.add(toolBar, BorderLayout.NORTH);
	 }
	
	public static class ControlerAction extends AbstractAction {
		public GUI_Panel appFrame;
		public ControlerAction(String text, Icon icon, String description, GUI_Panel appFrame) {
			super(text, icon);
			this.appFrame = appFrame;
			putValue(SHORT_DESCRIPTION, description);
		}

		public void actionPerformed(ActionEvent e) {
			if (getValue(NAME) == "Button") {
			appFrame.papplet.command=appFrame.papplet.addButton;
			appFrame.papplet.addPopulation(400, 300);
			}
			;
			if (getValue(NAME) == "Connection") {			
				appFrame.papplet.command=appFrame.papplet.addConnection;
				appFrame.papplet.addPopulationConn();
			}
			;
			if (getValue(NAME) == "Contraler") {
			}
			;
			if (getValue(NAME) == "Datebase") {
			}
			;

		}
	}
	
	
     static void main(){ new GUI_Panel();}

}
