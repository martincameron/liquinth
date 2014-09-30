
package jvst.examples.liquinth;

/*
	Moog Filter (Variation 2) from http://www.musicdsp.org
	Type : 24db resonant lowpass
	References : Stilson/Smith CCRMA paper., Timo Tossavainen (?) version
	I have no idea how this works :/
*/
public class MoogFilter {
	private static final float SCALE = 1f / 32768f;

	private float fc24khz;
	private float cutoff, cutoffDest, resonance, detune;
	private float ia1, ia2, ia3, ia4;
	private float ib1, ib2, ib3, ib4;
	private float oa1, oa2, oa3, oa4;
	private float ob1, ob2, ob3, ob4;

	public MoogFilter( float samplingRate ) {
		fc24khz = 48000f / samplingRate;
	}

	public void setCutoff( float cut ) {
		cut = cut * fc24khz;
		if( cut < 0f ) cut = 0f;
		if( cut > 1f ) cut = 1f;
		cutoffDest = cut;
	}

	public void setResonance( float res ) {
		if( res < 0f ) res = 0f;
		if( res > 4f ) res = 4f;
		resonance = res;
	}

	public void setDetune( float det ) {
		if( det < 0f ) det = 0f;
		if( det > 1f ) det = 1f;
		detune = 1f - det;
	}

	public void filter( int[] buf, int offset, int length ) {
		float cutoffDelta, in, out1, out2, f1, f2, f4, fb, fk;
		cutoffDelta = ( cutoffDest - cutoff ) / length;
		int end = offset + length;
		while( offset < end ) {
			cutoff += cutoffDelta;
			/* Filter 1.*/
			f1 = cutoff * 1.16f;
			f2 = f1 * f1;
			f4 = f2 * f2;
			fb = resonance * ( 1f - 0.15f * f2 );
			fk = 1f - f1;
			in = buf[ offset ] * SCALE - oa4 * fb;
			in = in * 0.35013f * f4;
			oa1 = in + 0.3f * ia1 + fk * oa1;
			ia1 = in;
			oa2 = oa1 + 0.3f * ia2 + fk * oa2;
			ia2 = oa1;
			oa3 = oa2 + 0.3f * ia3 + fk * oa3;
			ia3 = oa2;
			oa4 = oa3 + 0.3f * ia4 + fk * oa4;
			ia4 = oa3;
			if( oa4 > 1f ) oa4 = 1f;
			if( oa4 < -1f ) oa4 = -1f;
			/* Filter 2.*/
			f1 = f1 * detune;
			f2 = f1 * f1;
			f4 = f2 * f2;
			fb = resonance * ( 1f - 0.15f * f2 );
			fk = 1f - f1;
			in = buf[ offset ] * SCALE - ob4 * fb;
			in = in * 0.35013f * f4;
			ob1 = in + 0.3f * ib1 + fk * ob1;
			ib1 = in;
			ob2 = ob1 + 0.3f * ib2 + fk * ob2;
			ib2 = ob1;
			ob3 = ob2 + 0.3f * ib3 + fk * ob3;
			ib3 = ob2;
			ob4 = ob3 + 0.3f * ib4 + fk * ob4;
			ib4 = ob3;
			if( ob4 > 1f ) ob4 = 1f;
			if( ob4 < -1f ) ob4 = -1f;
			/* Waveshaping. */
			out1 = 1.5f * oa4 - 0.5f * oa4 * oa4 * oa4;
			out2 = 1.5f * ob4 - 0.5f * ob4 * ob4 * ob4;
			buf[ offset++ ] = ( int ) ( ( out1 + out2 ) * 16383f );
		}
	}
}
