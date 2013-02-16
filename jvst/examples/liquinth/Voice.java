
package jvst.examples.liquinth;

public class Voice {
	public static final int NUM_CONTROLLERS = 8;
	private static final String[] controlNames = new String[] {
		"Waveform",
		"Attack",
		"Release",
		"Detune",
		"Vibrato Speed",
		"Vibrato Depth",
		"Pulse Width",
		"Timbre"
	};

	private byte[] controllers = new byte[ NUM_CONTROLLERS ];
	private Oscillator osc1, osc2;
	private Envelope volEnv;
	private LFO vibLfo1, vibLfo2;

	private int tickLen, portaTime;
	private int pitch, portaPitch, portaRate;
	private int volume, key, pitchWheel, detune;

	public static String getControllerName( int controller ) {
		return controlNames[ controller ];
	}

	public Voice( int samplingRate ) {
		int idx;
		tickLen = samplingRate / 1000;
		osc1 = new Oscillator( samplingRate );
		osc2 = new Oscillator( samplingRate );
		volEnv = new Envelope( samplingRate );
		vibLfo1 = new LFO( samplingRate );
		vibLfo2 = new LFO( samplingRate );
		setPitchWheel( 0 );
		for( idx = 0; idx < NUM_CONTROLLERS; idx++ ) {
			setController( idx, 0 );
		}
		setPortamentoTime( 0 );
		setVolume( Maths.FP_ONE );
		keyOn( 60 ); /* Middle C.*/
		keyOff( true );
	}

	/* Portamento speed in ms. */
	public void setPortamentoTime( int millis ) {
		if( millis >= 0 ) {
			portaTime = millis;
		}
	}

	public void setVolume( int vol ) {
		volume = vol;
	}

	/* KeyOn without keyOff for portamento. */
	public void keyOn( int key ) {
		if( key != this.key ) {
			this.key = key;
			portaPitch = Maths.FP_ONE * ( ( key & 0x7F ) - 69 ) / 12;
			if( volEnv.keyIsOn() ) {
				/* Portamento */
				portaRate = 0;
				if( portaTime > 0 ) {
					portaRate = ( portaPitch - pitch ) / portaTime;
				}
				if( portaRate == 0 ) {
					pitch = portaPitch;
					calculatePitch( true );
				}
			} else {
				/* Not portamento.*/
				volEnv.keyOff( true );
				pitch = portaPitch;
				calculatePitch( true );
			} 
		}
		if( volEnv.getAmplitude() <= 0 ) {
			vibLfo1.setPhase( 0 );
			vibLfo2.setPhase( 0 );
		}
		volEnv.keyOn();
		calculateAmplitude( true );
	}

	public boolean keyIsOn() {
		return volEnv.keyIsOn();
	}

	public int getKey() {
		return key;
	}

	public void keyOff( boolean soundOff ) {
		volEnv.keyOff( soundOff );
		calculateAmplitude( true );
	}

	public int getVolume() {
		return volEnv.getAmplitude();
	}

	/*
		Set pitch wheel position ( +-1 octave ).
		Value is from -8192 to 8191 inclusive.
	*/
	public void setPitchWheel( int value ) {
		if( 8192 > value && value >= -8192 ) {
			pitchWheel = ( value << 18 ) >> 31 - Maths.FP_SHIFT;
			pitchWheel /= 6;
		}
	}

	public int getController( int controlIdx ) {
		int value = 0;
		if( controlIdx >= 0 && controlIdx < NUM_CONTROLLERS ) {
			value = controllers[ controlIdx ];
		}
		return value;
	}

	/*
		Set a modulation controller position.
		Value is from 0 to 127 inclusive.
	*/
	public void setController( int controlIdx, int value ) {
		if( controlIdx >= 0 && controlIdx < NUM_CONTROLLERS && value >= 0 && value < 128 ) {
			controllers[ controlIdx ] = ( byte ) value;
			switch( controlIdx ) {
				case 0:
					osc1.setEvenHarmonics( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
					osc2.setEvenHarmonics( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
					break;
				case 1:
					volEnv.setAttackTime( value << 4 );
					break;
				case 2:
					volEnv.setReleaseTime( value << 4 );
					break;
				case 3:
					if( value <= 0 ) {
						detune = 0;
						if( vibLfo1.getDepth() <= 0 ) {
							/* Lock the oscillators together.*/
							osc2.setPhase( osc1.getPhase() );
							calculatePitch( true );
						}
					} else {
						value = ( value + 1 ) << Maths.FP_SHIFT - 7;
						detune = Maths.expScale( value, 8 );
					}
					break;
				case 4:
					value = 128 - value << Maths.FP_SHIFT - 7;
					value = Maths.expScale( value, 11 );
					vibLfo1.setCycleLen( value );
					vibLfo2.setCycleLen( value * 99 / 70 );
					break;
				case 5:
					if( value <= 0 ) {
						vibLfo1.setDepth( 0 );
						vibLfo2.setDepth( 0 );
						if( detune <= 0 ) {
							osc2.setPhase( osc1.getPhase() );
							calculatePitch( true );
						}
					} else {
						value = value << Maths.FP_SHIFT - 7;
						value = Maths.expScale( value, 8 );
						vibLfo1.setDepth( value );
						vibLfo2.setDepth( value );
					}
					break;
				case 6:
					osc1.setPulseWidth( value << Maths.FP_SHIFT - 7 );
					osc2.setPulseWidth( value << Maths.FP_SHIFT - 7 );
					break;
				case 7:
					osc1.setComplexity( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
					osc2.setComplexity( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
					break;
			}
		}
	}

	public void getAudio( int[] outBuf, int offset, int length ) {
		int amplitude;
		vibLfo1.update( length );
		vibLfo2.update( length );
		if( pitch < portaPitch ) {
			pitch = pitch + portaRate * length / tickLen;
			if( pitch > portaPitch ) {
				pitch = portaPitch;
			}
		}
		if( pitch > portaPitch ) {
			pitch = pitch + portaRate * length / tickLen;
			if( pitch < portaPitch ) {
				pitch = portaPitch;
			}
		}
		calculatePitch( false );
		volEnv.update( length );
		calculateAmplitude( false );
		osc1.getAudio( outBuf, offset, length );
		osc2.getAudio( outBuf, offset, length );
	}

	private void calculatePitch( boolean now ) {
		int vibrato1 = vibLfo1.getAmplitude();
		int vibrato2 = vibLfo2.getAmplitude();
		osc1.setPitch( pitch + pitchWheel + vibrato1, now );
		osc2.setPitch( pitch + pitchWheel + vibrato2 + detune, now );
	}

	private void calculateAmplitude( boolean now ) {
		int amplitude;
		amplitude = volEnv.getAmplitude() * volume >> Maths.FP_SHIFT + 1;
		osc1.setAmplitude( amplitude, now );
		osc2.setAmplitude( amplitude, now );
	}
}
