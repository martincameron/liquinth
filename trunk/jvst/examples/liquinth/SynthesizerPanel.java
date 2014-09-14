
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
		modulationAssign = new JRadioButton[ numControllers ];
		ButtonGroup buttonGroup = new ButtonGroup();
		for( int idx = 0; idx < numControllers; idx++ ) {
			ControllerListener controllerListener = new ControllerListener( idx );
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

	public char getVersion() {
		return synthesizer.getVersion();
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
	
	public synchronized int getController( int controller ) {
		return synthesizer.getController( controller );
	}
		
	public synchronized void setController( final int controller, final int value ) {
		if( controller >= 0 && controller < controllers.length ) {
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					controllers[ controller ].setValue( value );
				}
			} );
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

	public synchronized void loadPatch( java.io.InputStream input ) throws java.io.IOException {
		resetAllControllers();
		int chr = input.read();
		for( int ctlIdx = 0; ctlIdx < controllers.length; ctlIdx++ ) {
			int value = 0;
			while( chr > synthesizer.getVersion() ) {
				/* Skip controllers from future versions. */
				chr = input.read();
				while( chr >= '0' && chr <= '9' ) {
					chr = input.read();
				}
			}
			if( chr > 32 ) {
				chr = input.read();
				while( chr >= '0' && chr <= '9' ) {
					value = value * 10 + chr - '0';
					chr = input.read();
				}
			}
			setController( ctlIdx, value & 0x7F );
		}
	}

	public synchronized void savePatch( java.io.OutputStream output ) throws java.io.IOException {
		char version = synthesizer.getVersion();
		for( int ctlIdx = 0; ctlIdx < controllers.length; ctlIdx++ ) {
			int ctlValue = synthesizer.getController( ctlIdx );
			/* The lowest version the controller is available. */
			output.write( version );
			/* The controller value in decimal. */
			output.write( '0' + ctlValue / 100 );
			output.write( '0' + ctlValue % 100 / 10 );
			output.write( '0' + ctlValue % 10 );
		}
		output.write( '\n' );
	}

	public synchronized void saveWave( java.io.OutputStream outputStream, int key, int sustain, int release ) throws java.io.IOException {
		int sampleRate = synthesizer.getSamplingRate();
		int tickLen = sampleRate >> 10;
		int ticks = sustain + release;
		writeChars( outputStream, "RIFF".toCharArray(), 4 );
		writeInt( outputStream, tickLen * ticks * 2 + 36 ); // Wave chunk length.
		writeChars( outputStream, "WAVE".toCharArray(), 4 );
		writeChars( outputStream, "fmt ".toCharArray(), 4 );
		writeInt( outputStream, 16 ); // Format chunk length.
		writeShort( outputStream, 1 ); // PCM format.
		writeShort( outputStream, 1 ); // Mono.
		writeInt( outputStream, sampleRate );
		writeInt( outputStream, sampleRate * 2 ); // Bytes per sec.
		writeShort( outputStream, 2 ); // Frame size.
		writeShort( outputStream, 16 ); // Bits per sample.
		writeChars( outputStream, "data".toCharArray(), 4 );
		writeInt( outputStream, tickLen * ticks * 2 ); // PCM data length.
		int[] inputBuf = new int[ tickLen ];
		byte[] outputBuf = new byte[ tickLen * 2 ];
		synthesizer.allNotesOff( true );
		synthesizer.noteOn( key, 127 );
		for( int tick = 0; tick < ticks; tick++ ) {
			if( tick == sustain ) {
				synthesizer.noteOff( key );
			}
			synthesizer.getAudio( inputBuf, tickLen );
			for( int outputIdx = 0; outputIdx < outputBuf.length; outputIdx += 2 ) {
				int amp = inputBuf[ outputIdx >> 1 ];
				outputBuf[ outputIdx ] = ( byte ) amp;
				outputBuf[ outputIdx + 1 ] = ( byte ) ( amp >> 8 );
			}
			outputStream.write( outputBuf, 0, outputBuf.length );
		}
		synthesizer.allNotesOff( true );
	}

	private static void writeInt( OutputStream output, int value ) throws IOException {
		writeShort( output, value );
		writeShort( output, value >> 16 );
	}

	private static void writeShort( OutputStream output, int value ) throws IOException {
		output.write( ( byte ) value );
		output.write( ( byte ) ( value >> 8 ) );
	}

	private static void writeChars( OutputStream output, char[] chars, int length ) throws IOException {
		for( int idx = 0; idx < length; idx++ ) {
			output.write( ( byte ) chars[ idx ] );
		}
	}

	private class ControllerListener implements ChangeListener, ActionListener {
		private int ctrlIdx;

		public ControllerListener( int controlIdx ) {
			ctrlIdx = controlIdx;
		}

		public void stateChanged( ChangeEvent e ) {
			synthesizer.setController( ctrlIdx, controllers[ ctrlIdx ].getValue() );
		}
		
		public void actionPerformed( ActionEvent e ) {
			modulationControlIdx = ctrlIdx;
		}	
	}
}
