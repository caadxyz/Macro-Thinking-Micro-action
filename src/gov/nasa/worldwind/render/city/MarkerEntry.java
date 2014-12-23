package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.airspaces.AirspaceAttributes;
import gov.nasa.worldwind.render.airspaces.editor.CityPolygonEditor;
import gov.nasa.worldwind.render.markers.MarkerAttributes;

public class MarkerEntry extends WWObjectImpl
{
    private ActivityMarker marker;
    private ActivityMarkerEditor editor;
    private MarkerAttributes attributes;
    
    
    
    private boolean editing = false;
    private boolean selected = false;
   

    public MarkerEntry(ActivityMarker marker, ActivityMarkerEditor markerEditor)
    {
        this.marker = marker;
        this.editor = markerEditor;
        this.attributes=this.marker.getAttributes();
        
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

 

    public String getLength()
    {
        return this.getStringValue("ActivityMarker.length");
    }

    public void setLength()
    {
        this.setValue("ActivityMarker.length", "hello world");
    }

    public String getName()
    {
        return this.getStringValue(AVKey.DISPLAY_NAME);
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }
    
    
    public ActivityMarker getMarker()
    {
        return marker;
    }

    public ActivityMarkerEditor getEditor()
    {
        return editor;
    }

    public MarkerAttributes getAttributes()
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
            value = this.marker.getValue(key);
        }
        return value;
    }

    public Object setValue(String key, Object value)
    {
        //noinspection StringEquality
        if (key == AVKey.DISPLAY_NAME)
        {
            return this.marker.setValue(key, value);
        }
        else if(key == "ActivityMarker.length"){
        	return this.marker.setValue(key, value);
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
            this.marker.setAttributes(this.getAttributes());
        }
    }


}