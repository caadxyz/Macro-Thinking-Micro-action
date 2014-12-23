package mhd_world;


import java.awt.Color;
import java.awt.Point;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.util.ArrayList;

import mhd_world.analytics.AnalyticSurface;
import mhd_world.analytics.AnalyticSurfaceAttributes;
import mhd_world.analytics.AnalyticSurfaceLegend;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.examples.util.SectorSelector;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ContourLinePolygon;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.WWMath;

public class PolisField {
	  int num=50;
	  Point_onField[][] point_onfield; 
	  Sub_Field[][] sub_field;
	  Sector sector;
	  Globe globe;
	  
	  
	  //defined neighbor
	  int nRIGHT=0;
	  int nUP=1;
	  int nLEFT=2;
	  int nDOWN=3;
	  
	  int nDOWNLEFT=0;
	  int nDOWNRIGHT=1;
	  int nUPRIGHT=2;
	  int nUPLEFT=3;
	  
	  
	  class Point_onField{
		     Position onpoint; 
			 int i;
			 int j;
			 Point_onField(){
		     this.onpoint=Position.fromDegrees(0, 0, 0);
		     this.i=0;
		     this.j=0;
			 }			 
			 ///////////////////get polispath from document by point on field/////////////////////////////////
			/*
			PolisPath getpath(){  
			PolisPath path=null;
			for(int i = 0; i <active_doc.paths.size();i++){       
			        path=(PolisPath) active_doc.paths.get(i); 
			        if(path.point_onfield==this) return path;
			        }
			return path;
			}
			*/
			/////////////////////end of get polispath from document by point on field ///////////////////////////////////////////// 
			 


			 ///////////////////get polisDistrict from document by point on field/////////////////////////////////
			
			 /* 
			 PolisDistrict getDistrict(){  
			PolisDistrict district=null;
			for(int i = 0; i <active_doc.districts.size();i++){       
			        district=(PolisDistrict) active_doc.districts.get(i); 
			        if(district.path.point_onfield==this) return district;
			        }
			return district;
			}
			 
			*/ 
			/////////////////////end of get polisDistrict from document by point on field ///////////////////////////////////////////// 
            }// end of class of point_onField.
	  
	  
	  
		
	  
	  class Sub_Field{
			Point_onField point_onfield0;
			Point_onField point_onfield1;
			Point_onField point_onfield2;
			Point_onField point_onfield3;
			int i;
			int j;
			 
			Sub_Field(Point_onField point_onfield0,Point_onField point_onfield1,Point_onField point_onfield2,Point_onField point_onfield3){
			this.point_onfield0=point_onfield0;
			this.point_onfield1=point_onfield1;
			this.point_onfield2=point_onfield2;
			this.point_onfield3=point_onfield3;
			this.i=point_onfield0.i;
			this.j=point_onfield0.j;
			}
			Sub_Field(){
			this.point_onfield0=new  Point_onField();	
			this.point_onfield1=new  Point_onField();
			this.point_onfield2=new  Point_onField();	
			this.point_onfield3=new  Point_onField();
			this.i=0;
			this.j=0;
			}

			
			/*
			double getArea(){
			double area;
			area=sqrt((this.point_onfield0.onpoint.x-this.point_onfield1.onpoint.x)*(this.point_onfield0.onpoint.x-this.point_onfield1.onpoint.x)+(this.point_onfield0.onpoint.y-this.point_onfield1.onpoint.y)*(this.point_onfield0.onpoint.y-this.point_onfield1.onpoint.y));
			return area;
			}
            */
			
			/*
			M_OnPoint getCentre(){
			M_OnPoint  centre= new M_OnPoint();
			centre.x=(point_onfield0.onpoint.x+point_onfield1.onpoint.x+point_onfield2.onpoint.x+point_onfield3.onpoint.x)/4;
			centre.y=(point_onfield0.onpoint.y+point_onfield1.onpoint.y+point_onfield2.onpoint.y+point_onfield3.onpoint.y)/4;
			centre.z=(point_onfield0.onpoint.z+point_onfield1.onpoint.z+point_onfield2.onpoint.z+point_onfield3.onpoint.z)/4;
			return centre;
			 }
			 */
			
			
		} //end of class
	  
public PolisField(int num, Sector sector, Globe globe) {// logical places in the network and xy coordinates
		    this.num=num; 
		    this.globe=globe;
		    this.point_onfield=new Point_onField[num][num] ;
		    this.sub_field =new Sub_Field[num-1][num-1] ;
	        set_Point_onField();
	        setSector(sector);
	        initial();
	  } //end of the construction function01
	  
public PolisField(int num, SectorSelector selector, Globe globe) {// logical places in the network and xy coordinates
	    this.num=num; 
	    this.globe=globe;
	    this.point_onfield=new Point_onField[num][num] ;
	    this.sub_field    =new Sub_Field[num-1][num-1] ;
        set_Point_onField();
        setSector(selector.getSector());
        initial();
        
}   // end of the construction function02

public PolisField(int num, Globe globe) {// logical places in the network and xy coordinates
    this.num=num; 
    this.globe=globe;
    this.point_onfield=new Point_onField[num][num] ;
    this.sub_field    =new Sub_Field[num-1][num-1] ;
    set_Point_onField();
    setSector(Sector.fromDegrees(0,1, 0, 1));
    initial();
    
}   // end of the construction function03
	  






void set_Point_onField(){
	  
	  for (int i = 0; i < num; i++) {       
		      for (int j = 0; j < num; j++) { 
		       point_onfield[i][j]=new Point_onField(); 
		       point_onfield[i][j].i=i;
		       point_onfield[i][j].j=j; 
		      }}      
		    
	 for (int i = 0; i < num-1; i++) {       
	      for (int j = 0; j < num-1; j++) { 
	      sub_field[i][j]=new Sub_Field();
	      sub_field[i][j].i=i;
	      sub_field[i][j].j=j;
	      }}
     
      
     }
  
public void setSector(Sector sector){
	Angle minlat,maxlat,minlon,maxlon;				
	minlat=sector.getMinLatitude();
	minlon=sector.getMinLongitude();
	maxlat=sector.getMaxLatitude();
	maxlon=sector.getMaxLongitude();	
		
	if(sector.getDeltaLatDegrees()<sector.getDeltaLonDegrees())
	{maxlon=Angle.fromDegrees(maxlon.degrees-sector.getDeltaLonDegrees()+sector.getDeltaLatDegrees());}
	else { maxlat=Angle.fromDegrees(maxlat.degrees+sector.getDeltaLonDegrees()-sector.getDeltaLatDegrees());}			
	this.sector=new Sector(minlat,maxlat,minlon,maxlon);
	
}
     

public void setSector(Position curPos){
	Angle minlat,maxlat,minlon,maxlon;	
	
	minlat=Angle.fromDegrees(curPos.getLatitude().degrees-1);
	minlon=Angle.fromDegrees(curPos.getLongitude().degrees-1);
	maxlat=Angle.fromDegrees(curPos.getLatitude().degrees+1);
	maxlon=Angle.fromDegrees(curPos.getLongitude().degrees+1);
	this.sector=new Sector(minlat,maxlat,minlon,maxlon);
	}




public void initial() {  
	
	//double d1=LatLon.ellipsoidalDistance(p0, p1, 6378137, 6356752.3 );
	//double d2=LatLon.ellipsoidalDistance(p0, p3, 6378137, 6356752.3 );
	double latStep =-this.sector.getDeltaLatDegrees() / (this.num-1);
    double lonStep = this.sector.getDeltaLonDegrees() / (this.num-1);
    double lat = this.sector.getMaxLatitude().degrees;
    
    Position[][] position=new  Position[this.num][this.num];
    for (int j = 0; j < this.num; j++)
    {
     double lon = this.sector.getMinLongitude().degrees;
        for (int i = 0; i <this.num; i++)
        {
                  
            double ele = globe.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));  
            position[i][j]=new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon),ele+100);  
            this.point_onfield[i][j].onpoint=position[i][j];
            lon += lonStep;
        }
                  
        lat += latStep;
    }
   

    for (int i = 0; i < num-1; i++) {       
	      for (int j = 0; j < num-1; j++) { 
	      sub_field[i][j].point_onfield0= point_onfield[i][j];
	      sub_field[i][j].point_onfield1=point_onfield[i+1][j];
	      sub_field[i][j].point_onfield2=point_onfield[i+1][j+1];
	      sub_field[i][j].point_onfield3=point_onfield[i][j+1];
	      }}
}




	//get neighbor of point
	Point_onField getNb(int x,int y,int dir){
	  if (dir==this.nRIGHT && x<num-1){
	    return point_onfield[x+1][y];
	  }   
	  if (dir==this.nUP && y<num-1){
	    return point_onfield[x][y+1];
	  }
	  if (dir==this.nLEFT && x>0){
	    return point_onfield[x-1][y];
	  }
	  if (dir==this.nDOWN && y>0){
	    return point_onfield[x][y-1];
	  }
	  return null;
	}


	//get point_onField 
	Point_onField getPoint(int x,int y){
	return point_onfield[x][y];
	}




	// get neighbor of sub_field;
Sub_Field getNb(Sub_Field  current_field,int dir){

	  if (dir==this.nRIGHT && current_field.i<num-2){
	    return this.sub_field[current_field.i+1][current_field.j];
	  }   
	  if (dir==this.nUP && current_field.j<num-1){
	    return this.sub_field[current_field.i][current_field.j+1];
	  }
	  if (dir==this.nLEFT && current_field.i>0){
	    return this.sub_field[current_field.i-1][current_field.j];
	  }
	  if (dir==this.nDOWN && current_field.j>0){
	    return this.sub_field[current_field.i][current_field.j-1];
	  }
	  return null;
	}

Sub_Field  getSF(Point_onField current_field,int dir){
	  if (dir==this.nDOWNLEFT && current_field.i>0 && current_field.j>0){
	    return this.sub_field[current_field.i-1][current_field.j-1];
	  } 
	   if (dir==this.nDOWNRIGHT && current_field.i<num-1 && current_field.j>0){
	    return this.sub_field[current_field.i][current_field.j-1];
	  }   
	    if (dir==this.nUPRIGHT && current_field.i<num-1 && current_field.j<num-1){
	    return this.sub_field[current_field.i][current_field.j];
	  }
	  
	  if (dir==this.nUPLEFT && current_field.i>0 && current_field.j<num-1){
	    return this.sub_field[current_field.i-1][current_field.j];
	  }
	  return null;
	}



//setup how to display this field in the globe
public void display_grid(RenderableLayer renderableLayer) {
	
	
    ArrayList<Position> positions_grid = new ArrayList<Position>();
    Polyline pline_lat[]=new Polyline[50];
    Polyline pline_lon[]=new Polyline[50];	        
    for (int j = 0; j < 50; j++)
    {
       for (int i = 0; i < 50; i++)
        {
    	   positions_grid.add(this.point_onfield[i][j].onpoint);
    	  	           	 
        }
       pline_lat[j]=new Polyline(positions_grid);
	   renderableLayer.addRenderable(pline_lat[j]);	
       positions_grid.clear();
      } 
    
    
    for (int i = 0; i < 50; i++)
    {
     for (int j = 0; j < 50; j++)
      {
      positions_grid.add(this.point_onfield[i][j].onpoint);
      } 
     pline_lon[i]=new Polyline(positions_grid);
     
     renderableLayer.addRenderable(pline_lon[i]);	
     positions_grid.clear();
      }
    } 




void display_contour(RenderableLayer renderableLayer) {
	
	ArrayList<LatLon> positions = new ArrayList<LatLon>();
	positions.add(LatLon.fromDegrees(this.sector
			.getMinLatitude().degrees, this.sector
			.getMinLongitude().degrees));
	positions.add(LatLon.fromDegrees(this.sector
			.getMaxLatitude().degrees, this.sector
			.getMinLongitude().degrees));
	positions.add(LatLon.fromDegrees(this.sector
			.getMaxLatitude().degrees, this.sector
			.getMaxLongitude().degrees));
	positions.add(LatLon.fromDegrees(this.sector
			.getMinLatitude().degrees, this.sector
			.getMaxLongitude().degrees));
	positions.add(LatLon.fromDegrees(this.sector
			.getMinLatitude().degrees, this.sector
			.getMinLongitude().degrees));



	for (int elevation = 0; elevation <= 8000; elevation += 100) {
		ContourLinePolygon cl = new ContourLinePolygon(elevation,
				positions);
		cl.setColor(new Color(.2f, .2f, .8f));
		cl.setViewClippingEnabled(true);
		renderableLayer.addRenderable(cl);
		if (elevation % 1000 == 0) {
			cl.setLineWidth(2);
			cl.setColor(new Color(0f, .1f, .6f));
		}
		if (elevation % 500 == 0)
			cl.setLineWidth(2);
	}
 } 

void display_Analytic(RenderableLayer renderableLayer) {


   double HUE_BLUE = 0d / 360d;
   double HUE_RED =  240d / 360d;        
   double minValue = Double.MAX_VALUE;
   double maxValue = -Double.MAX_VALUE;
   
   AnalyticSurface surface = new AnalyticSurface();
   AnalyticSurfaceAttributes as= new AnalyticSurfaceAttributes(surface.getSurfaceAttributes());
   as.setDrawShadow(false);
   //as.setDrawInterior(false);
   surface.setSector(this.sector);
   surface.setAltitude(200);
   surface.setSurfaceAttributes(as);        
   surface.setDimensions(50, 50);
   surface.setClientLayer(renderableLayer);
   renderableLayer.addRenderable(surface); 
        
   ArrayList<AnalyticSurface.GridPointAttributes> attributesList
   = new ArrayList<AnalyticSurface.GridPointAttributes>();         

          
   double latStep = -surface.getSector().getDeltaLatDegrees() / 49;
   double lonStep = surface.getSector().getDeltaLonDegrees() / 49;
   double lat = surface.getSector().getMaxLatitude().degrees;
   
   Position[][] position=new  Position[50][50];
   for (int y = 0; y < 50; y++)
   {
    double lon = surface.getSector().getMinLongitude().degrees;
       for (int x = 0; x < 50; x++)
       {
                 
           double ele = this.globe.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));  
           position[x][y]=new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon),ele+100);  
          
           if (minValue > ele)
               minValue = ele;
           if (maxValue < ele)
               maxValue = ele;
              lon += lonStep;
       }
                 
       lat += latStep;
   }  
   lat = surface.getSector().getMaxLatitude().degrees; 
   
       
  for (int y = 0; y < 50; y++)
   {
    double lon = surface.getSector().getMinLongitude().degrees;
       for (int x = 0; x < 50; x++)
       {              
           double ele = this.globe.getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));                
           double hueFactor = WWMath.computeInterpolationFactor(ele, minValue, maxValue);               
           System.out.println(hueFactor);
           Color color = Color.getHSBColor((float) WWMath.mixSmooth(hueFactor,HUE_RED,HUE_BLUE), 1f, 1f);
           attributesList.add( AnalyticSurface.createGridPointAttributes(ele,color ,1)); 
           lon += lonStep;
       }
       lat += latStep;
   } 
  lat = surface.getSector().getMaxLatitude().degrees; 
  surface.setValues(attributesList);
   
   
   
   
   
// create the color legend
     final double altitude = surface.getAltitude();
     final double verticalScale = surface.getVerticalScale();          
     Format legendLabelFormat = new DecimalFormat("# km")
     {
         public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition)
         {
             double altitudeMeters = altitude + verticalScale * number;
             double altitudeKm = altitudeMeters * WWMath.METERS_TO_KILOMETERS;
             return super.format(altitudeKm, result, fieldPosition);
         }
     };
     AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(minValue, maxValue, HUE_RED, HUE_BLUE,
         AnalyticSurfaceLegend.createDefaultColorGradientLabels(minValue, maxValue, legendLabelFormat),
         AnalyticSurfaceLegend.createDefaultTitle("City Altitudes"));
     
     legend.setOpacity(0.8);
     legend.setScreenLocation(new Point(50, 300));
     legend.setClientLayer(renderableLayer);
     renderableLayer.addRenderable(createLegendRenderable(surface, 300, legend));
	
	//delete the selector
 }

Renderable createLegendRenderable(final AnalyticSurface surface, final double surfaceMinScreenSize,
	        final AnalyticSurfaceLegend legend)
	    {
	        return new Renderable()
	        {
	            public void render(DrawContext dc)
	            {
	                Extent extent = surface.getExtent(dc);
	                if (!extent.intersects(dc.getView().getFrustumInModelCoordinates()))
	                    return;

	                if (WWMath.computeSizeInWindowCoordinates(dc, extent) < surfaceMinScreenSize)
	                    return;

	                legend.render(dc);
	            }
	        };
	    }







} //end of class


