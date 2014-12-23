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


class open_save extends ApplicationTemplate.AppFrame
{
   
    static String OPEN="open";
    static String SAVE="save";
    Iterable<Test_Polyline> polylines;
    Controller builderController;
	public open_save()
    {   
		double elevation = 10e3;
        ArrayList<Position> positions = new ArrayList<Position>();
        positions.add(new Position(Angle.fromDegrees(37.8484), Angle.fromDegrees(-119.9754), elevation));
        positions.add(new Position(Angle.fromDegrees(39.3540), Angle.fromDegrees(-110.1526), elevation));
        positions.add(new Position(Angle.fromDegrees(38.3540), Angle.fromDegrees(-100.1526), elevation));

        ArrayList<Position> positions2 = new ArrayList<Position>();
        positions2.add(new Position(Angle.fromDegrees(0), Angle.fromDegrees(-150), elevation));
        positions2.add(new Position(Angle.fromDegrees(25), Angle.fromDegrees(-75), elevation));
        positions2.add(new Position(Angle.fromDegrees(50), Angle.fromDegrees(0), elevation));
        
		Test_Polyline polyline01=new Test_Polyline(positions);
		Test_Polyline polyline02=new Test_Polyline(positions2);
		ArrayList<Test_Polyline> polys= new ArrayList<Test_Polyline>();
		polys.add(polyline01);
		polys.add(polyline02);
		
		polylines =polys;		
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
    	private open_save frame;
    	Controller(open_save frame){
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
                    final ArrayList<Test_Polyline> polylines = new ArrayList<Test_Polyline>();
                    try
                    {   
                        File[] files = dir.listFiles(new FilenameFilter()
                        {
                            public boolean accept(File dir, String name)
                            {
                                return name.startsWith("gov.nasa.worldwind.render.Test_Polyline") && name.endsWith(".xml");
                            }
                        });
                        
                        System.out.println(files.length);
                        for (File file : files)
                        {
                            String[] name = file.getName().split("-");
                            
                            try
                            {   
                                Class c = Class.forName(name[0]);
                                Test_Polyline polyline = (Test_Polyline) c.newInstance();
                                BufferedReader input = new BufferedReader(new FileReader(file));
                                String s = input.readLine();
                                polyline.restoreState(s);
                                polylines.add(polyline);

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
                                RenderableLayer layer_mhd = new RenderableLayer();
                                frame.getWwd().getModel().getLayers().add(layer_mhd);
                               for (Test_Polyline i : polylines){layer_mhd.addRenderable(i);
                               String xmlString = i.getRestorableState();
                               System.out.println(xmlString);
                               System.out.println(i.getPositions());
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

            final Iterable<Test_Polyline> entries = this.frame.polylines;

            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        java.text.DecimalFormat f = new java.text.DecimalFormat("####");
                        f.setMinimumIntegerDigits(4);
                        int counter = 0;

                        for (Test_Polyline entry : entries)
                        {
                        	Test_Polyline a =entry;
                                               
                            String xmlString = a.getRestorableState();
                            if (xmlString != null)
                            {
                                try
                                {
                                    PrintWriter of = new PrintWriter(new File(dir,
                                        a.getClass().getName() + "-" + f.format(counter++) + ".xml"));
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
    	
    	ApplicationTemplate.start("open_save", open_save.class);
    } 
    
}