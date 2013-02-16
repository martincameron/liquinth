
package jvst.examples.liquinth;

public class Envelope {
	private boolean keyIsOn;
	private int tickLen, amplitude;
	private int attackLen, attackDelta;
	private int releaseLen, releaseDelta;

	public Envelope( int samplingRate ) {
		tickLen = samplingRate / 1000;
		setAttackTime( 0 );
		setReleaseTime( 0 );
	}

	public void setAttackTime( int millis ) {
		attackLen = 0;
		attackDelta = 0;
		if( millis > 0 ) {
			attackLen = tickLen * millis;
			attackDelta = ( Maths.FP_ONE << Maths.FP_SHIFT ) / attackLen;
		}
	}

	public void setReleaseTime( int millis ) {
		releaseLen = 0;
		releaseDelta = 0;
		if( millis > 0 ) {
			releaseLen = tickLen * millis;
			releaseDelta = ( Maths.FP_ONE << Maths.FP_SHIFT ) / releaseLen;
		}
	}

	public boolean keyIsOn() {
		return keyIsOn;
	}

	public void keyOn() {
		keyIsOn = true;
		if( attackLen <= 0 ) {
			amplitude = Maths.FP_ONE << Maths.FP_SHIFT;
		}
	}

	public void keyOff( boolean silence ) {
		keyIsOn = false;
		if( releaseLen <= 0 || silence ) {
			amplitude = 0;
		}
	}

	public int getAmplitude() {
		return amplitude >> Maths.FP_SHIFT;
	}

	public void update( int length ) {
		int maxAmplitude;
		if( keyIsOn ) { /* Attack */
			maxAmplitude = Maths.FP_ONE << Maths.FP_SHIFT;
			if( length >= attackLen ) {
				amplitude = maxAmplitude;
			} else {
				amplitude += length * attackDelta;
				if( amplitude > maxAmplitude ) {
					amplitude = maxAmplitude;
				}
			}
		} else { /* Release */
			if( length >= releaseLen ) {
				amplitude = 0;
			} else {
				amplitude -= length * releaseDelta;
				if( amplitude < 0 ) {
					amplitude = 0;
				}
			}
		}
	}
}
