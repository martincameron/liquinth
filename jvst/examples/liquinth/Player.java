
package jvst.examples.liquinth;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/*
	BUF_FRAMES determines the latency.
	Default value of 1024 gives 21.3ms at 48000hz.
*/
public class Player implements Runnable {
	public static final int SAMPLING_RATE = 48000;
	public static final int OVERSAMPLE = 2;
	private static final int BUF_FRAMES = 1024;
	private static final int BUF_BYTES = BUF_FRAMES * 2;

	private Synthesizer synthesizer;
	private SourceDataLine audioLine;
	private boolean playing;

	public Player( Synthesizer synth, Mixer mixer ) throws LineUnavailableException {
		synthesizer = synth;
		AudioFormat audioFormat = new AudioFormat( SAMPLING_RATE, 16, 1, true, false );
		DataLine.Info lineInfo = new DataLine.Info( SourceDataLine.class, audioFormat, BUF_BYTES );
		audioLine = ( SourceDataLine ) mixer.getLine( lineInfo );
		audioLine.open( audioFormat, BUF_BYTES );
	}

	public void run() {
		if( !playing ) {
			int[] mixBuf = new int[ BUF_FRAMES * OVERSAMPLE ];
			byte[] outBuf = new byte[ BUF_BYTES ];
			audioLine.start();
			playing = true;
			while( playing ) {
				int outIdx = 0, mixIdx = 0, mixEnd = BUF_FRAMES * OVERSAMPLE;
				synthesizer.getAudio( mixBuf, mixEnd );
				while( mixIdx < mixEnd ) {
					int out = mixBuf[ mixIdx ];
					outBuf[ outIdx     ] = ( byte ) ( out & 0xFF );
					outBuf[ outIdx + 1 ] = ( byte ) ( out >> 8 );
					outIdx += 2;
					mixIdx += OVERSAMPLE;
				}
				audioLine.write( outBuf, 0, BUF_BYTES );
			}
			audioLine.drain();
			audioLine.close();
		}
	}

	public void stop() {
		playing = false;
	}
}
