
package jvst.examples.liquinth;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

public class MidiSelector extends JPanel {
	private MidiReceiver midiReceiver;
	private MidiDevice midiDevice;

	public MidiSelector( MidiReceiver receiver ) {
		midiReceiver = receiver;
		receiver.setChannel( 1 );

		setLayout( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		add( new JLabel( "Midi Device" ), gbc );
		JComboBox<Object> combo = new JComboBox<Object>( MidiSystem.getMidiDeviceInfo() );
		combo.insertItemAt( "None", 0 );
		combo.setSelectedIndex( 0 );
		combo.addItemListener( new DevComboListener() );
		gbc.weightx = 1;
		add( combo, gbc );

		gbc.weightx = 0;
		add( new JLabel( "Channel" ), gbc );
		combo = new JComboBox<Object>( new Integer[] {
			new Integer(  1 ), new Integer(  2 ), new Integer(  3 ), new Integer(  4 ),
			new Integer(  5 ), new Integer(  6 ), new Integer(  7 ), new Integer(  8 ),
			new Integer(  9 ), new Integer( 10 ), new Integer( 11 ), new Integer( 12 ),
			new Integer( 13 ), new Integer( 14 ), new Integer( 15 ), new Integer( 16 )	
		} );
		combo.addItemListener( new ChanComboListener() );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		add( combo, gbc );
	}

	private class DevComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			Object item;
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				if( midiDevice != null ) {
					midiDevice.close();
				}
				item = e.getItem();
				if( item instanceof MidiDevice.Info ) {
					try {
						midiDevice = MidiSystem.getMidiDevice( ( MidiDevice.Info ) item );
						midiDevice.open();
						midiDevice.getTransmitter().setReceiver( midiReceiver );
					} catch( MidiUnavailableException exception ) {
						System.out.println( "MidiSelector: Can't open MIDI device!" );
					}
				} else {
					midiDevice = null;
				}
			}
		}
	}

	private class ChanComboListener implements ItemListener {
		public void itemStateChanged( ItemEvent e ) {
			if( e.getStateChange() == ItemEvent.SELECTED ) {
				Integer chan;
				chan = ( Integer ) e.getItem();
				midiReceiver.setChannel( chan.intValue() );
			}
		}
	}
}
