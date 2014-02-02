
package jvst.examples.liquinth;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

public class LogoPanel extends JPanel {
	public LogoPanel() {
		setLayout( new BorderLayout() );
		setBackground( Color.BLACK );
		setBorder( new BevelBorder( BevelBorder.LOWERED, Color.WHITE, Color.GRAY ) );
		ImageIcon logo = new ImageIcon( getClass().getResource( "liquinth.png" ) );
		add( new JLabel( logo ), BorderLayout.CENTER );
	}
}
