
package liquinth;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class Liquinth extends JPanel {
	public static final String VERSION = "a27 (20070628)";

	public Liquinth() {
		JPanel logo_panel;
		ImageIcon logo;
		Synthesizer synthesizer = new Synthesizer( Player.SAMPLING_RATE );
		Player player = new Player( synthesizer );
		logo_panel = new JPanel( new BorderLayout() );
		logo_panel.setBackground( Color.BLACK );
		logo_panel.setBorder( new BevelBorder( BevelBorder.LOWERED, Color.WHITE, Color.GRAY ) );
		logo = new ImageIcon( getClass().getResource( "liquinth.png" ) );
		logo_panel.add( new JLabel( logo ), BorderLayout.CENTER );
		AudioSelector audio_selector = new AudioSelector( player );
		ControlPanel control_panel = new ControlPanel( synthesizer );
		MidiReceiver midi_receiver = new MidiReceiver( synthesizer, control_panel);
		MidiSelector midi_selector = new MidiSelector( midi_receiver );
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout( new GridBagLayout() );
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		add( audio_selector, gbc );
		add( midi_selector, gbc );
		gbc.weighty = 1;
		add( logo_panel, gbc );
		gbc.weighty = 0;
		add( control_panel, gbc );
		setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
	}

	public static void main( String[] args ) {
		System.out.println( "Liquinth " + VERSION );
		JFrame frame = new JFrame( "Liquinth (c)2007 mumart@gmail.com" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( new Liquinth() );
		frame.pack();
		frame.setVisible( true );
	}
}
