
package jvst.examples.liquinth;

public class Liquinth implements Synthesizer {
	public static final String VERSION = "Liquinth a42dev29";
	public static final String AUTHOR = "(c)2014 mumart@gmail.com";
	public static final int RELEASE_DATE = 20140204;

	private static final int
		CTRL_OVERDRIVE = 0,
		CTRL_FILTER_CUTOFF = 1,
		CTRL_FILTER_RESONANCE = 2,
		CTRL_FILTER_ATTACK = 3,
		CTRL_FILTER_SUSTAIN = 4,
		CTRL_FILTER_DECAY = 5,
		CTRL_PORTAMENTO = 6,
		CTRL_WAVEFORM = 7,
		CTRL_VOLUME_ATTACK = 8,
		CTRL_VOLUME_RELEASE = 9,
		CTRL_OSCILLATOR_DETUNE = 10,
		CTRL_VIBRATO_SPEED = 11,
		CTRL_VIBRATO_DEPTH = 12,
		CTRL_PULSE_WIDTH = 13,
		CTRL_PULSE_WIDTH_MODULATION = 14,
		CTRL_SUB_OSCILLATOR = 15,
		CTRL_TIMBRE = 16,
		NUM_CONTROLLERS = 17,
		NUM_VOICES = 16,
		NUM_PROGRAMS = 16;

	private MoogFilter filter;
	private Envelope filterEnv;
	private Voice[] voices;
	private byte[] keyStatus, controllers;
	private int sampleRate, programIdx;
	private int filterCutoff1, filterCutoff2;

	public Liquinth( int samplingRate ) {
		sampleRate = samplingRate;
		filter = new MoogFilter( sampleRate );
		voices = new Voice[ NUM_VOICES ];
		keyStatus = new byte[ 128 ];
		controllers = new byte[ NUM_CONTROLLERS ];
		//programs = new byte[ NUM_CONTROLLERS * NUM_PROGRAMS ];
		//bank = new byte[ NUM_CONTROLLERS * NUM_PROGRAMS ];
		filterEnv = new Envelope( sampleRate );
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ] = new Voice( sampleRate );
			voices[ idx ].keyOn( idx );
		}
		allNotesOff( true );
		programChange( 0 );
	}

	public int getNumControllers() {
		return controllers.length;
	}

	public String getControllerName( int controller ) {
		String name = "";
		switch( controller ) {
			case CTRL_OVERDRIVE: name = "Overdrive"; break;
			case CTRL_FILTER_CUTOFF: name = "Filter Cutoff"; break;
			case CTRL_FILTER_RESONANCE: name = "Filter Resonance"; break;
			case CTRL_FILTER_ATTACK: name = "Filter Attack"; break;
			case CTRL_FILTER_SUSTAIN: name = "Filter Sustain Level"; break;
			case CTRL_FILTER_DECAY: name = "Filter Decay"; break;
			case CTRL_PORTAMENTO: name = "Portamento Speed"; break;
			case CTRL_WAVEFORM: name = "Waveform"; break;
			case CTRL_VOLUME_ATTACK: name = "Volume Attack"; break;
			case CTRL_VOLUME_RELEASE: name = "Volume Release"; break;
			case CTRL_OSCILLATOR_DETUNE: name = "Detune"; break;
			case CTRL_VIBRATO_SPEED: name = "Vibrato Speed"; break;
			case CTRL_VIBRATO_DEPTH: name = "Vibrato Depth"; break;
			case CTRL_PULSE_WIDTH: name = "Pulse Width"; break;
			case CTRL_PULSE_WIDTH_MODULATION: name = "Pulse Width Modulation"; break;
			case CTRL_SUB_OSCILLATOR: name = "Sub Oscillator Level"; break;
			case CTRL_TIMBRE: name = "Timbre"; break;
		}
		return name;
	}

	public synchronized void getAudio( int[] outBuf, int length ) {
		/* Clear mix buffer.*/
		for( int idx = 0; idx < length; idx++ ) {
			outBuf[ idx ] = 0;
		}
		// Ensure changes to filter cutoff are smoothly interpolated.
		int filterCutoffRate = ( ( filterCutoff2 - filterCutoff1 ) << Maths.FP_SHIFT ) / length;
		int offset = 0;
		while( offset < length ) {
			int count = length - offset;
			if( count > ( sampleRate >> 7 ) ) {
				// Ensure count is no more than 4-8ms to improve envelope responsiveness.
				count = sampleRate >> 8;
			}		
			/* Get audio from voices. */
			for( int idx = 0; idx < NUM_VOICES; idx++ ) {
				voices[ idx ].getAudio( outBuf, offset, count );
			}
			/* Handle filter envelope.*/
			filterEnv.update( count );
			filterCutoff1 += ( ( filterCutoffRate * count ) >> Maths.FP_SHIFT );
			int alevel = controllers[ CTRL_FILTER_SUSTAIN ] << ( Maths.FP_SHIFT - 7 );
			int cutoff = filterCutoff1 + ( ( filterEnv.getAmplitude() * alevel ) >> Maths.FP_SHIFT );
			if( cutoff > Maths.FP_ONE ) {
				cutoff = Maths.FP_ONE;
			}
			cutoff = Maths.exp2( cutoff << 3 ) >> 8;
			filter.setCutoff( cutoff / ( float ) Maths.FP_ONE );
			filter.filter( outBuf, offset, count );
			offset += count;
		}
	}

	public synchronized void noteOn( int key, int velocity ) {
		key = key & 0x7F;
		velocity = velocity & 0x7F;
		keyStatus[ key ] = ( byte ) velocity;
		boolean portamento = controllers[ CTRL_PORTAMENTO ] > 0;
		int highestKey = -1;
		/* Determine highest depressed key. */
		for( int idx = 0; idx < 128; idx++ ) {
			if( keyStatus[ idx ] > 0 ) {
				highestKey = idx;
			}
		}
		int assignedVoice = -1;
		int quietestVoice = -1;
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			boolean keyIsOn = voices[ idx ].keyIsOn();
			int voiceKey = voices[ idx ].getKey();
			if( keyIsOn ) {
				if( portamento ) {
					/* Assign the highest keyed-on voice to portamento and key-off others.*/
					if( assignedVoice < 0 || !voices[ assignedVoice ].keyIsOn() ) {
						assignedVoice = idx;
					} else {
						if( voiceKey >= voices[ assignedVoice ].getKey() ) {
							voices[ assignedVoice ].keyOff( false );
							assignedVoice = idx;
						} else {
							voices[ idx ].keyOff( false );
						}
					}
				} else if( voiceKey == key ) {
					/* This voice is currently keyed-on at this key.*/
					assignedVoice = idx;
				}
			} else {
				if( assignedVoice < 0 && voiceKey == key ) {
					/* This voice is currently keyed-off at this key. */
					assignedVoice = idx;
				}
				if( quietestVoice < 0 || voices[ idx ].getVolume() < voices[ quietestVoice ].getVolume() ) {
					/* This is the quietest un-keyed voice. */
					quietestVoice = idx;
				}
			}
		}
		if( velocity > 0 ) {
			/* Key on */
			if( !portamento || key == highestKey ) {
				/* Only retrigger filter in porta mode if new key is highest. */
				if( controllers[ CTRL_FILTER_ATTACK ] == 0 ) {
					/* No filter sustain if attack is zero.*/
					filterEnv.setAmplitude( Maths.FP_MASK );
					filterEnv.keyOff();
				} else {
					filterEnv.keyOn();
				}
			}
			if( assignedVoice >= 0 ) {
				if( !portamento || key == highestKey ) {
					/* Re-key voice.*/
					voices[ assignedVoice ].keyOn( key );
				}
			} else if( quietestVoice >= 0 ) {
				/* Allocate new voice.*/
				voices[ quietestVoice ].keyOn( key );
			}
		} else {
			/* Key off */
			if( highestKey < 0 ) {
				filterEnv.keyOff();
			}
			if( assignedVoice >= 0 ) {
				if( portamento && highestKey >= 0 ) {
					/* Keys still down in portamento mode.*/
					voices[ assignedVoice ].keyOn( highestKey );
				} else {
					/* Key released.*/
					voices[ assignedVoice ].keyOff( false );
				}
			}
		}
	}

	public synchronized void noteOff( int key ) {
		noteOn( key, 0 );
	}

	public synchronized int getController( int controller ) {
		int value = 0;
		if( controller >= 0 && controller < controllers.length ) {
			value = controllers[ controller ];
		}
		return value;
	}

	public synchronized void setController( int controller, int value ) {
		int idx;
		if( value >= 0 && value < 128 ) {
			if( controller >= 0 && controller < controllers.length ) {
				controllers[ controller ] = ( byte ) value;
				switch( controller ) {
					case CTRL_OVERDRIVE:
						value = value << ( Maths.FP_SHIFT - 6 );
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolume( value );
						}
						break;
					case CTRL_FILTER_CUTOFF:
						filterCutoff2 = ( value + 1 ) << ( Maths.FP_SHIFT - 7 );
						break;
					case CTRL_FILTER_RESONANCE:
						filter.setResonance( value * 0.0314f );
						break;
					case CTRL_FILTER_ATTACK:
						filterEnv.setAttackTime( ( value * value ) >> 2 );
						break;
					case CTRL_FILTER_DECAY:
						filterEnv.setReleaseTime( ( value * value ) >> 2 );
						break;
					case CTRL_PORTAMENTO:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPortamentoTime( ( value * value ) >> 4 );
						}
						break;
					case CTRL_WAVEFORM:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setWaveform( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_VOLUME_ATTACK:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolAttack( ( value * value ) >> 2 );
						}
						break;
					case CTRL_VOLUME_RELEASE:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolRelease( ( value * value ) >> 2 );
						}
						break;
					case CTRL_OSCILLATOR_DETUNE:
						if( value < 61 ) {
							value = ( value << Maths.FP_SHIFT ) / 720;
						} else {
							value = ( ( value - 55 ) << Maths.FP_SHIFT ) / 72;
						}
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setOsc2Tuning( value );
						}
						break;
					case CTRL_VIBRATO_SPEED:
						value = ( 127 - value ) << ( Maths.FP_SHIFT - 7 );
						value = Maths.exp2( ( value * 11 ) + ( 4 << Maths.FP_SHIFT ) ) >> Maths.FP_SHIFT;
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setLFOSpeed( value );
						}
						break;
					case CTRL_VIBRATO_DEPTH:
						if( value > 0 ) {
							value = Maths.exp2( value << ( Maths.FP_SHIFT - 4 ) ) >> 8;
						}
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVibratoDepth( value );
						}
						break;
					case CTRL_PULSE_WIDTH:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidth( value << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_PULSE_WIDTH_MODULATION:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidthModulationDepth( value << ( Maths.FP_SHIFT - 8 ) );
						}
						break;
					case CTRL_SUB_OSCILLATOR:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setSubOscillatorLevel( value << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_TIMBRE:
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setTimbre( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
				}
			}
		}
	}
	
	public synchronized void resetAllControllers() {
		programChange( 0 );
	}

	public int getPortamentoController() {
		return CTRL_PORTAMENTO;
	}
	
	public int getWaveformController() {
		return CTRL_WAVEFORM;
	}

	public int getAttackController() {
		return CTRL_VOLUME_ATTACK;
	}
	
	public int getReleaseController() {
		return CTRL_VOLUME_RELEASE;
	}
	
	public int getCutoffController() {
		return CTRL_FILTER_CUTOFF;		
	}
	
	public int getResonanceController() {
		return CTRL_FILTER_RESONANCE;
	}

	public int getModulationController() {
		return CTRL_VIBRATO_DEPTH;
	}

	public synchronized void setPitchWheel( int octaves ) {
		int idx;
		for( idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ].setPitchWheel( octaves );
		}
	}

	public synchronized void allNotesOff( boolean soundOff ) {
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ].keyOff( soundOff );
		}
		for( int idx = 0; idx < 128; idx++ ) {
			keyStatus[ idx ] = 0;
		}
	}

	public int getNumPrograms() {
		return NUM_PROGRAMS;
	}

	public synchronized int programChange( int progIdx ) {
		for( int idx = 0; idx < NUM_CONTROLLERS; idx++ ) {
			setController( idx, 0 );
		}
		setController( CTRL_OVERDRIVE, 32 );
		setController( CTRL_FILTER_CUTOFF, 127 );
		return 0;
	}

	public synchronized String getProgramName( int progIdx ) {
		String name = "No program";
		if( progIdx >= 0 && progIdx < NUM_PROGRAMS ) {
			// Not implemented.
		}
		return name;
	}
	
	public synchronized void setProgramName( String name ) {
		// Not implemented.
	}
		
	public synchronized void loadBank( java.io.InputStream input ) {
		throw new UnsupportedOperationException( "Load bank not implemented." );
	}
	
	public synchronized void saveBank( java.io.OutputStream output ) {
		throw new UnsupportedOperationException( "Save bank not implemented." );
	}
}
