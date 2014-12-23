package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.airspaces.editor.PolylineEditor;

//**************************************************************//
//********************  Polyline Builder Model  ****************//
//**************************************************************//

public class PolylineEntry extends WWObjectImpl
{
    private CityPolyline polyline;
    private PolylineEditor editor;
    private ShapeAttributes attributes;
    
    
    private boolean editing = false;
    private boolean selected = false;
   

    public PolylineEntry(CityPolyline polyline, PolylineEditor editor)
    {
        this.polyline = polyline;
        this.editor = editor;
        this.attributes=this.polyline.getAttributes();
        
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
    	return this.getStringValue("cityPline.gid");
    }
    
    public void setGid(String gid)
    {
    	this.setValue("cityPline.gid", gid);
    }
    
    
    public String getName()
    {
        return this.getStringValue(AVKey.DISPLAY_NAME);
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }

    public String getLength()
    {
        return this.getStringValue("cityPline.length");
    }

    public void setLength(String length)
    {
        this.setValue("cityPline.length", length);
    }


    public String getWidth()
    {
        return this.getStringValue("cityPline.width");
    }

    public void setWidh(String width)
    {
        this.setValue("cityPline.width", width);
    }
    
  
    public String getColor()
    {
        return this.getStringValue("cityPline.color");
    }

    public void setColor(String color)
    {
        this.setValue("cityPline.color", color);
    }
    
    
    public String getType()
    {
        return this.getStringValue("cityPline.type");
    }

    public void setType(String type)
    {
        this.setValue("cityPline.type", type);
    }
    
    
    
    public CityPolyline getPolyline()
    {
        return polyline;
    }

    public PolylineEditor getEditor()
    {
        return editor;
    }

    public ShapeAttributes getAttributes()
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
            value = this.polyline.getValue(key);
        }
        return value;
    }

    public Object setValue(String key, Object value)
    {
        //noinspection StringEquality
        if (key == AVKey.DISPLAY_NAME)
        {
            return this.polyline.setValue(key, value);
        }
        
        else if(key == "cityPline.length"){
        	return this.polyline.setValue(key, value);
        }
        
        else if(key == "cityPline.gid"){
        	return this.polyline.setValue(key, value);
        }
        
        else if(key == "cityPline.width"){
        	return this.polyline.setValue(key, value);
        }
        
        else if(key == "cityPline.color"){
        	return this.polyline.setValue(key, value);
        }
        
        else if(key == "cityPline.type"){
        	return this.polyline.setValue(key, value);
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
            
        	
        	this.polyline.setAttributes(CityBuilder.getSelectionPlAttributes());
        
        
        
        }
       
        else
        {
            this.polyline.setAttributes(this.getAttributes());
        }
    }


}