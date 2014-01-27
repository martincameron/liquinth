
package jvst.examples.liquinth;

import javax.sound.sampled.*;

/*
	BUF_FRAMES determines the latency.
	Default value of 1024 gives 21.3ms at 48000hz.
*/
public class Player implements Runnable {
	public static final int SAMPLING_RATE = 48000;
	public static final int OVERSAMPLE = 2;

	private static final int BUF_FRAMES = 1024;
	private static final int BUF_BYTES = BUF_FRAMES * 2;

	private Synthesizer audioSource;
	private AudioFormat audioFormat;
	private SourceDataLine.Info lineInfo;
	private Mixer audioMixer;

	private boolean play, running;

	public Player( Synthesizer synthesizer ) {
		audioSource = synthesizer;
		audioFormat = new AudioFormat( SAMPLING_RATE, 16, 1, true, false );
		lineInfo = new DataLine.Info( SourceDataLine.class, audioFormat, BUF_BYTES );
	}

	public void setMixer( Mixer mixer ) {
		audioMixer = mixer;
	}		

	public boolean isRunning() {
		return running;
	}

	public void run() {
		int mixIdx, mixEnd, outIdx, out;
		int[] mixBuf;
		byte[] outBuf;
		SourceDataLine audioLine;
		if( play ) {
			stop();
		}
		play = true;
		mixBuf = new int[ BUF_FRAMES * OVERSAMPLE ];
		outBuf = new byte[ BUF_BYTES ];
		try {
			audioLine = ( SourceDataLine ) audioMixer.getLine( lineInfo );
			audioLine.open( audioFormat, BUF_BYTES );
		} catch( Exception e ) {
			System.out.println( "Player.run(): Unable to open audio output!" );
			return;
		}
		audioLine.start();
		running = true;
		while( play ) {
			outIdx = 0;
			mixIdx = 0;
			mixEnd = BUF_FRAMES * OVERSAMPLE;
			audioSource.getAudio( mixBuf, mixEnd );
			while( mixIdx < mixEnd ) {
				out = mixBuf[ mixIdx ];
				outBuf[ outIdx     ] = ( byte ) ( out & 0xFF );
				outBuf[ outIdx + 1 ] = ( byte ) ( out >> 8 );
				outIdx += 2;
				mixIdx += OVERSAMPLE;
			}
			audioLine.write( outBuf, 0, BUF_BYTES );
		}
		audioLine.drain();
		audioLine.close();
		running = false;
	}

	public void stop() {
		while( running ) {
			play = false;
			try {
				Thread.sleep( 20 );
			} catch( InterruptedException ie ) {
			}
		}
	}
}
