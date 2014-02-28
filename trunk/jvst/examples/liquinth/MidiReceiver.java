
package jvst.examples.liquinth;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

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
		int msgStatus = ( msgData[ 0 ] & 0xF0 ) >> 4;
		if( msgStatus == 0xF ) {
			/* Ignore system messages.*/
			return;
		}
		int msgChannel = ( msgData[ 0 ] & 0xF ) + 1;
		if( msgChannel != midiChannel ) {
			/* Message not on our channel.*/
			return;
		}
		switch( msgStatus ) {
			case 0x8: /* Note off.*/
				synthesizer.noteOff( msgData[ 1 ] & 0x7F );
				break;
			case 0x9: /* Note on.*/
				int key = msgData[ 1 ] & 0x7F;
				int vel = msgData[ 2 ] & 0x7F;
				if( vel == 0 ) {
					/* It seems note on with velocity = 0 is also note off.*/
					synthesizer.noteOff( key );
				} else {
					synthesizer.noteOn( key, vel );
				}
				break;
			case 0xB: /* Control change.*/
				int ctrlIndex = msgData[ 1 ] & 0x7F;
				int ctrlValue = msgData[ 2 ] & 0x7F;
				switch( ctrlIndex ) {
					case 1: /* Modulation wheel. */
						synthesizer.setController( synthesizer.getModulationControlIdx(), ctrlValue );
						break;
					case 5: /* Portamento.*/
						synthesizer.setController( synthesizer.getPortamentoControlIdx(), ctrlValue );
						break;
					case 70: /* Waveform.*/
						synthesizer.setController( synthesizer.getWaveformControlIdx(), ctrlValue );
						break;
					case 71: /* Resonance.*/
						synthesizer.setController( synthesizer.getResonanceControlIdx(), ctrlValue );
						break;
					case 72: /* Release. */
						synthesizer.setController( synthesizer.getReleaseControlIdx(), ctrlValue );
						break;
					case 73: /* Attack.*/
						synthesizer.setController( synthesizer.getAttackControlIdx(), ctrlValue );
						break;
					case 74: /* Cutoff. */
						synthesizer.setController( synthesizer.getCutoffControlIdx(), ctrlValue );
						break;
					case 120: /* All sound off. */
						synthesizer.allNotesOff( true );
						break;
					case 121: /* Reset all controllers. */
						synthesizer.resetAllControllers();
						break;
					case 123: /* All notes off. */
						synthesizer.allNotesOff( false );
						break;
					default:
						synthesizer.setController( ctrlIndex - 20, ctrlValue );
						break;
				}				
				break;
			case 0xC: /* Program change.*/
				synthesizer.programChange( msgData[ 1 ] & 0x7F );
				break;
			case 0xE: /* Pitch wheel.*/
				int wheelValue = ( msgData[ 1 ] & 0x7F ) | ( ( msgData[ 2 ] & 0x7F ) << 7 );
				if( wheelValue > 8191 ) {
					wheelValue = wheelValue - 16384;
				}
				wheelValue = ( ( wheelValue << 18 ) >> ( 31 - Maths.FP_SHIFT ) ) / 6;
				synthesizer.setPitchWheel( wheelValue );
				break;
		}
	}

	public void close() {
	}
}
