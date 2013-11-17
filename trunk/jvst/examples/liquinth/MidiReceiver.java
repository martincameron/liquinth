
package jvst.examples.liquinth;

import javax.sound.midi.*;

public class MidiReceiver implements Receiver {
	private Synthesizer synthesizer;
	private int midiChannel;

	public MidiReceiver( Synthesizer synth ) {
		synthesizer = synth;
		setChannel( 1 );
	}

	public void setChannel( int channel ) {
		if( channel >= 1 && channel <= 16 ) {
			midiChannel = channel;
		}
	}

	public void send( MidiMessage midiMsg, long timeStamp ) {
		send( midiMsg.getMessage() );
	}

	public void send( byte[] msgData ) {
		int ctrlIndex, ctrlValue, msgStatus, msgChannel;
		msgStatus = ( msgData[ 0 ] & 0xF0 ) >> 4;
		if( msgStatus == 0xF ) {
			/* Ignore system messages.*/
			return;
		}
		msgChannel = ( msgData[ 0 ] & 0xF ) + 1;
		if( msgChannel != midiChannel ) {
			/* Message not on our channel.*/
			return;
		}
		switch( msgStatus ) {
			case 0x8: /* Note off.*/
				synthesizer.noteOn( msgData[ 1 ] & 0x7F, 0 );
				break;
			case 0x9: /* Note on.*/
				/* It seems note on with velocity = 0 is also note off.*/
				synthesizer.noteOn( msgData[ 1 ] & 0x7F, msgData[ 2 ] & 0x7F );
				break;
			case 0xB: /* Control change.*/
				ctrlIndex = msgData[ 1 ] & 0x7F;
				ctrlValue = msgData[ 2 ] & 0x7F;
				switch( ctrlIndex ) {
					case 1: /* Modulation wheel. */
						synthesizer.setModWheel( ctrlValue );
						break;
					case 120: /* All sound off. */
						synthesizer.allNotesOff( true );
						break;
					case 121: /* Reset all controllers. */
						break;
					case 123: /* All notes off. */
						synthesizer.allNotesOff( false );
						break;
					default:
						synthesizer.setController( synthesizer.mapMIDIController( ctrlIndex ), ctrlValue );
						break;
				}				
				break;
			case 0xC: /* Program change.*/
				/* program = msgData[ 1 ] & 0x7F; */
				break;
			case 0xE: /* Pitch wheel.*/
				ctrlValue = ( msgData[ 1 ] & 0x7F ) | ( ( msgData[ 2 ] & 0x7F ) << 7 );
				if( ctrlValue > 8191 ) {
					ctrlValue = ctrlValue - 16384;
				}
				ctrlValue = ( ctrlValue << 18 ) >> ( 31 - Maths.FP_SHIFT );
				synthesizer.setPitchWheel( ctrlValue / 6 );
				break;
		}
	}

	public void close() {
	}
}
