
package liquinth;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.sound.midi.*;

public class MidiSelector extends JPanel {
	private MidiReceiver midi_receiver;
	private MidiDevice midi_device;

	public MidiSelector( MidiReceiver receiver ) {
		JComboBox combo;
		GridBagLayout gbl;
		GridBagConstraints gbc;

		midi_receiver = receiver;
		receiver.set_channel( 1 );

		setLayout( new GridBagLayout() );
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		add( new JLabel( "Midi Device" ), gbc );
		combo = new JComboBox( MidiSystem.getMidiDeviceInfo() );
		combo.insertItemAt( "None", 0 );
		combo.setSelectedIndex( 0 );
		combo.addItemListener( new DevComboListener() );
		gbc.weightx = 1;
		add( combo, gbc );

		gbc.weightx = 0;
		add( new JLabel( "Channel" ), gbc );
		combo = new JComboBox( new Integer[] {
			1, 2, 3, 4, 5, 6, 7, 8,
			9,10,11,12,13,14,15,16
		} );
		combo.addItemListener( new ChanComboListener() );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		add( combo, gbc );
	}

	private class DevComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			Object item;
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				if( midi_device != null ) {
					midi_device.close();
				}
				item = e.getItem();
				if( item instanceof MidiDevice.Info ) {
					try {
						midi_device = MidiSystem.getMidiDevice( ( MidiDevice.Info ) item );
						midi_device.open();
						midi_device.getTransmitter().setReceiver( midi_receiver );
					} catch( MidiUnavailableException exception ) {
						System.out.println( "MidiSelector: Can't open MIDI device!" );
					}
				} else {
					midi_device = null;
				}
			}
		}
	}

	private class ChanComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				Integer chan;
				chan = ( Integer ) e.getItem();
				midi_receiver.set_channel( chan.intValue() );
			}
		}
	}
}
