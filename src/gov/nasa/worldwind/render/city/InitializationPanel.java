package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.examples.GazetteerPanel;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
public class InitializationPanel extends JPanel implements ActionListener{
    private     CityBuilder.AppFrame appFrame;
    ViewDisplay viewDisplay; 
    public InitializationPanel(CityBuilder.AppFrame appFrame)
    {   
		this.appFrame=appFrame;
        this.initComponents();
    }
    
    public class ViewDisplay extends JPanel implements PositionListener, RenderingListener, ActionListener
    {
    	private     CityBuilder.AppFrame appFrame;
        // Units constants
        public final static String UNIT_METRIC = "gov.nasa.worldwind.StatusBar.Metric";
        public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.StatusBar.Imperial";
        
        //private final static double METER_TO_FEET = 3.280839895;
        private final static double METER_TO_MILE = 0.000621371192;

        
        private WorldWindow eventSource;
        protected final JLabel latDisplay = new JLabel("");
        protected final JLabel lonDisplay = new JLabel(Logging.getMessage("term.OffGlobe"));
        protected final JLabel eleDisplay = new JLabel("");
        protected final JLabel headingDisplay = new JLabel("");
        protected final JLabel pitchDisplay = new JLabel("");
        private String elevationUnit = UNIT_METRIC;
        private String angleFormat = Angle.ANGLE_FORMAT_DD;

        // Viewer class information.  view and inputHandler member variables are lazy initialized.
        // Constructor gets the class names for the View, ViewInputHandler pair.
        public class ViewerClass
        {
            protected String viewClassName;
            protected String inputHandlerClassName;
            protected View view;
            protected ViewInputHandler viewInputHandler;
            ViewerClass(String viewClassName, String inputHandlerClassName)
            {
                this.viewClassName = viewClassName;
                this.inputHandlerClassName = inputHandlerClassName;
                this.view = null;
                this.viewInputHandler = null;
            }
        }

        
        
        // Maps the combo box label to class information.
        public class ViewerClassMap extends HashMap<String, ViewerClass>
        {
        }
        
        public ViewerClassMap classNameList = new ViewerClassMap();

        // Orbit view class information
        public ViewerClass orbitViewer = new ViewerClass(
            "gov.nasa.worldwind.view.orbit.BasicOrbitView",
            "gov.nasa.worldwind.view.orbit.OrbitViewInputHandler");
        // Fly viewer class information
        public ViewerClass flyViewer = new ViewerClass(
            "gov.nasa.worldwind.view.firstperson.BasicFlyView",
            "gov.nasa.worldwind.view.firstperson.FlyViewInputHandler");

        // Viewer class array used for loop that initializes the map.
        ViewerClass[] viewerClasses =
            {
        		orbitViewer,
        		flyViewer
               
            };

        // Viewer names for the combo box
        String[] viewerNames = { "Orbit","Fly"};
        String currentName;
        DefaultComboBoxModel viewerClassNames;
        JComboBox viewList;

        // The class currently being used.
        ViewerClass currentViewer = null;
        public ViewDisplay(CityBuilder.AppFrame appFrame)
        {   
            super(new GridLayout(0, 1));
             
            this.appFrame=appFrame;
            // Initialize the viewer label -> viewer class map
            for (int i = 0; i < 2; i++)
            {
                classNameList.put(viewerNames[i], viewerClasses[i]);
            }

            setViewer(viewerClasses[0], false);
            currentName = viewerNames[0];
            currentViewer = viewerClasses[0];

            // Set up the combo box for choosing viewers
            viewerClassNames = new DefaultComboBoxModel();
            viewList = new JComboBox(viewerNames);
            viewList.addActionListener(this);

            // Set up the viewer parameter display
            headingDisplay.setHorizontalAlignment(SwingConstants.CENTER);
            pitchDisplay.setHorizontalAlignment(SwingConstants.CENTER);
            latDisplay.setHorizontalAlignment(SwingConstants.CENTER);
            lonDisplay.setHorizontalAlignment(SwingConstants.CENTER);
            eleDisplay.setHorizontalAlignment(SwingConstants.CENTER);

            this.add(viewList);
            try {
                this.add(new GazetteerPanel(this.appFrame.getWwd(),
                "gov.nasa.worldwind.poi.YahooGazetteer"),  SwingConstants.CENTER);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error creating Gazetteer");
            }
            this.add(latDisplay);
            this.add(lonDisplay);
            this.add(eleDisplay);
            this.add(headingDisplay);
            this.add(pitchDisplay);

        }

        public void setViewer(ViewerClass vc, boolean copyValues)
        {
            if (vc.view == null)
            {
                vc.view = (View) WorldWind.createComponent(vc.viewClassName);
                vc.viewInputHandler =
                    vc.view.getViewInputHandler();
            }
            if (copyValues)
            {
                View viewToCopy = this.appFrame.getWwd().getView();

                try {
                    vc.view.copyViewState(viewToCopy);
                    this.appFrame.getWwd().setView(vc.view);
                }
                catch (IllegalArgumentException iae)
                {
                    JOptionPane.showMessageDialog(this,
                        "Cannot switch to new view from this position/orientation");
                    viewList.setSelectedItem(currentName);
                }
            }
            else
            {
            	this.appFrame.getWwd().setView(vc.view);
            }

        }

        public void actionPerformed(ActionEvent event)
        {
            if (event.getSource() == viewList)
            {
                String classLabel = (String) viewList.getSelectedItem();
                ViewerClass vc = classNameList.get(classLabel);

                setViewer(vc, true);
            }
        }

        public void moved(PositionEvent event)
        {

        }


        public void setEventSource(WorldWindow newEventSource)
        {
            if (this.eventSource != null)
            {
                this.eventSource.removePositionListener(this);
                this.eventSource.removeRenderingListener(this);
            }

            if (newEventSource != null)
            {
                newEventSource.addPositionListener(this);
                newEventSource.addRenderingListener(this);
            }

            this.eventSource = newEventSource;
        }

        protected String makeEyeAltitudeDescription(double metersAltitude)
        {
            String s;
            String altitude = Logging.getMessage("term.Altitude");
            if (UNIT_IMPERIAL.equals(elevationUnit))
                s = String.format(altitude + " %,7d mi", (int) Math.round(metersAltitude * METER_TO_MILE));
            else // Default to metric units.
                s = String.format(altitude + " %,7d m", (int) Math.round(metersAltitude));
            return s;
        }

        protected String makeAngleDescription(String label, Angle angle)
        {
            String s;
            if (Angle.ANGLE_FORMAT_DMS.equals(angleFormat))
                s = String.format("%s %s", label, angle.toDMSString());
            else
                s = String.format("%s %7.4f\u00B0", label, angle.degrees);
            return s;
        }

        public void stageChanged(RenderingEvent event)
        {
            if (!event.getStage().equals(RenderingEvent.BEFORE_BUFFER_SWAP))
                return;

            EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {

                    if (eventSource.getView() != null && eventSource.getView().getEyePosition() != null)
                    {
                        Position newPos = eventSource.getView().getEyePosition();

                        if (newPos != null)
                        {
                            String las = makeAngleDescription("Lat", newPos.getLatitude());
                            String los = makeAngleDescription("Lon", newPos.getLongitude());
                            String heading = makeAngleDescription("Heading", eventSource.getView().getHeading());
                            String pitch = makeAngleDescription("Pitch", eventSource.getView().getPitch());

                            latDisplay.setText(las);
                            lonDisplay.setText(los);
                            eleDisplay.setText(makeEyeAltitudeDescription(
                                newPos.getElevation()));
                            headingDisplay.setText(heading);
                            pitchDisplay.setText(pitch);
                        }
                        else
                        {
                            latDisplay.setText("");
                            lonDisplay.setText(Logging.getMessage("term.OffGlobe"));
                            eleDisplay.setText("");
                            pitchDisplay.setText("");
                            headingDisplay.setText("");
                        }

                    }
                    else
                    {
                        eleDisplay.setText(Logging.getMessage("term.Altitude"));
                    }
                }
            });
        }
    }
    
    private void initComponents()
    {
    	
        this.add(makeControlPanel());
        viewDisplay.setEventSource(this.appFrame.getWwd());

    	
    	
    }
    
    
    private JPanel makeControlPanel()
    {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(
            new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9),
                new TitledBorder("View Controls")));
        controlPanel.setToolTipText("Select active view controls");
        viewDisplay = new ViewDisplay(this.appFrame);
        controlPanel.add(viewDisplay);
        return(controlPanel);
    }
    

	public void actionPerformed(ActionEvent e) {
    }
	

}
