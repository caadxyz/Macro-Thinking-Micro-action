package gov.nasa.worldwind.render.city;



import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;


public class Population {
	Point point;
	Color color; 
	int width;
	int height;
	boolean selected=false;	
	Graphy papplet;
	JPopupMenu popupMenu;
	public ArrayList<Population> inPopulations=new ArrayList<Population>();
	public ArrayList<Population> outPopulations=new ArrayList<Population>();
	
	Population(Graphy papplet){
		// Create some menu items for the popup
		JMenuItem menuFileNew    = new JMenuItem( "New" );
		JMenuItem menuFileOpen   = new JMenuItem( "Open..." );
		JMenuItem menuFileSave   = new JMenuItem( "Save" );
		JMenuItem menuFileSaveAs = new JMenuItem( "Save As..." );
		JMenuItem menuFileExit   = new JMenuItem( "Exit" );
		
		// Create a popup menu
		popupMenu = new JPopupMenu( "Menu" );
		popupMenu.add( menuFileNew );
		popupMenu.add( menuFileOpen );
		popupMenu.add( menuFileSave );
		popupMenu.add( menuFileSaveAs );
		popupMenu.add( menuFileExit ); 
		this.point=new Point();
		this.point.x=0;
		this.point.y=0;
		this.color=new Color(0,0,0);
		this.width=50;
		this.height=50;
		this.papplet=papplet;
	}

	public	void display(){		
	this.papplet.pushMatrix();
	this.papplet.translate(this.point.x-this.width/2, this.point.y-this.height/2);
	this.papplet.pushStyle();
	if(selected){
	this.papplet.stroke(255,0,0);
	}
	else{
	this.papplet.stroke(this.color.getRed(),this.color.getGreen(),this.color.getBlue());
	}
	
	this.papplet.rect(0, 0, this.width, this.height);
	this.papplet.popStyle();
    this.papplet.popMatrix();
		
	}
	
public void mouseEvent(MouseEvent event){	
	
	if(this.papplet.command==this.papplet.addButton){
	if (this.isInside(event.getX(), event.getY()))
	{
        if(event.getID()==MouseEvent.MOUSE_DRAGGED && event.getModifiers()==MouseEvent.BUTTON1_MASK){
    	  this.point.x=event.getX();
    	  this.point.y=event.getY();
    	  this.selected=true;
         }
         
        if(event.getID()==MouseEvent.MOUSE_CLICKED && event.getModifiers()==MouseEvent.BUTTON3_MASK){
        	 popupMenu.show( event.getComponent(),event.getX(), event.getY() );
         }
	}
	else {this.selected=false;}
	
	}

}

public boolean isInside(int x, int y){
	if ( Math.abs(x-this.point.x) <this.width/2 && Math.abs(y-this.point.y)<this.height/2 ){
		return true;
	}else {return false;}
	
	}



  
	
	

}
