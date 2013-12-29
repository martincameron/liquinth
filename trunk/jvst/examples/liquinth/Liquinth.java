
package jvst.examples.liquinth;

public class Liquinth implements Synthesizer, AudioSource {
	public static final String VERSION = "Liquinth a42dev15";
	public static final String AUTHOR = "(c)2013 mumart@gmail.com";
	public static final int RELEASE_DATE = 20131229;

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
		NUM_VOICES = 16;

	private MoogFilter filter;
	private Envelope filterEnv;
	private Voice[] voices;
	private byte[] keyStatus, controllers;
	private int sampleRate, filterCutoff1, filterCutoff2;

	public Liquinth( int samplingRate ) {
		sampleRate = samplingRate;
		filter = new MoogFilter( sampleRate );
		voices = new Voice[ NUM_VOICES ];
		keyStatus = new byte[ 128 ];
		controllers = new byte[ NUM_CONTROLLERS ];
		filterEnv = new Envelope( sampleRate );
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ] = new Voice( sampleRate );
			voices[ idx ].keyOn( idx );
		}
		allNotesOff( true );
		for( int idx = 0; idx < controllers.length; idx++ ) {
			setController( idx, 0 );
		}
		setController( CTRL_OVERDRIVE, 42 );
		setController( CTRL_FILTER_CUTOFF, 127 );
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
			cutoff = Maths.expScale( cutoff, 8 );
			filter.setCutoff( cutoff / ( float ) Maths.FP_ONE );
			filter.filter( outBuf, offset, count );
			offset += count;
		}
	}

	public synchronized void noteOn( int key, int velocity ) {
		int idx;
		int portaVoice, assignedVoice, quietestVoice;
		int highestKey, voiceKey, voiceVol, minVol;
		boolean keyIsOn;
		if( key < 0 || key > 127 ) {
			return;
		}
		keyStatus[ key ] = 0;
		if( velocity > 0 ) {
			keyStatus[ key ] = 1;
		}
		/* Determine highest depressed key. */
		highestKey = 128;
		for( idx = 0; idx < 128; idx++ ) {
			if( keyStatus[ idx ] > 0 ) {
				highestKey = idx;
			}
		}
		minVol = -1;
		portaVoice = -1;
		assignedVoice = -1;
		quietestVoice = -1;
		for( idx = 0; idx < NUM_VOICES; idx++ ) {
			keyIsOn = voices[ idx ].keyIsOn();
			voiceKey = voices[ idx ].getKey();
			if( key == voiceKey ) {
				/* Voice has this key already assigned to it. */
				if( keyIsOn || assignedVoice < 0 ) {
					/* Voices may have the same key. Prefer */
					/* the keyed-on voice over a keyed off one. */
					assignedVoice = idx;
				}
			}
			if( keyIsOn ) {
				if( controllers[ CTRL_PORTAMENTO ] > 0 ) {
					/* Portamento mode. */
					if( portaVoice > -1 ) {
						/* Only one voice should be active.*/
						voices[ portaVoice ].keyOff( false );
					}
					portaVoice = idx;
				}
			} else {
				/* Test if this is the quietest. */
				voiceVol = voices[ idx ].getVolume();
				if( quietestVoice < 0 || voiceVol < minVol ) {
					quietestVoice = idx;
					minVol = voiceVol;
				}
			}
		}
		if( velocity > 0 ) {
			/* Key on */
			if( portaVoice == 0 || key == highestKey ) {
				/* Only retrigger in porta mode if new key is highest. */
				if( controllers[ CTRL_FILTER_ATTACK ] == 0 ) {
					/* No filter sustain if attack is zero.*/
					filterEnv.setAmplitude( Maths.FP_MASK );
					filterEnv.keyOff();
				} else {
					filterEnv.keyOn();
				}
			}
			if( portaVoice > -1 ) {
				if( key == highestKey ) {
					/* New key is the highest. */
					voices[ portaVoice ].keyOn( key );
				}
			} else {
				if( assignedVoice > -1 ) {
					/* Re-key previously assigned voice. */
					voices[ assignedVoice ].keyOn( key );
				} else if( quietestVoice > -1 ) {
					/* Allocate new voice.*/
					voices[ quietestVoice ].keyOn( key );
				}
			}
		} else {
			/* Key off */
			if( highestKey > 127 ) {
				filterEnv.keyOff();
			}
			if( portaVoice > -1 ) {
				if( highestKey > 127 ) {
					/* Porta voice released.*/
					voices[ portaVoice ].keyOff( false );
				} else if( key > highestKey ) {
					/* Highest key released, keys still down. */
					voices[ portaVoice ].keyOn( highestKey );
				}
			} else {
				if( assignedVoice > -1 ) {
					/* Key off assigned voice. */
					voices[ assignedVoice ].keyOff( false );
				}
			}
		}
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
						value = Maths.expScale( Maths.FP_ONE - ( value << ( Maths.FP_SHIFT - 7 ) ), 11 );
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setLFOSpeed( value );
						}
						break;
					case CTRL_VIBRATO_DEPTH:
						if( value > 0 ) {
							value = Maths.expScale( value << ( Maths.FP_SHIFT - 7 ), 8 );
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

	public synchronized void setPitchWheel( int octaves ) {
		int idx;
		for( idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ].setPitchWheel( octaves );
		}
	}

	public int mapMIDIController( int controller ) {
		switch( controller ) {
			case 5:  return CTRL_PORTAMENTO;
			case 70: return CTRL_WAVEFORM;
			case 71: return CTRL_FILTER_RESONANCE;
			case 72: return CTRL_VOLUME_RELEASE;
			case 73: return CTRL_VOLUME_ATTACK;
			case 74: return CTRL_FILTER_CUTOFF;
			default: return controller - 20;
		}
	}
	
	public synchronized void setModWheel( int value ) {
		// Hard coded to vibrato depth.
		setController( CTRL_VIBRATO_DEPTH, value );
	}

	public synchronized void allNotesOff( boolean soundOff ) {
		int idx;
		for( idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ].keyOff( soundOff );
		}
		for( idx = 0; idx < 128; idx++ ) {
			keyStatus[ idx ] = -1;
		}
	}
}
