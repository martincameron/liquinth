
package jvst.examples.liquinth;

import jvst.wrapper.*;
import jvst.wrapper.valueobjects.*;

public class LiquinthVST extends VSTPluginAdapter {
	private static final int
		NUM_PROGRAMS = 16,
		MIX_BUF_FRAMES = 1024;

	private Synthesizer synthesizer;
	private AudioSource audioSource;
	private MidiReceiver midiReceiver;
	private Program[] programs;
	private int currentProgram;
	private int[] mixBuf;

	public LiquinthVST( long wrapper ) {
		super( wrapper );
		
		Liquinth liquinth = new Liquinth( 48000 );
		synthesizer = liquinth;
		audioSource = liquinth;
		midiReceiver = new MidiReceiver( synthesizer );

		programs = new Program[ NUM_PROGRAMS ];
		for( int prgIdx = 0; prgIdx < NUM_PROGRAMS; prgIdx++ ) {
			programs[ prgIdx ] = new Program( "Blank " + prgIdx, synthesizer );
		}

		mixBuf = audioSource.allocateMixBuf( MIX_BUF_FRAMES );
		
		setNumInputs( 0 );
		setNumOutputs( 1 );
		canProcessReplacing( true );
		isSynth( true );
		setUniqueID( Liquinth.RELEASE_DATE );

		suspend();
	}

	public SynthesizerPanel initGui() {
		synthesizer = new SynthesizerPanel( synthesizer );
		midiReceiver = new MidiReceiver( synthesizer );
		for( int prgIdx = 0; prgIdx < NUM_PROGRAMS; prgIdx++ ) {
			programs[ prgIdx ].setControls( synthesizer );
		}
		return ( SynthesizerPanel ) synthesizer;
	}
	
	private void setController( int ctrlIdx, int value ) {
		synthesizer.setController( ctrlIdx, value );
	}

	/* Deprecated as of VST 2.4 */
	public void resume() {
		wantEvents( 1 );
	}

	public void setSampleRate( float sampleRate ) {
		audioSource.setSamplingRate( ( int ) sampleRate );
	}

	public void setProgram( int index ) {
		if( index < 0 || index >= NUM_PROGRAMS ) return;
		programs[ currentProgram ].store();
		currentProgram = index;
		programs[ currentProgram ].load();
	}

	public void setParameter( int index, float value ) {
		setController( index, ( int ) Math.round( value * 127 ) );
	}

	public float getParameter( int index ) {
		return synthesizer.getController( index ) / 127f;
	}

	public void setProgramName( String name ) {
		programs[ currentProgram ].name = name;
	}

	public String getProgramName() {
		return programs[ currentProgram ].name;
	}

	public String getParameterLabel( int index ) {
		return "";
	}

	public String getParameterDisplay( int index ) {
		return "";
	}

	public String getParameterName( int index ) {
		return synthesizer.getControllerName( index );
	}

	public VSTPinProperties getOutputProperties( int index ) {
		VSTPinProperties vpp = null;
		if( index == 0 ) {
			vpp = new VSTPinProperties();
			vpp.setLabel( "Liquinth" );
			vpp.setFlags( VSTPinProperties.VST_PIN_IS_ACTIVE );
		}
		return vpp;
	}

	public String getProgramNameIndexed( int category, int index ) {
		if( index < 0 || index >= NUM_PROGRAMS ) return "";
		return programs[ index ].name;
	}

	/* Deprecated as of VST 2.4 */
	public boolean copyProgram( int destIdx ) {
		if( destIdx < 0 || destIdx >= NUM_PROGRAMS ) return false;
		programs[ destIdx ] = new Program( programs[ currentProgram ] );
		return true;
	}

	public String getEffectName() {
		return Liquinth.VERSION;
	}

	public String getVendorString() {
		return Liquinth.AUTHOR;
	}

	public String getProductString() {
		return Liquinth.VERSION;
	}

	public int getNumPrograms() {
		return NUM_PROGRAMS;
	}

	public int getNumParams() {
		return synthesizer.getNumControllers();
	}

	public boolean setBypass( boolean value ) {
		return false;
	}

	public int getProgram() {	
		return currentProgram;
	}

	public int getPlugCategory() {
		return VSTPluginAdapter.PLUG_CATEG_SYNTH;
	}

	public int canDo( String feature ) {
		if( feature.equals( CANDO_PLUG_RECEIVE_VST_EVENTS ) )
			return CANDO_YES;
		if( feature.equals( CANDO_PLUG_RECEIVE_VST_MIDI_EVENT ) )
			return CANDO_YES;
		return CANDO_NO;
	}

	public boolean string2Parameter( int index, String value ) {
		try {
			float floatValue = Float.parseFloat( value );
			setParameter( index, floatValue );
		} catch( Exception e ) {
			return false;
		}
		return true;
	}

	/* Deprecated as of VST 2.4 */
	public void process( float[][] inputs, float[][] outputs, int frames ) {
		float[] output = outputs[ 0 ];
		int outIdx = 0;
		while( frames > 0 ) {
			int length = frames;
			if( length > MIX_BUF_FRAMES ) length = MIX_BUF_FRAMES;
			audioSource.getAudio( mixBuf, length );
			for( int mixIdx = 0; mixIdx < length; mixIdx++ ) {
				float out = mixBuf[ mixIdx ];
				output[ outIdx++ ] += out * 0.00003f; 
			}
			frames -= length;
		}
	}

	public void processReplacing( float[][] inputs, float[][] outputs, int frames ) {
		float[] output = outputs[ 0 ];
		int outIdx = 0;
		while( frames > 0 ) {
			int length = frames;
			if( length > MIX_BUF_FRAMES ) length = MIX_BUF_FRAMES;
			audioSource.getAudio( mixBuf, length );
			for( int mixIdx = 0; mixIdx < length; mixIdx++ ) {
				float out = mixBuf[ mixIdx ];
				output[ outIdx++ ] = out * 0.00003f; 
			}
			frames -= length;
		}
	}

	public int processEvents( VSTEvents vstEvents ) {
		VSTEvent[] events = vstEvents.getEvents();
		int numEvents = vstEvents.getNumEvents();
		for( int evIdx = 0; evIdx < numEvents; evIdx++ ) {
			VSTEvent event = events[ evIdx ];
			if( event.getType() == VSTEvent.VST_EVENT_MIDI_TYPE ) {
				midiReceiver.send( ( ( VSTMidiEvent ) event ).getData() );
			}
		}
		return 1;
	}
}

class Program {
	public String name = "";
	private int[] controllers;
	private Controls controls;
	
	public Program( String name, Controls controls ) {
		this.name = name;
		controllers = new int[ controls.getNumControllers() ];
		setControls( controls );
		store();
	}
	
	public Program( Program program ) {
		name = program.name;
		controls = program.controls;
		controllers = new int[ program.controllers.length ];
		for( int idx = 0; idx < controllers.length; idx++ ) {
			controllers[ idx ] = program.controllers[ idx ];
		}
	}
	
	public void load() {
		for( int idx = 0; idx < controllers.length; idx++ ) {
			controls.setController( idx, controllers[ idx ] );
		}		
	}
	
	public void store() {
		for( int idx = 0; idx < controllers.length; idx++ ) {
			controllers[ idx ] = controls.getController( idx );
		}
	}
	
	public void setControls( Controls controls ) {
		if( controls.getNumControllers() != controllers.length ) {
			throw new IllegalArgumentException( "Number of controllers differ." );
		}
		this.controls = controls;
	}
}
