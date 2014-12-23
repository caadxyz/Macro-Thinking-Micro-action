package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.editor.CityPolygonEditor;
import gov.nasa.worldwind.render.airspaces.editor.PolygonEditor;
import gov.nasa.worldwind.render.airspaces.editor.PolylineEditor;
//Haidong Ma
//**************************************************************//
//********************  Polyline Builder Model  ****************//
//**************************************************************//

public class PolygonEntry extends WWObjectImpl
{
    private CityPolygon polygon;
    private CityPolygonEditor editor;
    private AirspaceAttributes attributes;
    
    
    private boolean editing = false;
    private boolean selected = false;
   

    public PolygonEntry(CityPolygon polylgon, CityPolygonEditor cityPolygonEditor)
    {
        this.polygon = polylgon;
        this.editor =  cityPolygonEditor;
        this.attributes=this.polygon.getAttributes();
        
    }

    public boolean isEditing()
    {
        return this.editing;
    }

    public void setEditing(boolean editing)
    {
        this.editing = editing;
        this.updateAttributes();
    }

    public boolean isSelected()
    {
        return this.selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
        this.updateAttributes();
     }

 
    public String getGid()
    {
    	return this.getStringValue("cityPolygon.gid");
    }
    
    public void setGid(String gid)
    {
    	this.setValue("cityPolygon.gid", gid);
    }
    
    public String getColor()
    {
        return this.getStringValue("cityPolygon.color");
    }

    public void setColor(String color)
    {
        this.setValue("cityPolygon.color", color);
    }
    

    public String getLength()
    {
        return this.getStringValue("cityPolygon.length");
    }

    public void setLength()
    {
        this.setValue("cityPolygon.length", "hello world");
    }

    public String getName()
    {
        return this.getStringValue(AVKey.DISPLAY_NAME);
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }
    
    
    public CityPolygon getPolygon()
    {
        return polygon;
    }

    public CityPolygonEditor getEditor()
    {
        return editor;
    }

    public AirspaceAttributes getAttributes()
    {
        return this.attributes;
    }
    public String toString()
    {
        return this.getName();
    }

    public Object getValue(String key)
    {
        Object value = super.getValue(key);
        if (value == null)
        {
            value = this.polygon.getValue(key);
        }
        return value;
    }

    public Object setValue(String key, Object value)
    {
        //noinspection StringEquality
        if (key == AVKey.DISPLAY_NAME)
        {
            return this.polygon.setValue(key, value);
        }
        else if(key == "cityPolygon.length"){
        	return this.polygon.setValue(key, value);
        }
        else if(key == "cityPolygon.gid"){
        	return this.polygon.setValue(key, value);
        }
        else if(key == "cityPolygon.color"){
        	return this.polygon.setValue(key, value);
        }
        
        else
        {
            return super.setValue(key, value);
        }
    }
    
    
    protected void updateAttributes()
    {
        if (this.isSelected())
        {
            
        	
        	//this.polygon.setAttributes(CityBuilder.getSelectionAttributes());
        
        
        
        }
       
        else
        {
            this.polygon.setAttributes(this.getAttributes());
        }
    }


}