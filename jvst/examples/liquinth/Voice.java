
package jvst.examples.liquinth;

public class Voice {
	private Oscillator osc1, osc2;
	private Envelope volEnv;
	private LFO lfo;

	private int tickLen, portaTime;
	private int pitch, portaPitch, portaRate;
	private int volume, key, pitchWheel, detune;
	private int pulseWidth, pwmDepth, vibratoDepth;

	public Voice( int samplingRate ) {
		int idx;
		tickLen = samplingRate / 1000;
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
	
	public void setOsc1Tuning( int octaves ) {
	}

	public void setOsc2Tuning( int octaves ) {
		detune = 0;
		if( octaves > 0 ) {
			detune = Maths.expScale( octaves, 8 );
		}
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

	public void setPortamentoTime( int millis ) {
		if( millis >= 0 ) {
			portaTime = millis;
		}
	}

	public void setVolume( int vol ) {
		volume = vol;
	}

	public int getVolume() {
		return volEnv.getAmplitude();
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
			/* Synchronize oscillators. */
			lfo.setPhase( 0 );
			osc1.setPhase( 0 );
			osc2.setPhase( 0 );
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

	public void setPitchWheel( int octaves ) {
		pitchWheel = octaves;
	}

	public void getAudio( int[] outBuf, int offset, int length ) {
		int pwm;
		lfo.update( length );
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
		pwm = pulseWidth + ( ( lfo.getAmplitude() * pwmDepth ) >> Maths.FP_SHIFT );
		osc1.setPulseWidth( pwm );
		osc2.setPulseWidth( pwm );
		osc1.getAudio( outBuf, offset, length );
		osc2.getAudio( outBuf, offset, length );
	}

	private void calculatePitch( boolean now ) {
		int vibrato = ( lfo.getAmplitude() * vibratoDepth ) >> Maths.FP_SHIFT;
		osc1.setPitch( pitch + pitchWheel + vibrato, now );
		osc2.setPitch( pitch + pitchWheel + detune, now );
	}

	private void calculateAmplitude( boolean now ) {
		int amplitude;
		amplitude = ( volEnv.getAmplitude() * volume ) >> ( Maths.FP_SHIFT + 1 );
		osc1.setAmplitude( amplitude, now );
		osc2.setAmplitude( amplitude, now );
	}
}
