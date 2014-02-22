
package jvst.examples.liquinth.vst;

import jvst.wrapper.VSTPluginAdapter;
import jvst.wrapper.valueobjects.VSTEvent;
import jvst.wrapper.valueobjects.VSTEvents;
import jvst.wrapper.valueobjects.VSTMidiEvent;
import jvst.wrapper.valueobjects.VSTPinProperties;

import jvst.examples.liquinth.Liquinth;
import jvst.examples.liquinth.MidiReceiver;
import jvst.examples.liquinth.Synthesizer;
import jvst.examples.liquinth.SynthesizerPanel;

public class LiquinthVST extends VSTPluginAdapter {
	private static final int MIX_BUF_FRAMES = 4096;
	private Synthesizer synthesizer;
	private MidiReceiver midiReceiver;
	private int[] mixBuf;

	public LiquinthVST( long wrapper ) {
		super( wrapper );
		setSampleRate( 48000 );
		mixBuf = new int[ MIX_BUF_FRAMES ];
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
		return ( SynthesizerPanel ) synthesizer;
	}
	
	private void setController( int ctrlIdx, int value ) {
		synthesizer.setController( ctrlIdx, value );
	}

	public void setSampleRate( float sampleRate ) {
		synthesizer = new Liquinth( ( int ) sampleRate );
		midiReceiver = new MidiReceiver( synthesizer );
		setProgram( 0 );
	}

	public void setProgram( int index ) {
	}

	public void setParameter( int index, float value ) {
		setController( index, ( int ) Math.round( value * 127 ) );
	}

	public float getParameter( int index ) {
		return synthesizer.getController( index ) / 127f;
	}

	public void setProgramName( String name ) {
	}

	public String getProgramName() {
		return "";
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
		return "";
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
		return 1;
	}

	public int getNumParams() {
		return synthesizer.getNumControllers();
	}

	public boolean setBypass( boolean value ) {
		return false;
	}

	public int getProgram() {	
		return 0;
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

	public void processReplacing( float[][] inputs, float[][] outputs, int frames ) {
		float[] output = outputs[ 0 ];
		int outIdx = 0;
		while( frames > 0 ) {
			int length = frames;
			if( length > MIX_BUF_FRAMES ) length = MIX_BUF_FRAMES;
			synthesizer.getAudio( mixBuf, length );
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
