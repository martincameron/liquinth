
package jvst.examples.liquinth;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class SynthesizerPanel extends JPanel implements Synthesizer {
	private Synthesizer synthesizer;
	private JSlider[] controllers;
	private JRadioButton[] modulationAssign;

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
		add( new JSpinner( new SpinnerNumberModel( 0, 0, 127, 1 ) ), gbc );
				
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		add( new JTextField(), gbc );

		gbc.weightx = 0;
		gbc.gridwidth = 1;
		add( new JButton( "Store" ), gbc );

		gbc.weightx = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		add( new JLabel( "Mod" ), gbc );

		int numControllers = synth.getNumControllers();
		controllers = new JSlider[ numControllers ];
		modulationAssign = new JRadioButton[ numControllers ];
		ButtonGroup buttonGroup = new ButtonGroup();
		for( int idx = 0; idx < numControllers; idx++ ) {
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 2;
			add( new JLabel( synth.getControllerName( idx ) ), gbc );			
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 2;
			int value = synth.getController( idx );
			controllers[ idx ] = new JSlider( JSlider.HORIZONTAL, 0, 127, value );
			controllers[ idx ].addChangeListener( new SliderListener( idx ) );
			controllers[ idx ].addKeyListener( keyboard );
			add( controllers[ idx ], gbc );
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			modulationAssign[ idx ] = new JRadioButton();
			modulationAssign[ idx ].addActionListener( new RadioListener( idx ) );
			buttonGroup.add( modulationAssign[ idx ] );
			add( modulationAssign[ idx ], gbc );
		}
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
		return synthesizer.getModulationController();
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

	public int programChange( int progIdx ) {
		return synthesizer.programChange( progIdx );
	}

	public String getProgramName( int progIdx ) {
		return synthesizer.getProgramName( progIdx );
	}
	
	public void storeProgram( String name ) {
		synthesizer.storeProgram( name );
	}
	
	public boolean loadProgram( String program ) {
		return synthesizer.loadProgram( program );
	}
	
	public String saveProgram() {
		return synthesizer.saveProgram();
	}

	private class RadioListener implements ActionListener {
		private int controller;
		
		public RadioListener( int controlIdx ) {
			controller = controlIdx;
		}

		public void actionPerformed( ActionEvent e ) {
			synthesizer.setModulationController( controller );
		}	
	}

	private class SliderListener implements ChangeListener {
		private int controller;

		public SliderListener( int controlIdx ) {
			controller = controlIdx;
		}

		public void stateChanged( ChangeEvent e ) {
			int value;
			value = controllers[ controller ].getValue();
			synthesizer.setController( controller, value );
		}
	}
}
