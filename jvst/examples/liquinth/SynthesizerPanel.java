
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
	private Sequencer sequencer;
	private JSlider[] controllers;
	private ControllerListener[] controllerListeners;
	private JRadioButton[] modulationAssign;
	private int modulationControlIdx;

	public SynthesizerPanel( Synthesizer synth ) {	
		synthesizer = synth;
		sequencer = new Sequencer( this );
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

	public synchronized void loadPatch( InputStream inputStream ) throws IOException {
		resetAllControllers();
		sequencer.runSequence( inputStream, null, 0 );
	}

	public synchronized void savePatch( OutputStream outputStream ) throws IOException {
		sequencer.savePatch( outputStream );
	}

	public synchronized void saveWave( java.io.OutputStream outputStream, int[] keys, int sustain, int decay ) throws IOException {
		String sequence = "";
		for( int key : keys ) {
			if( key > 0 ) sequence += " :" + Sequencer.keyToNote( key );
		}
		sequence += " +" + ( sustain > 1 ? sustain : 1 );
		for( int key : keys ) {
			if( key > 0 ) sequence += " /" + Sequencer.keyToNote( key );
		}
		sequence += " +" + ( decay > 1 ? decay : 1 );
		sequencer.saveWave( sequence, outputStream, getSamplingRate() / 1000 );
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
