
package liquinth;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jvst.wrapper.*;
import jvst.wrapper.gui.VSTPluginGUIRunner;

public class LiquinthVSTGUI extends VSTPluginGUIAdapter {
	private Synthesizer synthesizer;

	public LiquinthVSTGUI( VSTPluginGUIRunner r, VSTPluginAdapter plugin ) {
		super( r, plugin );
		final LiquinthVST liquinth_vst = ( LiquinthVST ) plugin;
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				setTitle( Liquinth.VERSION );
				JPanel panel = new JPanel();
				panel.setLayout( new BorderLayout() );
				panel.add( new LogoPanel(), BorderLayout.NORTH );
				panel.add( liquinth_vst.init_gui(), BorderLayout.CENTER );
				panel.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
				getContentPane().add( panel );
				pack();
				setVisible( true );
				//if( RUNNING_MAC_X ) setVisible( true );
			}
		} );
	}
}
