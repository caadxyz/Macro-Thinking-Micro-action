package GUI;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;

public class Algorithm_panel extends JPanel{
	
        Algorithm_panel(){
		
		JButton button=new JButton();
		button.setName("steed");
		button.setText("steed");
		button.setVisible(true);
		
		Dimension dimension=new Dimension(800,600);
		this.setSize(dimension);
		this.setLayout(null);
		this.add(button);
		button.setLocation(300, 300);

        }
	

        
}
