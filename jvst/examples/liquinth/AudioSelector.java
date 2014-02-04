
package jvst.examples.liquinth;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

public class AudioSelector extends JPanel {
	private Synthesizer synthesizer;
	private Player player;
	private Thread thread;

	public AudioSelector( Synthesizer synth ) {
		setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		add( new JLabel( "Audio Device" ), gbc );
		JComboBox<Object> combo = new JComboBox<Object>( AudioSystem.getMixerInfo() );
		combo.addItemListener( new ComboListener() );
		gbc.weightx = 1;
		add( combo, gbc );

		synthesizer = synth;
		setMixer( ( Mixer.Info ) combo.getSelectedItem() );
	}

	private void setMixer( Mixer.Info mixerInfo ) {
		try {
			if( player != null ) {
				player.stop();
			}
			if( thread != null ) {
				while( thread.isAlive() ) {
					try { thread.join(); } catch( InterruptedException ie ) {}
				}
			}
			player = new Player( synthesizer, AudioSystem.getMixer( mixerInfo ) );
			thread = new Thread( player );
			thread.start();
		} catch( LineUnavailableException e ) {
			System.err.println( "Unable to open audio device: " + e.getMessage() );
		}
	}

	private class ComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				setMixer( ( Mixer.Info ) e.getItem() );
			}
		}
	}
}
