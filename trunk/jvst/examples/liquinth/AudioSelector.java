
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
import javax.sound.sampled.Mixer;

public class AudioSelector extends JPanel {
	private Player player;

	public AudioSelector( Player player ) {
		setLayout( new GridBagLayout() );

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		add( new JLabel( "Audio Device" ), gbc );
		JComboBox<Object> combo = new JComboBox<Object>( AudioSystem.getMixerInfo() );
		combo.addItemListener( new ComboListener() );
		gbc.weightx = 1;
		add( combo, gbc );

		this.player = player;
		setMixer( ( Mixer.Info ) combo.getSelectedItem() );
	}

	private void setMixer( Mixer.Info mixerInfo ) {
		player.stop();
		player.setMixer( AudioSystem.getMixer( mixerInfo ) );
		new Thread( player ).start();
	}

	private class ComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				setMixer( ( Mixer.Info ) e.getItem() );
			}
		}
	}
}
