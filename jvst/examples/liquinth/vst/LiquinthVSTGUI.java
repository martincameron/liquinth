
package jvst.examples.liquinth.vst;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import jvst.wrapper.VSTPluginAdapter;
import jvst.wrapper.VSTPluginGUIAdapter;
import jvst.wrapper.gui.VSTPluginGUIRunner;

import jvst.examples.liquinth.Liquinth;
import jvst.examples.liquinth.LogoPanel;
import jvst.examples.liquinth.Synthesizer;

public class LiquinthVSTGUI extends VSTPluginGUIAdapter {
	private Synthesizer synthesizer;

	public LiquinthVSTGUI( VSTPluginGUIRunner r, VSTPluginAdapter plugin ) {
		super( r, plugin );
		final LiquinthVST liquinthVst = ( LiquinthVST ) plugin;
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				setTitle( Liquinth.VERSION );
				JPanel panel = new JPanel();
				panel.setLayout( new BorderLayout() );
				panel.add( new LogoPanel(), BorderLayout.NORTH );
				panel.add( liquinthVst.initGui(), BorderLayout.CENTER );
				panel.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
				getContentPane().add( panel );
				pack();
				setVisible( true );
				//if( RUNNING_MAC_X ) setVisible( true );
			}
		} );
	}
}
