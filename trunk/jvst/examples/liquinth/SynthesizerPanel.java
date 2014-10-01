
package jvst.examples.liquinth;

import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SynthesizerPanel extends JPanel implements Synthesizer {
	private Synthesizer synthesizer;
	private JSlider[] controllers;
	private ControllerListener[] controllerListeners;
	private JRadioButton[] modulationAssign;
	private int modulationControlIdx;

	public SynthesizerPanel( Synthesizer synth ) {	
		synthesizer = synth;
		VirtualKeyboard keyboard = new VirtualKeyboard( synth );
		GridBagLayout gbl = new GridBagLayout();
		setLayout( gbl );
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );
		int numControllers = synth.getNumControllers();
		controllers = new JSlider[ numControllers ];
		controllerListeners = new ControllerListener[ numControllers ];
		modulationAssign = new JRadioButton[ numControllers ];
		ButtonGroup buttonGroup = new ButtonGroup();
		for( int idx = 0; idx < numControllers; idx++ ) {
			ControllerListener controllerListener = new ControllerListener( idx );
			controllerListeners[ idx ] = controllerListener;
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			add( new JLabel( synth.getControllerName( idx ) ), gbc );			
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 3;
			int value = synth.getController( idx );
			controllers[ idx ] = new JSlider( JSlider.HORIZONTAL, 0, 127, value );
			controllers[ idx ].addChangeListener( controllerListener );
			controllers[ idx ].addKeyListener( keyboard );
			add( controllers[ idx ], gbc );
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			modulationAssign[ idx ] = new JRadioButton();
			modulationAssign[ idx ].addActionListener( controllerListener );
			buttonGroup.add( modulationAssign[ idx ] );
			add( modulationAssign[ idx ], gbc );
		}
		resetAllControllers();
	}

	public int getSamplingRate() {
		return synthesizer.getSamplingRate();
	}

	public synchronized int setSamplingRate( int samplingRate ) {
		return synthesizer.setSamplingRate( samplingRate );
	}

	public String getVersion() {
		return synthesizer.getVersion();
	}

	public int getRevision() {
		return synthesizer.getRevision();
	}

	public synchronized void noteOn( int key, int velocity ) {
		synthesizer.noteOn( key, velocity );
	}
	
	public synchronized void noteOff( int key ) {
		synthesizer.noteOff( key );
	}
	
	public synchronized void allNotesOff( boolean soundOff ) {
		synthesizer.allNotesOff( soundOff );
	}
	
	public int getNumControllers() {
		return synthesizer.getNumControllers();
	}
	
	public String getControllerName( int control ) {
		return synthesizer.getControllerName( control );
	}

	public String getControllerKey( int control ) {
		return synthesizer.getControllerKey( control );
	}

	public int getControllerIdx( String key ) {
		return synthesizer.getControllerIdx( key );
	}

	public synchronized int getController( int controller ) {
		return synthesizer.getController( controller );
	}
		
	public synchronized void setController( final int controller, final int value ) {
		if( controller >= 0 && controller < controllers.length ) {
			synthesizer.setController( controller, value );
			SwingUtilities.invokeLater( controllerListeners[ controller ] );
		}
	}

	public synchronized void resetAllControllers() {
		synthesizer.resetAllControllers();
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				for( int ctrlIdx = 0; ctrlIdx < controllers.length; ctrlIdx++ ) {
					controllers[ ctrlIdx ].setValue( synthesizer.getController( ctrlIdx ) );
					if( ctrlIdx == synthesizer.getModulationControlIdx() ) {
						modulationAssign[ ctrlIdx ].doClick();
					}
				}
			}
		} );
	}

	public synchronized void setPitchWheel( int value ) {
		synthesizer.setPitchWheel( value );
	}
	
	public int getModulationControlIdx() {
		return modulationControlIdx;
	}

	public int getPortamentoControlIdx() {
		return synthesizer.getPortamentoControlIdx();
	}
	
	public int getWaveformControlIdx() {
		return synthesizer.getWaveformControlIdx();
	}

	public int getAttackControlIdx() {
		return synthesizer.getAttackControlIdx();
	}
	
	public int getReleaseControlIdx() {
		return synthesizer.getReleaseControlIdx();
	}
	
	public int getCutoffControlIdx() {
		return synthesizer.getCutoffControlIdx();
	}
	
	public int getResonanceControlIdx() {
		return synthesizer.getResonanceControlIdx();
	}
	
	public synchronized int programChange( int programIdx ) {
		return synthesizer.programChange( programIdx );
	}
	
	public synchronized void getAudio( int[] mixBuf, int length ) {
		synthesizer.getAudio( mixBuf, length );
	}

	/* Save a simple sequence that just sets the controllers. */
	public synchronized void savePatch( java.io.OutputStream output ) throws java.io.IOException {
		writeString( "(" + getVersion() + ")\n", output );
		for( int ctlIdx = 0; ctlIdx < controllers.length; ctlIdx++ ) {
			writeString( getControllerKey( ctlIdx ) + getController( ctlIdx ) + "\n", output );
		}
	}

	/* Convert the input sequence to a wave file. */
	public synchronized void saveWave( String sequence, java.io.OutputStream outputStream, int tickLen ) throws java.io.IOException {
		byte[] inputBuf = sequence.getBytes( "US-ASCII" );
		int sampleRate = getSamplingRate();
		int numSamples = runSequence( new java.io.ByteArrayInputStream( inputBuf ), null, tickLen );
		writeString( "RIFF", outputStream );
		writeInt( outputStream, numSamples * 2 + 36 ); // Wave chunk length.
		writeString( "WAVE", outputStream );
		writeString( "fmt ", outputStream );
		writeInt( outputStream, 16 ); // Format chunk length.
		writeShort( outputStream, 1 ); // PCM format.
		writeShort( outputStream, 1 ); // Mono.
		writeInt( outputStream, sampleRate );
		writeInt( outputStream, sampleRate * 2 ); // Bytes per sec.
		writeShort( outputStream, 2 ); // Frame size.
		writeShort( outputStream, 16 ); // Bits per sample.
		writeString( "data", outputStream );
		writeInt( outputStream, numSamples * 2 ); // PCM data length.
		runSequence( new java.io.ByteArrayInputStream( inputBuf ), outputStream, tickLen );
	}

	/*	Run the sequence contained within the specified InputStream into the Synthesizer.
		The sequence consists of commands in ASCII text separated by whitespace:
			(Comments are in brackets.)
			(Set controller with key "wf" to 127.) wf127
			(Key On 3 keys.) :a-5 :c-5 :e-5
			(Wait 50 ticks.) +50
			(Key Off.) /a-5 /c-5 /e-5
			(Wait 200 ticks.) +200
	   The resulting audio is written to the output (as 16 bit little-endian mono PCM) if it is not null.
	   The tickLen parameter determines the number of samples per tick of the sequence.
	   The total length of the sequence in samples is returned. */
	public synchronized int runSequence( java.io.InputStream inputStream, java.io.OutputStream outputStream, int tickLen ) throws java.io.IOException {
		int line = 1, numSamples = 0, chr = inputStream.read();
		char[] inputBuf = new char[ 8 ];
		int[] audioBuf = new int[ tickLen ];
		byte[] outputBuf = new byte[ tickLen * 2 ];
		allNotesOff( true );
		while( chr > 0 ) {
			while( ( chr > 0 && chr <= 32 ) || chr == '(' ) {
				if( chr == 10 ) line++;
				if( chr == '(' ) {
					while( chr > 0 && chr != ')' ) {
						if( chr == 10 ) line++;
						chr = inputStream.read();
					}
				}
				chr = inputStream.read();
			}
			int len = 0;
			while( chr > 32 && chr != '(' ) {
				if( len < inputBuf.length ) {
					inputBuf[ len++ ] = ( char ) chr;
				} else {
					throw new IllegalArgumentException( "Error on line " + line + ": Token '" + new String( inputBuf, 0, len ) + "' too long." );
				}
				chr = inputStream.read();
			}
			if( len > 0 ) {
				try {
					int ticks = runCommand( new String( inputBuf, 0, len ) );
					if( outputStream != null ) {
						for( int tick = 0; tick < ticks; tick++ ) {
							getAudio( audioBuf, tickLen );
							for( int outputIdx = 0; outputIdx < outputBuf.length; outputIdx += 2 ) {
								int amp = audioBuf[ outputIdx >> 1 ];
								outputBuf[ outputIdx ] = ( byte ) amp;
								outputBuf[ outputIdx + 1 ] = ( byte ) ( amp >> 8 );
							}
							outputStream.write( outputBuf, 0, outputBuf.length );
						}
					}
					numSamples += tickLen * ticks;
				} catch( IllegalArgumentException exception ) {
					throw new IllegalArgumentException( "Error on line " + line + ": " + exception.getMessage() );
				}
			}
		}
		allNotesOff( true );
		return numSamples;
	}

	/*	Run a single command into the Synthesizer.
		If the command is a wait, the number of ticks is returned. */
	public synchronized int runCommand( String command ) {
		int ticks = 0;
		char chr = command.charAt( 0 );
		if( chr == '+' ) {
			ticks = parseNumber( command.substring( 1 ) );
		} else if( chr >= '/' && chr <= ':' ) {
			int key = parseNote( command.substring( 1 ) );
			int velocity = ( chr - '0' ) * 13;
			if( velocity > 0 ) {
				noteOn( key, velocity < 127 ? velocity : 127 );
			} else {
				noteOff( key );
			}
		} else {
			int idx = 0, end = command.length();
			while( idx < end ) {
				chr = command.charAt( idx );
				if( chr <= '9' ) break;
				idx++;
			}
			if( idx == 0 ) {
				throw new IllegalArgumentException( "Invalid command: '" + command + "'.");
			}
			int ctrlIndex = getControllerIdx( command.substring( 0, idx ) );
			int ctrlValue = parseNumber( command.substring( idx ) );
			setController( ctrlIndex, ctrlValue );
		}
		return ticks;
	}

	/* Convert MIDI key (1-116) to a note string such as "c-5" for use in sequences. */
	public static String keyToNote( int key ) {
		char[] chars = new char[ 3 ];
		String keys = "c-c#d-d#e-f-f#g-g#a-a#b-c-";
		chars[ 0 ] = ( key > 0 && key < 117 ) ? keys.charAt( ( key % 12 ) * 2 ) : '-';
		chars[ 1 ] = ( key > 0 && key < 117 ) ? keys.charAt( ( key % 12 ) * 2 + 1 ) : '-';
		chars[ 2 ] = ( key > 0 && key < 117 ) ? ( char ) ( '0' + key / 12 ) : '-';
		return new String( chars );
	}

	private static int parseNote( String note ) {
		if( note.length() != 3 ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		String keys = "c-c#d-d#e-f-f#g-g#a-a#b-c-";
		int keysIdx = keys.indexOf( note.charAt( 0 ) );
		if( keysIdx < 0 ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		char chr = note.charAt( 1 );
		if( chr == '#' && keys.charAt( keysIdx + 2 ) == keys.charAt( keysIdx ) ) {
			keysIdx += 2;
		} else if( chr != '-' ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		chr = note.charAt( 2 );
		if( chr >= '0' && chr <= '9' ) {
			return ( keysIdx / 2 ) + ( chr - '0' ) * 12;
		} else {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
	}

	private static int parseNumber( String token ) {
		int num = 0;
		for( int idx = 0, end = token.length(); idx < end; idx++ ) {
			char chr = token.charAt( idx );
			if( chr >= '0' && chr <= '9' ) {
				num = num * 10 + chr - '0';
			} else {
				throw new IllegalArgumentException( "Value '" + token + "' is not a number." );
			}
		}
		return num;
	}

	private static void writeInt( OutputStream output, int value ) throws IOException {
		writeShort( output, value );
		writeShort( output, value >> 16 );
	}

	private static void writeShort( OutputStream output, int value ) throws IOException {
		output.write( ( byte ) value );
		output.write( ( byte ) ( value >> 8 ) );
	}

	private static void writeString( String string, OutputStream output ) throws IOException {
		for( int idx = 0, end = string.length(); idx < end; idx++ ) {
			output.write( ( byte ) string.charAt( idx ) );
		}
	}

	private class ControllerListener implements ChangeListener, ActionListener, Runnable {
		private int ctrlIdx;

		public ControllerListener( int controlIdx ) {
			ctrlIdx = controlIdx;
		}

		public void stateChanged( ChangeEvent e ) {
			setController( ctrlIdx, controllers[ ctrlIdx ].getValue() );
		}
		
		public void actionPerformed( ActionEvent e ) {
			modulationControlIdx = ctrlIdx;
		}

		public void run() {
			controllers[ ctrlIdx ].setValue( synthesizer.getController( ctrlIdx ) );
		}
	}
}
