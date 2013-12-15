
package jvst.examples.liquinth;

public class Liquinth implements Synthesizer, AudioSource {
	public static final String VERSION = "Liquinth a42dev12";
	public static final String AUTHOR = "(c)2013 mumart@gmail.com";
	public static final int RELEASE_DATE = 20131215;

	private static final int
		LOG2_NUM_VOICES = 4, /* 16 voices.*/
		NUM_VOICES = 1 << LOG2_NUM_VOICES;

	private static final String[] controlNames = new String[] {
		"Overdrive",
		"Filter Cutoff",
		"Filter Resonance",
		"Filter Attack Level",
		"Filter Decay",
		"Portamento Speed",
		"Waveform",
		"Attack",
		"Release",
		"Detune",
		"Vibrato Speed",
		"Vibrato Depth",
		"Pulse Width",
		"Timbre",
		"Pulse Width Modulation",
		"Sub Oscillator Level"
	};

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
		controllers = new byte[ controlNames.length ];
		filterEnv = new Envelope( sampleRate );
		for( int idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ] = new Voice( sampleRate );
			voices[ idx ].keyOn( idx );
		}
		allNotesOff( true );
		setController( 0, 42 );
		setController( 1, 127 );
		for( int idx = 2; idx < controllers.length; idx++ ) {
			setController( idx, 0 );
		}
	}

	public int getNumControllers() {
		return controllers.length;
	}

	public String getControllerName( int controller ) {
		String name = "";
		if( controller >= 0 && controller < controllers.length ) {
			name = controlNames[ controller ];
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
			int alevel = controllers[ 3 ] << ( Maths.FP_SHIFT - 7 );
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
				if( controllers[ 5 ] > 0 ) {
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
			if( portaVoice > -1 ) {
				if( key == highestKey ) {
					/* Key pressed is higher than before.*/
					filterEnv.setAmplitude( Maths.FP_MASK );
					filterEnv.keyOff();
					/* New key is the highest. */
					voices[ portaVoice ].keyOn( key );
				}
			} else {
				filterEnv.setAmplitude( Maths.FP_MASK );
				filterEnv.keyOff();
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
					case 0: /* Overdrive. */
						value = value << ( Maths.FP_SHIFT - 6 );
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolume( value );
						}
						break;
					case 1: /* Filter cutoff. */
						filterCutoff2 = ( value + 1 ) << ( Maths.FP_SHIFT - 7 );
						break;
					case 2: /* Filter resonance.*/
						filter.setResonance( value * 0.0314f );
						break;
					case 3: /* Filter envelope level.*/
						break;
					case 4: /* Filter release time.*/
						filterEnv.setReleaseTime( value << 5 );
						break;
					case 5: /* Portamento time.*/
						if( value > 0 ) {
							value = Maths.expScale( ( value << Maths.FP_SHIFT - 7 ), 10 );
						}
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPortamentoTime( value >> ( Maths.FP_SHIFT - 10 ) );
						}
						break;
					case 6: /* Voice waveform.*/
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setWaveform( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case 7: /* Volume attack time. */
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolAttack( value << 5 );
						}
						break;
					case 8: /* Volume release time. */
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVolRelease( value << 5 );
						}
						break;
					case 9: /* Detune. */
						if( value > 0 ) {
							value = ( value + 1 ) << ( Maths.FP_SHIFT - 7 );
							value = Maths.expScale( value, 8 );
						}
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setOsc2Tuning( value );
						}
						break;
					case 10: /* Vibrato speed. */
						value = ( 128 - value ) << ( Maths.FP_SHIFT - 7 );
						value = Maths.expScale( value, 11 );
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setLFOSpeed( value );
						}
						break;
					case 11: /* Vibrato depth.*/
						if( value > 0 ) {
							value = Maths.expScale( value << ( Maths.FP_SHIFT - 7 ), 8 );
						}
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setVibratoDepth( value );
						}
						break;
					case 12: /* Pulse width.*/
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidth( value << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case 13: /* Timbre.*/
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setTimbre( ( 127 - value ) << ( Maths.FP_SHIFT - 7 ) );
						}
						break;
					case 14: /* Pulse width modulation. */
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setPulseWidthModulationDepth( value << ( Maths.FP_SHIFT - 8 ) );
						}
						break;
					case 15: /* Sub oscillator level. */
						for( idx = 0; idx < NUM_VOICES; idx++ ) {
							voices[ idx ].setSubOscillatorLevel( value << ( Maths.FP_SHIFT - 7 ) );
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
			case 5: /* Portamento time. */
				return 5;
			case 70: /* Waveform. */
				return 6;
			case 71: /* Resonance. */
				return 2;
			case 72: /* Release. */
				return 8;
			case 73: /* Attack. */
				return 7;
			case 74: /* Cutoff. */
				return 1;
			default:
				return controller - 20;
		}
	}
	
	public synchronized void setModWheel( int value ) {
		// Hard coded to vibrato depth.
		setController( 11, value );
	}

	public synchronized void allNotesOff( boolean soundOff ) {
		int idx;
		for( idx = 0; idx < NUM_VOICES; idx++ ) {
			voices[ idx ].keyOff( soundOff );
		}
		for( idx = 0; idx < 128; idx++ ) {
			keyStatus[ idx ] = 0;
		}
	}

	private static int downsample( int s, int[] buffer, int len ) {
		// Convolve with kernel ( 0.25, 0.5, 0.25 ).
		// Filter envelope is Sin^2( PI * f ) / ( PI * f )^2 where fs = 1.0.
		for( int in = 0, out = 0; in < len; in += 2, out += 1 ) {
			int a = s + ( buffer[ in ] >> 1 );
			s = buffer[ in + 1 ] >> 2;
			buffer[ out ] = a + s;
		}
		return s;
	}
}

