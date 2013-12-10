
package jvst.examples.liquinth;

/*
	Moog Filter (Variation 2) from http://www.musicdsp.org
	Type : 24db resonant lowpass
	References : Stilson/Smith CCRMA paper., Timo Tossavainen (?) version

	I have no idea how this works :/
	
	cutoffs for resonant peaks (48khz):
	  55 = 0.006286621
	 110 = 0.012573242
	 220 = 0.02407837
	 440 = 0.048187256
	 880 = 0.092285156
	1660 = 0.17675781
	3320 = 0.32418823
*/
public class MoogFilter {
	private static final float SCALE = 1f / 32768f;
	
	private float fc24khz;
	private float cutoff, cutoffDest, resonance;
	private float i1, i2, i3, i4;
	private float o1, o2, o3, o4;

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

	public void filter( int[] buf, int offset, int length ) {
		float cutoffDelta, in, out, f1, f2, f4, fb, fk;
		cutoffDelta = ( cutoffDest - cutoff ) / length;
		int end = offset + length;
		while( offset < end ) {
			in = buf[ offset ] * SCALE;
			cutoff += cutoffDelta;
			f1 = cutoff * 1.16f;
			f2 = f1 * f1;
			f4 = f2 * f2;
			fb = resonance * ( 1f - 0.15f * f2 );
			fk = 1f - f1;
			in = in - o4 * fb;
			in = in * 0.35013f * f4;
			o1 = in + 0.3f * i1 + fk * o1;
			i1 = in;
			o2 = o1 + 0.3f * i2 + fk * o2;
			i2 = o1;
			o3 = o2 + 0.3f * i3 + fk * o3;
			i3 = o2;
			o4 = o3 + 0.3f * i4 + fk * o4;
			i4 = o3;
			/* Clamp the feedback. */
			if( o4 > 1f ) o4 = 1f;
			if( o4 < -1f ) o4 = -1f;
			/* Waveshaping. */
			out = 1.5f * o4 - 0.5f * o4 * o4 * o4;
			buf[ offset++ ] = ( int ) ( out * 32767f );
		}
	}
}
