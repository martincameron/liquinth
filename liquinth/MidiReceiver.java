
package liquinth;

import javax.sound.midi.*;

public class MidiReceiver implements Receiver {
	private Synthesizer synthesizer;
	private ControlPanel control_panel;
	private int midi_channel;

	public MidiReceiver( Synthesizer synth, ControlPanel control ) {
		synthesizer = synth;
		control_panel = control;
		set_channel( 1 );
	}

	public void set_channel( int channel ) {
		if( channel >= 1 && channel <= 16 ) {
			midi_channel = channel;
		}
	}

	public void send( MidiMessage midi_msg, long time_stamp ) {
		int idx, value;
		int msg_status, msg_channel;
		byte[] msg_data;
		msg_data = midi_msg.getMessage();
		msg_status = ( msg_data[ 0 ] & 0xF0 ) >> 4;
		if( msg_status == 0xF ) {
			/* Ignore system messages.*/
			return;
		}
		msg_channel = ( msg_data[ 0 ] & 0xF ) + 1;
		if( msg_channel != midi_channel ) {
			/* Message not on our channel.*/
			return;
		}
		switch( msg_status ) {
			case 0x8: /* Note off.*/
				synthesizer.note_on( msg_data[ 1 ] & 0x7F, 0 );
				break;
			case 0x9: /* Note on.*/
				/* It seems note on with velocity = 0 is also note off.*/
				synthesizer.note_on( msg_data[ 1 ] & 0x7F, msg_data[ 2 ] & 0x7F );
				break;
			case 0xB: /* Control change.*/
				/* Controller 120 = all sound off */
				/* Controller 121 = reset all controllers */
				/* Controller 123 = all notes off */
				control_panel.set_controller( msg_data[ 1 ] & 0x7F, msg_data[ 2 ] & 0x7F );
				break;
			case 0xC: /* Program change.*/
				/* program = msg_data[ 1 ] & 0x7F; */
				break;
			case 0xE: /* Pitch wheel.*/
				value = ( msg_data[ 1 ] & 0x7F ) | ( ( msg_data[ 2 ] & 0x7F ) << 7 );
				synthesizer.set_pitch_wheel( value - 8192 );
				break;
		}
	}

	public void close() {
	}
}
