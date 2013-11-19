
package jvst.examples.liquinth;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class LiquinthJS extends JPanel {
	public LiquinthJS() {
		Liquinth liquinth = new Liquinth( Player.SAMPLING_RATE * Player.OVERSAMPLE );
		Player player = new Player( liquinth );
		AudioSelector audioSelector = new AudioSelector( player );
		SynthesizerPanel synthPanel = new SynthesizerPanel( liquinth );
		MidiReceiver midiReceiver = new MidiReceiver( synthPanel );
		MidiSelector midiSelector = new MidiSelector( midiReceiver );
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout( new GridBagLayout() );
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		add( audioSelector, gbc );
		add( midiSelector, gbc );
		gbc.weighty = 1;
		add( new LogoPanel(), gbc );
		gbc.weighty = 0;
		add( synthPanel, gbc );
		setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
	}

	public static void main( String[] args ) {
		final String title = Liquinth.VERSION + " " + Liquinth.AUTHOR;
		System.out.println( title );
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				JFrame frame = new JFrame( title );
				frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
				frame.getContentPane().add( new LiquinthJS() );
				frame.pack();
				frame.setVisible( true );
			}
		} );
	}
}
