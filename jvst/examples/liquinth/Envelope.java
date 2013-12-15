
package jvst.examples.liquinth;

public class Envelope {
	private boolean keyIsOn;
	private int sampleRate, attackLen, releaseLen, amplitude;
	
	public Envelope( int samplingRate ) {
		sampleRate = samplingRate;
		setAttackTime( 0 );
		setReleaseTime( 0 );
	}

	public void setAttackTime( int millis ) {
		if( millis < 1 ) millis = 1;
		attackLen = ( sampleRate * millis ) >> 10;
	}

	public void setReleaseTime( int millis ) {
		if( millis < 1 ) millis = 1;
		releaseLen = ( sampleRate * millis ) >> 10;
	}

	public boolean keyIsOn() {
		return keyIsOn;
	}

	public void keyOn() {
		keyIsOn = true;
	}

	public void keyOff() {
		keyIsOn = false;
	}

	public int getAmplitude() {
		return amplitude;
	}

	public void setAmplitude( int amp ) {
		amplitude = amp & Maths.FP_MASK;
	}

	public void update( int length ) {
		if( keyIsOn ) { /* Attack */
			amplitude += ( length << Maths.FP_SHIFT ) / attackLen;
			if( amplitude > Maths.FP_MASK ) {
				amplitude = Maths.FP_MASK;
			}
		} else { /* Release */
			amplitude -= ( length << Maths.FP_SHIFT ) / releaseLen;
			if( amplitude < 0 ) {
				amplitude = 0;
			}
		}
	}
}
