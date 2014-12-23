package gov.nasa.worldwind.examples;


import java.util.ArrayList;

import gov.nasa.worldwind.Restorable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.RestorableSupport;


public class City_Test implements Restorable{

	int value=0;
	String name="Test One";	
	ArrayList<InsideClass> inClasses=new ArrayList<InsideClass>();
	void setInsideClass(int a, int  b, int c){
		inClasses.add(new InsideClass(a,b,c));
     }
	void setInsideClass(){
		inClasses.add(new InsideClass());
	}
	void setValue(int value){this.value=value;}
	void setName(String name){this.name=name;}
	
	
	public String getRestorableState() {
		
		RestorableSupport restorableSupport = RestorableSupport.newRestorableSupport();
        // Creating a new RestorableSupport failed. RestorableSupport logged the problem, so just return null.
        if (restorableSupport == null)
            return null;
        
        
        restorableSupport.addStateValueAsInteger("value", this.value);
        restorableSupport.addStateValueAsString("name",this.name);
        
        
        if (this.inClasses != null)
        {
            
            RestorableSupport.StateObject inClassStateObj = restorableSupport.addStateObject("inClasses");
            if (inClassStateObj != null)
            {
                for (InsideClass i : this.inClasses)
                {
                    
                    if (i != null)
                    {
                        // Create a nested "inClasses" element underneath the base "inClasses".
                        RestorableSupport.StateObject iStateObj =
                            restorableSupport.addStateObject(inClassStateObj, "inClass");
                        if (iStateObj != null)
                        {
                            restorableSupport.addStateValueAsInteger(iStateObj, "a",
                                    i.a);
                            restorableSupport.addStateValueAsInteger(iStateObj, "b",
                                    i.b);
                            restorableSupport.addStateValueAsInteger(iStateObj, "c",
                                    i.c);
                        }
                    }
                }
            }
        }
        
        
        
        return restorableSupport.getStateAsXml();
	}

	public void restoreState(String stateInXml) {
        if (stateInXml == null)
        {
            String message = Logging.getMessage("nullValue.StringIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        RestorableSupport restorableSupport;
        try
        {
            restorableSupport = RestorableSupport.parse(stateInXml);
        }
        catch (Exception e)
        {
            // Parsing the document specified by stateInXml failed.
            String message = Logging.getMessage("generic.ExceptionAttemptingToParseStateXml", stateInXml);
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message, e);
        }
        
        
        Integer value = restorableSupport.getStateValueAsInteger("value");
        if (value != null)
            this.setValue(value);
        
        String name = restorableSupport.getStateValueAsString("name");
        if (name != null)
            this.setName(name);
        
        
        // Get the base "positions" state object.
        RestorableSupport.StateObject inClassStateObj = restorableSupport.getStateObject("inClasses");
        if (inClassStateObj!= null)
        {
           
            // Get the nested "position" states beneath the base "positions".
            RestorableSupport.StateObject[] inClassStateArray =
                restorableSupport.getAllStateObjects(inClassStateObj, "inClass");
            if (inClassStateArray  != null && inClassStateArray .length != 0)
            {
                for (RestorableSupport.StateObject iStateObj : inClassStateArray)
                {
                    if (iStateObj != null)
                    {
                        // Restore each position only if all parts are available.
                        // We will not restore a partial position (for example, just the elevation).
                        int a = restorableSupport.getStateValueAsInteger(iStateObj, "a");
                        int b = restorableSupport.getStateValueAsInteger(iStateObj, "b");
                        int c = restorableSupport.getStateValueAsInteger(iStateObj, "c");                        
                        this.setInsideClass(a, b, c);
                    }
                }
            }

        }
        
		
	
	
	
	
	
	
	
	
	}
	
static class InsideClass{
	int a=0;
	int b=1;
	int c=2;
	InsideClass(){}
	InsideClass(int a, int b, int c){
		this.a=a;
		this.b=b;
		this.c=c;
	}
	
}

}
