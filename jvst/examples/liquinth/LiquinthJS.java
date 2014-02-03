
package jvst.examples.liquinth;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class LiquinthJS extends JFrame {
	public LiquinthJS() {
		Liquinth liquinth = new Liquinth( Player.SAMPLING_RATE * Player.OVERSAMPLE );
		final SynthesizerPanel synthPanel = new SynthesizerPanel( liquinth );
		final Player player = new Player( liquinth );
		AudioSelector audioSelector = new AudioSelector( player );
		MidiReceiver midiReceiver = new MidiReceiver( synthPanel );
		MidiSelector midiSelector = new MidiSelector( midiReceiver );
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu( "File" );
		JMenuItem loadMenuItem = new JMenuItem( "Load Bank" );
		UIManager.put( "FileChooser.readOnly", Boolean.TRUE );
		final JFileChooser loadFileChooser = new JFileChooser();
		loadFileChooser.setFileFilter( new FileNameExtensionFilter( "Sound Bank", "liq" ) );
		loadMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				int result = loadFileChooser.showOpenDialog( LiquinthJS.this );
				if( result == JFileChooser.APPROVE_OPTION ) {
					try {
						File file = loadFileChooser.getSelectedFile();
						FileInputStream inputStream = new FileInputStream( file );
						try {
							synthPanel.loadBank( inputStream );
						} finally {
							inputStream.close();
						}
					} catch( Exception exception ) {
						JOptionPane.showMessageDialog( LiquinthJS.this,
							exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
					}
				}
			}
		} );
		JMenuItem saveMenuItem = new JMenuItem( "Save Bank" );
		final JFileChooser saveFileChooser = new JFileChooser();
		saveFileChooser.setFileFilter( new FileNameExtensionFilter( "Sound Bank", "liq" ) );
		saveMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				int result = saveFileChooser.showSaveDialog( LiquinthJS.this );
				if( result == JFileChooser.APPROVE_OPTION ) {
					try {
						File file = loadFileChooser.getSelectedFile();
						if( file.exists() ) {
							throw new Exception( "File already exists!" );
						}
						FileOutputStream outputStream = new FileOutputStream( file );
						try {
							synthPanel.saveBank( outputStream );
						} finally {
							outputStream.close();
						}
					} catch( Exception exception ) {
						JOptionPane.showMessageDialog( LiquinthJS.this,
							exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
					}
				}
			}
		} );
		JMenuItem quitMenuItem = new JMenuItem( "Quit" );
		quitMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				player.stop();
				dispose();
			}
		} );
		fileMenu.add( loadMenuItem );
		fileMenu.add( saveMenuItem );
		fileMenu.add( new JSeparator() );
		fileMenu.add( quitMenuItem );
		menuBar.add( fileMenu );
		setJMenuBar( menuBar );
		JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		panel.add( audioSelector, gbc );
		panel.add( midiSelector, gbc );
		gbc.weighty = 1;
		panel.add( new LogoPanel(), gbc );
		gbc.weighty = 0;
		panel.add( synthPanel, gbc );
		panel.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
		getContentPane().add( panel );
	}

	public static void main( String[] args ) {
		final String title = Liquinth.VERSION + " " + Liquinth.AUTHOR;
		System.out.println( title );
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				JFrame frame = new LiquinthJS();
				frame.setTitle( title );
				frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
				frame.pack();
				frame.setVisible( true );
			}
		} );
	}
}
