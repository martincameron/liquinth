
package jvst.examples.liquinth;

public class Liquinth implements Synthesizer {
	public static final int REVISION = 42, RELEASE_DATE = 20140908;
	public static final String VERSION = "Liquinth a" + REVISION + "svn53";
	public static final String AUTHOR = "(c)2014 mumart@gmail.com";

	private static final int
		CTRL_OVERDRIVE = 0,
		CTRL_REVERB_TIME = 1,
		CTRL_FILTER_CUTOFF = 2,
		CTRL_FILTER_RESONANCE = 3,
		CTRL_FILTER_DETUNE = 4,
		CTRL_FILTER_ATTACK = 5,
		CTRL_FILTER_SUSTAIN = 6,
		CTRL_FILTER_DECAY = 7,
		CTRL_PORTAMENTO = 8,
		CTRL_WAVEFORM = 9,
		CTRL_VOLUME_ATTACK = 10,
		CTRL_VOLUME_RELEASE = 11,
		CTRL_OSCILLATOR_DETUNE = 12,
		CTRL_VIBRATO_SPEED = 13,
		CTRL_VIBRATO_DEPTH = 14,
		CTRL_PULSE_WIDTH = 15,
		CTRL_PULSE_WIDTH_MODULATION = 16,
		CTRL_SUB_OSCILLATOR = 17,
		CTRL_TIMBRE = 18,
		NUM_CONTROLLERS = 19,
		NUM_VOICES = 16;

	private MoogFilter filter;
	private Envelope filterEnv;
	private Voice[] voices = new Voice[ NUM_VOICES ];
	private byte[] keyStatus = new byte[ 128 ];
	private byte[] controllers = new byte[ NUM_CONTROLLERS ];
	private int[] reverbBuffer;
	private int sampleRate, reverbIndex, reverbLength;
	private int filterCutoff1, filterCutoff2;

	public Liquinth( int samplingRate ) {
		setSamplingRate( samplingRate );
	}
	
	public int setSamplingRate( int samplingRate ) {
		sampleRate = samplingRate;
		filter = new MoogFilter( sampleRate );
		filterEnv = new Envelope( sampleRate );
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ] = new Voice( sampleRate );
			voices[ idx ].keyOn( idx );
		}
		reverbBuffer = new int[ samplingRate ];
		allNotesOff( true );
		for( int ctlIdx = 0; ctlIdx < NUM_CONTROLLERS; ctlIdx++ ) {
			setController( ctlIdx, getController( ctlIdx ) );
		}
		return samplingRate;
	}

	public char getVersion() {
		return REVISION;
	}

	public int getNumControllers() {
		return controllers.length;
	}

	public String getControllerName( int controller ) {
		String name = "";
		switch( controller ) {
			case CTRL_OVERDRIVE: name = "Overdrive"; break;
			case CTRL_REVERB_TIME: name = "Reverb Time"; break;
			case CTRL_FILTER_CUTOFF: name = "Filter Cutoff"; break;
			case CTRL_FILTER_RESONANCE: name = "Filter Resonance"; break;
			case CTRL_FILTER_DETUNE: name = "Filter Detune"; break;
			case CTRL_FILTER_ATTACK: name = "Filter Attack"; break;
			case CTRL_FILTER_SUSTAIN: name = "Filter Sustain Level"; break;
			case CTRL_FILTER_DECAY: name = "Filter Decay"; break;
			case CTRL_PORTAMENTO: name = "Portamento Speed"; break;
			case CTRL_WAVEFORM: name = "Waveform"; break;
			case CTRL_VOLUME_ATTACK: name = "Volume Attack"; break;
			case CTRL_VOLUME_RELEASE: name = "Volume Release"; break;
			case CTRL_OSCILLATOR_DETUNE: name = "Oscillator Detune"; break;
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
		/* Ensure changes to filter cutoff are smoothly interpolated. */
		int filterCutoffRate = ( ( filterCutoff2 - filterCutoff1 ) << Maths.FP_SHIFT ) / length;
		int offset = 0;
		while( offset < length ) {
			int count = length - offset;
			if( count > ( sampleRate >> 7 ) ) {
				/* Ensure count is no more than 4-8ms to improve envelope responsiveness. */
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
			/* Apply reverb.*/
			if( reverbLength > 0 ) {
				reverb( outBuf, offset, count );
			}
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
		if( value >= 0 && value < 128 ) {
			if( controller >= 0 && controller < controllers.length ) {
				controllers[ controller ] = ( byte ) value;
				switch( controller ) {
					case CTRL_OVERDRIVE:
						value = value << ( Maths.FP_SHIFT - 6 );
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolume( value );
						}
						break;
					case CTRL_REVERB_TIME:
						int len = ( sampleRate * value ) >> 7;
						for( int idx = reverbLength; idx < len; idx++ ) {
							reverbBuffer[ idx ] = 0;
						}
						if( reverbIndex > len ) {
							reverbIndex = 0;
						}
						reverbLength = len;
						break;
					case CTRL_FILTER_CUTOFF:
						filterCutoff2 = ( value + 1 ) << ( Maths.FP_SHIFT - 7 );
						break;
					case CTRL_FILTER_RESONANCE:
						filter.setResonance( value * 0.0314f );
						break;
					case CTRL_FILTER_DETUNE:
						filter.setDetune( value * 0.0078f );
						break;
					case CTRL_FILTER_ATTACK:
						filterEnv.setAttackTime( ( value * value ) >> 2 );
						break;
					case CTRL_FILTER_DECAY:
						filterEnv.setReleaseTime( ( value * value ) >> 2 );
						break;
					case CTRL_PORTAMENTO:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPortamentoTime( ( value * value ) >> 4 );
						}
						break;
					case CTRL_WAVEFORM:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setWaveform( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_VOLUME_ATTACK:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolAttack( ( value * value ) >> 2 );
						}
						break;
					case CTRL_VOLUME_RELEASE:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolRelease( ( value * value ) >> 2 );
						}
						break;
					case CTRL_OSCILLATOR_DETUNE:
						if( value < 61 ) {
							value = ( value << Maths.FP_SHIFT ) / 720;
						} else {
							value = ( ( value - 55 ) << Maths.FP_SHIFT ) / 72;
						}
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setOsc2Tuning( value );
						}
						break;
					case CTRL_VIBRATO_SPEED:
						value = ( 127 - value ) << ( Maths.FP_SHIFT - 7 );
						value = Maths.exp2( ( value * 11 ) + ( 4 << Maths.FP_SHIFT ) ) >> Maths.FP_SHIFT;
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setLFOSpeed( value );
						}
						break;
					case CTRL_VIBRATO_DEPTH:
						if( value > 0 ) {
							value = Maths.exp2( value << ( Maths.FP_SHIFT - 4 ) ) >> 8;
						}
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVibratoDepth( value );
						}
						break;
					case CTRL_PULSE_WIDTH:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidth( value << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_PULSE_WIDTH_MODULATION:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidthModulationDepth( value << ( Maths.FP_SHIFT - 8 ) );
						}
						break;
					case CTRL_SUB_OSCILLATOR:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setSubOscillatorLevel( value << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case CTRL_TIMBRE:
						for( int idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setTimbre( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
				}
			}
		}
	}
	
	public synchronized void resetAllControllers() {
		for( int ctlIdx = 0; ctlIdx < NUM_CONTROLLERS; ctlIdx++ ) {
			int ctlValue = 0;
			if( ctlIdx == CTRL_OVERDRIVE ) {
				ctlValue = 32;
			}
			if( ctlIdx == CTRL_FILTER_CUTOFF ) {
				ctlValue = 127;
			}
			setController( ctlIdx, ctlValue );
		}
	}

	public int getPortamentoControlIdx() {
		return CTRL_PORTAMENTO;
	}
	
	public int getWaveformControlIdx() {
		return CTRL_WAVEFORM;
	}

	public int getAttackControlIdx() {
		return CTRL_VOLUME_ATTACK;
	}
	
	public int getReleaseControlIdx() {
		return CTRL_VOLUME_RELEASE;
	}
	
	public int getCutoffControlIdx() {
		return CTRL_FILTER_CUTOFF;		
	}
	
	public int getResonanceControlIdx() {
		return CTRL_FILTER_RESONANCE;
	}

	public int getModulationControlIdx() {
		return CTRL_VIBRATO_DEPTH;
	}

	public int programChange( int programIdx ) {
		return 0;
	}

	public synchronized void setPitchWheel( int octaves ) {
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
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

	private void reverb( int[] mixBuf, int mixIdx, int count ) {
		/* Simple delay with feedback. */
		int mixEnd = mixIdx + count;
		while( mixIdx < mixEnd ) {
			mixBuf[ mixIdx ] = ( mixBuf[ mixIdx ] * 3 + reverbBuffer[ reverbIndex ] ) >> 2;
			reverbBuffer[ reverbIndex++ ] = mixBuf[ mixIdx++ ];
			if( reverbIndex >= reverbLength ) {
				reverbIndex = 0;
			}
		}
	}
}
