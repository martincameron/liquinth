
package jvst.examples.liquinth;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class SynthesizerPanel extends JPanel implements Synthesizer {
	private Synthesizer synthesizer;
	private JComboBox c1AssignCb;
	private JSlider[] controllers;

	public SynthesizerPanel( Synthesizer synth ) {
		int idx, numControllers, value;
		String controlName;
		GridBagLayout gbl;
		GridBagConstraints gbc;
		VirtualKeyboard keyboard;

		synthesizer = synth;
		keyboard = new VirtualKeyboard( synth );

		gbl = new GridBagLayout();
		setLayout( gbl );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		gbc.weightx = 0;
		gbc.gridwidth = 1;
		add( new JLabel( "Modulation Wheel" ), gbc );
		gbc.weightx = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		c1AssignCb = new JComboBox();
		add( c1AssignCb, gbc );

		numControllers = synth.getNumControllers();
		controllers = new JSlider[ numControllers ];
		for( idx = 0; idx < numControllers; idx++ ) {
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			controlName = synth.getControllerName( idx );
			c1AssignCb.addItem( controlName );
			add( new JLabel( controlName ), gbc );
			gbc.weightx = 1;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			value = synth.getController( idx );
			controllers[ idx ] = new JSlider( JSlider.HORIZONTAL, 0, 127, value );
			controllers[ idx ].addChangeListener( new SliderListener( idx ) );
			controllers[ idx ].addKeyListener( keyboard );
			add( controllers[ idx ], gbc );
		}
	}
	
	public void noteOn( int key, int velocity ) {
		synthesizer.noteOn( key, velocity );
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
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				controllers[ controller ].setValue( value );
			}
		} );
	}

	public void setPitchWheel( int value ) {
		synthesizer.setPitchWheel( value );
	}
	
	public void setModWheel( int value ) {
		setController( c1AssignCb.getSelectedIndex(), value );
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
