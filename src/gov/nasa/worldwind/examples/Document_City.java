package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwind.render.Test_Polyline;
import gov.nasa.worldwind.util.WWIO;

import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;


class Document_City extends ApplicationTemplate.AppFrame
{
   
    static String OPEN="open";
    static String SAVE="save";
    Iterable<City_Test> elements;
    Controller builderController;
	public Document_City()
    {   
		ArrayList<City_Test> elements=new ArrayList<City_Test>();
		for(int i=0; i<5; i++) {
		City_Test element=new City_Test();
		element.setValue(i);
		element.setName("steed"+i);
		element.setInsideClass(i, i+1, i+2);
		element.setInsideClass();
		element.setInsideClass(i, i, i);
		elements.add(element);
		this.elements=elements;

		}
				
		Controller builderController= new Controller(this);
		this.builderController=builderController;
        makeMenuBar(this,this.builderController);
    }
	
	
    public static void makeMenuBar(JFrame frame,Controller controller)
    {
        JMenuBar menuBar = new JMenuBar();
       
      
        JMenu menu = new JMenu("File");
        {
            JMenuItem item = new JMenuItem("Open...");
            item.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            item.setActionCommand(OPEN);
            item.addActionListener(controller);
            menu.add(item);


            item = new JMenuItem("Save...");
            item.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
           item.setActionCommand(SAVE);
           item.addActionListener(controller);
            menu.add(item);

        }
        
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);
    }
    
    static class Controller extends WWObjectImpl implements ActionListener{
    	private JFileChooser fileChooser;
    	private Document_City frame;
    	Controller(Document_City frame){
    		this.frame=frame;
      };
        public void actionPerformed(ActionEvent e)
        {
        	if (e.getActionCommand() == OPEN)
            {
                this.openFromFile();
            }
            else if (e.getActionCommand() == SAVE)
            {
                this.saveToFile();
            }
           
        }	
        void openFromFile(){
        	if (this.fileChooser == null)
            {
                this.fileChooser = new JFileChooser();
                this.fileChooser.setCurrentDirectory(new File(Configuration.getUserHomeDirectory()));
            }

            this.fileChooser.setDialogTitle("Choose Polyline File Directory");
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            this.fileChooser.setMultiSelectionEnabled(false);
            int status = this.fileChooser.showOpenDialog(null);
            if (status != JFileChooser.APPROVE_OPTION)
                return;

            final File dir = this.fileChooser.getSelectedFile();
            if (dir == null)
                return;

            Thread t = new Thread(new Runnable()
            {  
            	 
                public void run()
                {   
                    final ArrayList<City_Test> elements = new ArrayList<City_Test>();
                    try
                    {   
                        File[] files = dir.listFiles(new FilenameFilter()
                        {
                            public boolean accept(File dir, String name)
                            {
                                return name.startsWith("gov.nasa.worldwind.examples.City_Test") && name.endsWith(".xml");
                            }
                        });
                        
                        System.out.println(files.length);
                        for (File file : files)
                        {
                            String[] name = file.getName().split("-");
                            
                            try
                            {   
                                Class c = Class.forName(name[0]);
                                City_Test element = (City_Test) c.newInstance();
                                BufferedReader input = new BufferedReader(new FileReader(file));
                                String s = input.readLine();
                                element.restoreState(s);
                                elements.add(element);

                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                               
                                for (City_Test i : elements){
                               System.out.println("name: "+i.name);
                               System.out.println("value:"+i.value);
                                  for (int j=0; j<i.inClasses.size(); j++){
                                  System.out.println("inClass_a:   "+i.inClasses.get(j).a);
                                  System.out.println("inClass_b:   "+i.inClasses.get(j).b);
                                  System.out.println("inClass_c:   "+i.inClasses.get(j).c);
                                  }
                               }
                              
                             }
                        });
                    }
                }
            });
            t.start();
        }
        void saveToFile(){
        	
            if (this.fileChooser == null)
            {
                this.fileChooser = new JFileChooser();
                this.fileChooser.setCurrentDirectory(new File(Configuration.getUserHomeDirectory()));
            }

            this.fileChooser.setDialogTitle("Choose Directory to Place Polylines");
            this.fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            this.fileChooser.setMultiSelectionEnabled(false);
            int status = this.fileChooser.showSaveDialog(null);
            if (status != JFileChooser.APPROVE_OPTION)
                return;

            final File dir = this.fileChooser.getSelectedFile();
            if (dir == null)
                return;

            if (dir.exists())
            {
                try
                {
                    WWIO.deleteDirectory(dir);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            if (!dir.exists())
            {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }

            final Iterable<City_Test> entries = this.frame.elements;

            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        java.text.DecimalFormat f = new java.text.DecimalFormat("####");
                        f.setMinimumIntegerDigits(4);
                        int counter = 0;

                        for (City_Test entry : entries)
                        {
                        	String xmlString = entry.getRestorableState();
                            if (xmlString != null)
                            {
                                try
                                {
                                    PrintWriter of = new PrintWriter(new File(dir,
                                    		entry.getClass().getName() + "-" + f.format(counter++) + ".xml"));
                                    of.write(xmlString);
                                    of.flush();
                                    of.close();
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    finally
                    {
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                            }
                        });
                    }
                }
            });           
            t.start();
        }
    }
    
    
    public static void main(String[] args)
    {
    	
    	ApplicationTemplate.start("Document_City", Document_City.class);
    } 
    
}