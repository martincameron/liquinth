
package liquinth;

/*
	Moog Filter (Variation 2) from http://www.musicdsp.org
	Type : 24db resonant lowpass
	References : Stilson/Smith CCRMA paper., Timo Tossavainen (?) version

	I have no idea how this works :/
*/
public class MoogFilter {
	private static final int[] wave_shape = {
		-32767, -32512, -32256, -31744, -30720, -28672, -24576, -16384,     0,
		 16384,  24576,  28672,  30720,  31744,  32256,  32512,  32767, 32767
	};

	private float cutoff, cutoff_dest, resonance;
	private float i1, i2, i3, i4;
	private float o1, o2, o3;
	private int o4;

	public void set_cutoff( float cut ) {
		if( cut < 0 ) cut = 0;
		if( cut > 1 ) cut = 1;
		cutoff_dest = cut;
	}

	public void set_resonance( float res ) {
		if( res < 0 ) res = 0;
		if( res > 4 ) res = 4;
		resonance = res;
	}

	public void filter( int[] buf, int length ) {
		int idx, i, c, m, x;
		float cutoff_delta, in, f1, f2, f4, fb, fk;
		cutoff_delta = ( cutoff_dest - cutoff ) / length;
		idx = 0;
		while( idx < length ) {
			in = buf[ idx ];
			cutoff += cutoff_delta;
			f1 = cutoff * 1.16f;
			f2 = f1 * f1;
			f4 = f2 * f2;
			fb = resonance * ( 1 - 0.15f * f2 );
			fk = 1 - f1;
			in = in - o4 * fb;
			in = in * 0.35013f * f4;
			o1 = in + 0.3f * i1 + fk * o1;
			i1 = in;
			o2 = o1 + 0.3f * i2 + fk * o2;
			i2 = o1;
			o3 = o2 + 0.3f * i3 + fk * o3;
			i3 = o2;
			o4 = ( int ) ( o3 + 0.3f * i4 + fk * o4 );
			i4 = o3;
			/* Soft clipping.*/
			o4 += 131072;
			if( o4 < 0 ) o4 = 0;
			if( o4 > 262144 ) o4 = 262144;
			i = o4 >> 14;
			c = wave_shape[ i ];
			m = wave_shape[ i + 1 ] - c;
			x = o4 & 0x3FFF;
			o4 = ( m * x >> 14 ) + c;	
			buf[ idx++ ] = o4;
		}
	}
}
