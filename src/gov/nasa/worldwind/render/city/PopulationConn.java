package gov.nasa.worldwind.render.city;


import java.awt.event.MouseEvent;

public class PopulationConn {
	public Graphy papplet;
	public Population p1=null, p2=null;
	public boolean selected=false;	

	PopulationConn(Population p1, Population p2, Graphy papplet) {
		this.papplet = papplet;
		this.p1 = p1;
		this.p2 = p2;
	}
	
	PopulationConn(Graphy papplet) {
		this.papplet = papplet;
		
	}

	public void display() {
		if(p1 !=null && p2 != null){
		this.papplet.line(p1.point.x, p1.point.y, p2.point.x, p2.point.y);
		}
	}

	public void mouseEvent(MouseEvent event) {

		if (this.papplet.command == this.papplet.addConnection) {
			if (event.getID() == MouseEvent.MOUSE_CLICKED
					&& event.getModifiers() == MouseEvent.BUTTON1_MASK) {
                 if(this.papplet.populations !=null && this.papplet.populations.size()>1){   
				 for(Population population: this.papplet.populations){
					 if(population.isInside(event.getX(), event.getY())){
						 this.p1=population;
					 }
				 }
                 }
			}
			
			if (event.getID() == MouseEvent.MOUSE_DRAGGED
					&& event.getModifiers() == MouseEvent.BUTTON1_MASK) {
                 if(this.papplet.populations !=null && this.papplet.populations.size()>1){   
				 for(Population population: this.papplet.populations){
					 if(population.isInside(event.getX(), event.getY())){
						 this.p2=population;
					 }
				 }
                 }
			}
			
			
			
			

		}
		
		
		
		
		
		
		
		

	}

}
