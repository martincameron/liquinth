
package liquinth;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class ControlPanel extends JPanel {
	private Synthesizer synthesizer;
	private JComboBox c1_assign_cb;
	private JSlider[] controllers;

	public ControlPanel( Synthesizer synth ) {
		int idx, num_controllers, value;
		String control_name;
		GridBagLayout gbl;
		GridBagConstraints gbc;
		Keyboard keyboard;

		synthesizer = synth;
		keyboard = new Keyboard( synth );

		gbl = new GridBagLayout();
		setLayout( gbl );

		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets( 2, 2, 2, 2 );

		gbc.weightx = 0;
		gbc.gridwidth = 1;
		add( new JLabel( "Midi Control 1" ), gbc );
		gbc.weightx = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		c1_assign_cb = new JComboBox();
		add( c1_assign_cb, gbc );

		num_controllers = synth.get_num_controllers();
		controllers = new JSlider[ num_controllers ];
		for( idx = 0; idx < num_controllers; idx++ ) {
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			control_name = synth.get_controller_name( idx );
			c1_assign_cb.addItem( control_name );
			add( new JLabel( control_name ), gbc );
			gbc.weightx = 1;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			value = synth.get_controller( idx );
			controllers[ idx ] = new JSlider( JSlider.HORIZONTAL, 0, 127, value );
			controllers[ idx ].addChangeListener( new SliderListener( idx ) );
			controllers[ idx ].addKeyListener( keyboard );
			add( controllers[ idx ], gbc );
		}
	}

	public void set_controller( int controller, int value ) {
		if( controller <= 1 ) {
			controller = c1_assign_cb.getSelectedIndex() + 2;
		}
		controllers[ controller - 2 ].setValue( value );
	}

	private class SliderListener implements ChangeListener {
		private int controller;

		public SliderListener( int control_idx ) {
			controller = control_idx;
		}

		public void stateChanged( ChangeEvent e ) {
			int value;
			value = controllers[ controller ].getValue();
			synthesizer.set_controller( controller, value );
		}
	}
}
