
package jvst.examples.liquinth;

public class Voice {
	private Oscillator osc1, osc2;
	private Envelope volEnv;
	private LFO lfo;

	private int sampleRate;
	private int pitch, portaPitch, portaRate;
	private int volume, key, pitchWheel, detune;
	private int pulseWidth, pwmDepth, vibratoDepth;

	public Voice( int samplingRate ) {
		sampleRate = samplingRate;
		osc1 = new Oscillator( samplingRate );
		osc2 = new Oscillator( samplingRate );
		volEnv = new Envelope( samplingRate );
		lfo = new LFO( samplingRate );
		setVolume( Maths.FP_ONE );
		keyOn( 60 ); /* Middle C.*/
		keyOff( true );
	}

	public void setWaveform( int wave ) {
		osc1.setEvenHarmonics( wave );
		osc2.setEvenHarmonics( wave );
	}
	
	public void setVolAttack( int millis ) {
		volEnv.setAttackTime( millis );
	}
	
	public void setVolSustain( int level ) {
	}
	
	public void setVolRelease( int millis ) {
		volEnv.setReleaseTime( millis );
	}

	public void setOsc2Tuning( int octaves ) {
		detune = octaves;
	}
	
	public void setLFOSpeed( int millis ) {
		lfo.setCycleLen( millis );
	}
	
	public void setVibratoDepth( int octaves ) {
		vibratoDepth = octaves;
	}
	
	public void setPulseWidth( int octaves ) {
		pulseWidth = octaves;
	}

	public void setTimbre( int value ) {
		osc1.setComplexity( value );
		osc2.setComplexity( value );
	}

	public void setPulseWidthModulationDepth( int octaves ) {
		pwmDepth = octaves;
	}
	
	public void setSubOscillatorLevel( int value ) {
		osc1.setSubOscillator( value );
		osc2.setSubOscillator( value );
	}

	public void setPortamentoTime( int millisPerOctave ) {
		if( millisPerOctave < 1 ) {
			millisPerOctave = 1;
		}
		portaRate = ( Maths.FP_ONE << 15 ) / ( sampleRate * millisPerOctave );
	}

	public void setVolume( int vol ) {
		volume = vol;
	}

	public int getVolume() {
		return volEnv.getAmplitude();
	}

	/* KeyOn without keyOff for portamento. */
	public void keyOn( int key ) {
		this.key = key;
		portaPitch = Maths.FP_ONE * ( ( key & 0x7F ) - 69 ) / 12;
		if( volEnv.getAmplitude() <= 0 ) {
			/* Synchronize oscillators. */
			lfo.setPhase( 0 );
			osc1.setPhase( 0 );
			osc2.setPhase( 0 );
			pitch = portaPitch;
		}
		volEnv.keyOn();
	}

	public boolean keyIsOn() {
		return volEnv.keyIsOn();
	}

	public int getKey() {
		return key;
	}

	public void keyOff( boolean soundOff ) {
		volEnv.keyOff();
		if( soundOff ) {
			volEnv.setAmplitude( 0 );
		}
	}

	public void setPitchWheel( int octaves ) {
		pitchWheel = octaves;
	}

	public void getAudio( int[] outBuf, int offset, int length ) {
		if( pitch < portaPitch ) {
			pitch = pitch + ( ( portaRate * length ) >> 5 );
			if( pitch > portaPitch ) {
				pitch = portaPitch;
			}
		}
		if( pitch > portaPitch ) {
			pitch = pitch - ( ( portaRate * length ) >> 5 );
			if( pitch < portaPitch ) {
				pitch = portaPitch;
			}
		}
		lfo.update( length );
		int vibrato = ( lfo.getAmplitude() * vibratoDepth ) >> Maths.FP_SHIFT;
		osc1.setPitch( pitch + pitchWheel + vibrato );
		osc2.setPitch( pitch + pitchWheel + vibrato + detune - ( ( detune * vibrato ) >> ( Maths.FP_SHIFT + 1 ) ) );
		volEnv.update( length );
		int pwm = pulseWidth + ( ( lfo.getAmplitude() * pwmDepth ) >> Maths.FP_SHIFT );
		osc1.setPulseWidth( pwm );
		osc2.setPulseWidth( pwm );
		int amplitude = ( volEnv.getAmplitude() * volume ) >> ( Maths.FP_SHIFT + 1 );
		// x^2 volume curve.
		amplitude = ( amplitude * amplitude ) >> Maths.FP_SHIFT;
		osc1.setAmplitude( amplitude );
		osc2.setAmplitude( amplitude );
		osc1.getAudio( outBuf, offset, length );
		osc2.getAudio( outBuf, offset, length );
	}
}
