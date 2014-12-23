package gov.nasa.worldwind.render.city;


import java.util.ArrayList;

import processing.core.PApplet;

public class Graphy extends PApplet {
	public String command=null;
	public String addButton="PApplet.AddButton";
	public String addConnection="PApplet.AddConnection";
	public ArrayList<Population> populations=new ArrayList<Population>();
	public ArrayList<PopulationConn> populationConns=new ArrayList<PopulationConn>();
	
	
	public Graphy() {super();}
    public void setup() {
		this.size(800, 600, P3D);
    }
   public void draw() {	   
	    this.background(255);
		this.pushStyle(); 
		this.stroke(200);
		for (int i=0; i<this.height/10; i++){
		this.line(0, i*10, this.width,i*10);
		}
		for (int i=0; i<this.width/10; i++){
			this.line( i*10,0, i*10,this.height);
			}
		this.popStyle();
	   
	    if(populations !=null){
	    for (Population population :populations){	
		population.display();
	    }		
	   }
	    
	    if(populationConns !=null){
		    for (PopulationConn populationConn :populationConns){	
			populationConn.display();
		    }
			
		   }
	}

 public void addPopulation(int x, int y){	  
	  Population population = new Population(this);
	  this.registerMouseEvent(population);
	  population.point.x=x;
	  population.point.y=y;
	  this.populations.add(population);
  }
 
 public void addPopulationConn(){	  
	  PopulationConn populationConn = new PopulationConn(this);
	  this.registerMouseEvent(populationConn);
	  
	  this.populationConns.add(populationConn);

 }
 
 



}
