
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
	private SpinnerNumberModel programChangeSpinnerModel;
	private JTextField programNameField;
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
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		add( new JLabel( "Program" ), gbc );
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		programChangeSpinnerModel = new SpinnerNumberModel( 0, 0, 127, 1 );
		JSpinner programChangeSpinner = new JSpinner( programChangeSpinnerModel );
		programChangeSpinner.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				programChange( programChangeSpinnerModel.getNumber().intValue() );
			}
		} );
		add( programChangeSpinner, gbc );	
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		programNameField = new JTextField();
		ProgramNameListener programNameListener = new ProgramNameListener();
		programNameField.addActionListener( programNameListener );
		programNameField.addFocusListener( programNameListener );
		add( programNameField, gbc );
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		javax.swing.JButton resetButton = new javax.swing.JButton( "Reset" );
		resetButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				resetAllControllers();
			}
		} );
		add( resetButton, gbc );
		gbc.weightx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		add( new JLabel( "Mod" ), gbc );
		int numControllers = synth.getNumControllers();
		controllers = new JSlider[ numControllers ];
		modulationAssign = new JRadioButton[ numControllers ];
		ButtonGroup buttonGroup = new ButtonGroup();
		for( int idx = 0; idx < numControllers; idx++ ) {
			ControllerListener controllerListener = new ControllerListener( idx );
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 2;
			add( new JLabel( synth.getControllerName( idx ) ), gbc );			
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 2;
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
		programChange( 0 );
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
	
	public void setModulationController( final int controlIdx ) {
		if( controlIdx >= 0 && controlIdx < controllers.length ) {
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					modulationAssign[ controlIdx ].doClick();
				}
			} );
		}
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

	public int getNumPrograms() {
		return synthesizer.getNumPrograms();
	}

	public int programChange( int programIdx ) {
		final int progIdx = synthesizer.programChange( programIdx );
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				programChangeSpinnerModel.setValue( Integer.valueOf( progIdx ) );
				programNameField.setText( synthesizer.getProgramName( progIdx ) );
				modulationController = synthesizer.getModulationController();
				modulationAssign[ modulationController ].setSelected( true );
				for( int ctrlIdx = 0; ctrlIdx < controllers.length; ctrlIdx++ ) {
					controllers[ ctrlIdx ].setValue( synthesizer.getController( ctrlIdx ) );
				}
			}
		} );		
		return progIdx;
	}

	public String getProgramName( int progIdx ) {
		return synthesizer.getProgramName( progIdx );
	}

	public void setProgramName( String name ) {
		synthesizer.setProgramName( name );
	}

	public void loadBank( InputStream input ) throws IOException {
		synthesizer.loadBank( input );
		programChange( 0 );
	}
		
	public void saveBank( OutputStream output ) throws IOException {
		synthesizer.saveBank( output );
	}
	
	public void getAudio( int[] mixBuf, int length ) {
		synthesizer.getAudio( mixBuf, length );
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
	
	private class ProgramNameListener implements FocusListener, ActionListener {
		public void focusGained( FocusEvent e ) {
		}
		
		public void focusLost( FocusEvent e ) {
			synthesizer.setProgramName( programNameField.getText() );
		}

		public void actionPerformed( ActionEvent e ) {
			synthesizer.setProgramName( programNameField.getText() );
		}
	}
}
