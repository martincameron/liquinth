
package jvst.examples.liquinth;

public class LiquinthJS {
	public static void main( String[] args ) throws java.io.IOException {
		Synthesizer synthesizer = new Liquinth( Player.SAMPLING_RATE * Player.OVERSAMPLE );
		final String title = Liquinth.VERSION + " " + Liquinth.AUTHOR;
		System.out.println( title );
		if( args.length == 0 ) {
			/* Start the GUI. */
			javax.swing.SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					SynthesizerFrame frame = new SynthesizerFrame( synthesizer );
					frame.setTitle( title );
					frame.setDefaultCloseOperation( SynthesizerFrame.EXIT_ON_CLOSE );
					frame.pack();
					frame.setVisible( true );
				}
			} );
		} else if( args.length == 2 ) {
			/* Convert the specified pattern to a wave file. */
			java.io.File inputFile = new java.io.File( args[ 0 ] );
			byte[] inputBuf = new byte[ ( int ) inputFile.length() ];
			new java.io.DataInputStream( new java.io.FileInputStream( inputFile ) ).readFully( inputBuf, 0, inputBuf.length );
			String sequence = new String( inputBuf, "US-ASCII" );
			java.io.OutputStream outputStream = new java.io.FileOutputStream( args[ 1 ] );
			try {
				new Sequencer( synthesizer ).saveWave( sequence, outputStream, synthesizer.getSamplingRate() / 1000 );
			} finally {
				outputStream.close();
			}
		} else {
			System.err.println( "Usage: java " + LiquinthJS.class.getName() + " [input.pat output.wav]" );
		}
	}
}
