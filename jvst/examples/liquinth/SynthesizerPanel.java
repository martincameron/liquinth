
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
	private int modulationController;

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

	public char getVersion() {
		return synthesizer.getVersion();
	}

	public void noteOn( int key, int velocity ) {
		synthesizer.noteOn( key, velocity );
	}
	
	public void noteOff( int key ) {
		synthesizer.noteOff( key );
	}
	
	public void allNotesOff( boolean soundOff ) {
		synthesizer.allNotesOff( soundOff );
	}
	
	public int getNumControllers() {
		return synthesizer.getNumControllers();
	}
	
	public String getControllerName( int control ) {
		return synthesizer.getControllerName( control );
	}
	
	public int getController( int controller ) {
		return synthesizer.getController( controller );
	}
		
	public void setController( final int controller, final int value ) {
		if( controller >= 0 && controller < controllers.length ) {
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					controllers[ controller ].setValue( value );
				}
			} );
		}
	}

	public void resetAllControllers() {
		synthesizer.resetAllControllers();
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				for( int ctrlIdx = 0; ctrlIdx < controllers.length; ctrlIdx++ ) {
					controllers[ ctrlIdx ].setValue( synthesizer.getController( ctrlIdx ) );
				}
			}
		} );
	}

	public void setPitchWheel( int value ) {
		synthesizer.setPitchWheel( value );
	}
	
	public int getModulationController() {
		return modulationController;
	}

	public int getPortamentoController() {
		return synthesizer.getPortamentoController();
	}
	
	public int getWaveformController() {
		return synthesizer.getWaveformController();
	}

	public int getAttackController() {
		return synthesizer.getAttackController();
	}
	
	public int getReleaseController() {
		return synthesizer.getReleaseController();
	}
	
	public int getCutoffController() {
		return synthesizer.getCutoffController();
	}
	
	public int getResonanceController() {
		return synthesizer.getResonanceController();
	}
	
	public void getAudio( int[] mixBuf, int length ) {
		synthesizer.getAudio( mixBuf, length );
	}

	public void loadPatch( java.io.InputStream input ) throws java.io.IOException {
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

	private class ControllerListener implements ChangeListener, ActionListener {
		private int controller;

		public ControllerListener( int controlIdx ) {
			controller = controlIdx;
		}

		public void stateChanged( ChangeEvent e ) {
			synthesizer.setController( controller, controllers[ controller ].getValue() );
		}
		
		public void actionPerformed( ActionEvent e ) {
			modulationController = controller;
		}	
	}
}
