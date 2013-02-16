
package jvst.examples.liquinth;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.sampled.*;

public class AudioSelector extends JPanel {
	private Player player;

	public AudioSelector( Player player ) {
		JComboBox combo;
		GridBagLayout gbl;
		GridBagConstraints gbc;

		gbl = new GridBagLayout();
		setLayout( gbl );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		add( new JLabel( "Audio Device" ), gbc );
		combo = new JComboBox( AudioSystem.getMixerInfo() );
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
