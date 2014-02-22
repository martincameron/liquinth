
package jvst.examples.liquinth;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class LiquinthJS extends JFrame {
	private Liquinth liquinth;
	private SynthesizerPanel synthesizerPanel;
	private MidiReceiver midiReceiver;
	private MidiDevice midiDevice;
	private Player player;
	private Thread playThread;
	private JFileChooser loadPatchFileChooser, savePatchFileChooser;
	
	public LiquinthJS() {
		liquinth = new Liquinth( Player.SAMPLING_RATE * Player.OVERSAMPLE );
		synthesizerPanel = new SynthesizerPanel( liquinth );
		midiReceiver = new MidiReceiver( synthesizerPanel );
		JMenuBar menuBar = new JMenuBar();
		// Add file menu.
		JMenu fileMenu = new JMenu( "File" );
		UIManager.put( "FileChooser.readOnly", Boolean.TRUE );
		FileNameExtensionFilter filter = new FileNameExtensionFilter( "Patch File", "pat" );
		// Add load bank menu item.
		JMenuItem loadMenuItem = new JMenuItem( "Load Patch" );
		loadPatchFileChooser = new JFileChooser();
		loadPatchFileChooser.setFileFilter( filter );
		loadMenuItem.addActionListener( new LoadPatchMenuItemListener() );
		fileMenu.add( loadMenuItem );
		// Add save bank menu item.
		JMenuItem saveMenuItem = new JMenuItem( "Save Patch" );
		savePatchFileChooser = new JFileChooser();
		savePatchFileChooser.setFileFilter( filter );
		saveMenuItem.addActionListener( new SavePatchMenuItemListener() );
		fileMenu.add( saveMenuItem );
		// Add quit menu item.
		fileMenu.add( new JSeparator() );
		JMenuItem quitMenuItem = new JMenuItem( "Quit" );
		quitMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				player.stop();
				dispose();
			}
		} );
		fileMenu.add( quitMenuItem );
		menuBar.add( fileMenu );
		JMenu optionsMenu = new JMenu( "Options" );
		// Add audio device menu.
		JMenu audioDeviceMenu = new JMenu( "Audio Device" );
		ButtonGroup audioDeviceButtonGroup = new ButtonGroup();
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
		for( int idx = 0; idx < mixerInfo.length; idx++ ) {
			JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( mixerInfo[ idx ].toString() );
			menuItem.addActionListener( new AudioDeviceMenuItemListener( mixerInfo[ idx ] ) );
			audioDeviceButtonGroup.add( menuItem );
			audioDeviceMenu.add( menuItem );
		}
		optionsMenu.add( audioDeviceMenu );
		// Activate the first mixer.
		audioDeviceButtonGroup.getElements().nextElement().doClick();
		// Add MIDI device menu.
		JMenu midiDeviceMenu = new JMenu( "MIDI Input Device" );
		ButtonGroup midiDeviceButtonGroup = new ButtonGroup();
		MidiDevice.Info[] midiInfo = MidiSystem.getMidiDeviceInfo();
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( "None", true );
		menuItem.addActionListener( new MidiDeviceMenuItemListener( null ) );
		midiDeviceButtonGroup.add( menuItem );
		midiDeviceMenu.add( menuItem );
		for( int idx = 0; idx < midiInfo.length; idx++ ) {
			menuItem = new JRadioButtonMenuItem( midiInfo[ idx ].toString() );
			menuItem.addActionListener( new MidiDeviceMenuItemListener( midiInfo[ idx ] ) );
			midiDeviceButtonGroup.add( menuItem );
			midiDeviceMenu.add( menuItem );
		}
		optionsMenu.add( midiDeviceMenu );
		// Add MIDI channel menu.
		JMenu midiChannelMenu = new JMenu( "MIDI Input Channel" );
		ButtonGroup midiChannelButtonGroup = new ButtonGroup();
		for( int channel = 1; channel <= 16; channel++ ) {
			menuItem = new JRadioButtonMenuItem( String.valueOf( channel ) );
			menuItem.addActionListener( new MidiChannelMenuItemListener( channel ) );
			midiChannelButtonGroup.add( menuItem );
			midiChannelMenu.add( menuItem );
		}
		optionsMenu.add( midiChannelMenu );
		// Select first channel.
		midiChannelButtonGroup.getElements().nextElement().doClick();
		menuBar.add( optionsMenu );
		setJMenuBar( menuBar );
		// Create panel for logo and Synthesizer.
		JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = gbc.weighty = 1;
		panel.add( new LogoPanel(), gbc );
		gbc.weighty = 0;
		panel.add( synthesizerPanel, gbc );
		panel.setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
		getContentPane().add( panel );
	}

	private class AudioDeviceMenuItemListener implements ActionListener {
		private Mixer.Info mixerInfo;
		public AudioDeviceMenuItemListener( Mixer.Info info ) {
			mixerInfo = info;
		}
		public void actionPerformed( ActionEvent actionEvent ) {
			try {
				if( player != null ) {
					player.stop();
				}
				if( playThread != null ) {
					while( playThread.isAlive() ) {
						try { playThread.join(); } catch( InterruptedException ie ) {}
					}
				}
				player = new Player( liquinth, AudioSystem.getMixer( mixerInfo ) );
				playThread = new Thread( player );
				playThread.start();
			} catch( Exception exception ) {
				JOptionPane.showMessageDialog( LiquinthJS.this, exception.getMessage(),
					"Unable to open audio device", JOptionPane.ERROR_MESSAGE );
			}
		}
	}

	private class LoadPatchMenuItemListener implements ActionListener {
		public void actionPerformed( ActionEvent event ) {
			int result = loadPatchFileChooser.showOpenDialog( LiquinthJS.this );
			if( result == JFileChooser.APPROVE_OPTION ) {
				try {
					File file = loadPatchFileChooser.getSelectedFile();
					FileInputStream inputStream = new FileInputStream( file );
					try {
						synthesizerPanel.loadPatch( inputStream );
					} finally {
						inputStream.close();
					}
				} catch( Exception exception ) {
					JOptionPane.showMessageDialog( LiquinthJS.this,
						exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
				}
			}
		}
	} 

	private class SavePatchMenuItemListener implements ActionListener {
		public void actionPerformed( ActionEvent event ) {
			int result = savePatchFileChooser.showSaveDialog( LiquinthJS.this );
			if( result == JFileChooser.APPROVE_OPTION ) {
				try {
					File file = savePatchFileChooser.getSelectedFile();
					if( file.exists() ) {
						throw new Exception( "File already exists!" );
					}
					FileOutputStream outputStream = new FileOutputStream( file );
					try {
						synthesizerPanel.savePatch( outputStream );
					} finally {
						outputStream.close();
					}
				} catch( Exception exception ) {
					JOptionPane.showMessageDialog( LiquinthJS.this,
						exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE );
				}
			}
		}
	}

	private class MidiDeviceMenuItemListener implements ActionListener {
		private MidiDevice.Info midiInfo;
		public MidiDeviceMenuItemListener( MidiDevice.Info info ) {
			midiInfo = info;
		}
		public void actionPerformed( ActionEvent actionEvent ) {
			if( midiDevice != null ) {
				midiDevice.close();
				midiDevice = null;
			}
			if( midiInfo != null ) {
				try {
					midiDevice = MidiSystem.getMidiDevice( midiInfo );
					midiDevice.open();
					midiDevice.getTransmitter().setReceiver( midiReceiver );
				} catch( MidiUnavailableException exception ) {
					JOptionPane.showMessageDialog( LiquinthJS.this, exception.getMessage(),
						"Unable to open MIDI device", JOptionPane.ERROR_MESSAGE );
					if( midiDevice != null ) {
						midiDevice.close();
						midiDevice = null;
					}
				}
			}
		}
	}

	private class MidiChannelMenuItemListener implements ActionListener {
		private int channel;
		public MidiChannelMenuItemListener( int chan ) {
			channel = chan;
		}
		public void actionPerformed( ActionEvent actionEvent ) {
			midiReceiver.setChannel( channel );
		}
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
