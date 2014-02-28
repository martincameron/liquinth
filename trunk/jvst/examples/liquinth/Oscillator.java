
package jvst.examples.liquinth;

public class Oscillator {
	private static final int NUM_TABLES = 10;
	private static final int TABLE_0_PARTIALS = 768;
	private static final int WAVE_LEN = 1 << 11;
	private static final int WAVE_MASK = WAVE_LEN - 1;
	private static final int PHASE_MASK = ( WAVE_LEN << ( Maths.FP_SHIFT + 1 ) ) - 1;

	private static short[][] oddHarmonics, evenHarmonics;
	private static int[] pulseWidth, evenScale, oddScale;

	private int a5Pitch, evnAmp, subAmp, minTab;
	private int ampl1, ampl2, pitch1, pitch2, pWidth1, pWidth2, phase;

	public Oscillator( int samplingRate ) {
		int a5Step = 440 * ( ( WAVE_LEN << Maths.FP_SHIFT ) / samplingRate );
		a5Pitch = Maths.log2( a5Step );
		setPulseWidth( 0 );
		pWidth1 = pWidth2;
	}

	/* Set the level of the even harmonics from 0 (square) to Maths.FP_ONE (sawtooth). */
	public void setEvenHarmonics( int level ) {
		evnAmp = level;
	}

	/* Set the pulse width of the oscillator.
	   The parameter determines the frequency of the odd cycles relative to the current pitch.
	   The frequency of the even cycles are adjusted to maintain the same overall wavelength. */
	public void setPulseWidth( int octaves ) {
		if( octaves < 0 ) octaves = -octaves;
		pWidth2 = ( octaves >> ( Maths.FP_SHIFT - 7 ) ) & 0xFF;
	}

	/* Set the harmonic complexity of the oscillator. */
	public void setComplexity( int value ) {
		minTab = NUM_TABLES - 1 - ( ( value * NUM_TABLES ) >> Maths.FP_SHIFT );
	}
	
	/* Set the amplitude of the sub oscillator (a square wave 1 octave below the current pitch). */
	public void setSubOscillator( int level ) {
		subAmp = level;
	}

	public void setAmplitude( int amplitude ) {
		ampl2 = amplitude;
	}

	/* Pitch is in octaves relative to 440hz (A5)*/
	public void setPitch( int pitch ) {
		pitch2 = pitch;
	}

	public int getPhase() {
		return phase;
	}

	public void setPhase( int phase ) {
		this.phase = phase & PHASE_MASK;
	}

	public void getAudio( int[] outBuf, int offset, int length ) {
		int table = ( a5Pitch + pitch1 + ( pWidth1 << ( Maths.FP_SHIFT - 7 ) ) ) >> Maths.FP_SHIFT;
		if( table < minTab ) table = minTab;
		if( table >= NUM_TABLES ) table = NUM_TABLES - 1;
		int pwid = pWidth1 << Maths.FP_SHIFT;
		int dwid = ( ( pWidth2 - pWidth1 ) << Maths.FP_SHIFT ) / length;
		int step = Maths.exp2( a5Pitch + pitch1 ) << 4;
		int dstp = ( ( Maths.exp2( a5Pitch + pitch2 ) << 4 ) - step ) / length;
		int ampl = ampl1 << 16;
		int damp = ( ( ampl2 << 16 ) - ampl ) / length;
		short[] oddTab = oddHarmonics[ table ];
		short[] evnTab = evenHarmonics[ table ];
		int end = offset + length;
		while( offset < end ) {
			int w = pulseWidth[ pWidth1 ];
			int x = phase >> Maths.FP_SHIFT;
			int y = ( oddTab[ x >> 1 ] * subAmp ) >> Maths.FP_SHIFT;
			x = ( ( x < w ) ? ( x * oddScale[ pWidth1 ] ) : ( x - w ) * evenScale[ pWidth1 ] ) >> Maths.FP_SHIFT;
			y = y + oddTab[ x ] + ( ( evnTab[ x ] * evnAmp ) >> Maths.FP_SHIFT );
			outBuf[ offset++ ] += ( y * ( ampl >> 16 ) ) >> Maths.FP_SHIFT;
			phase = phase + ( step >> 4 );
			if( phase > PHASE_MASK ) {
				pWidth1 = pwid >> Maths.FP_SHIFT;
				phase &= PHASE_MASK;
			}
			pwid += dwid;
			ampl += damp;
			step += dstp;
		}
		pitch1 = pitch2;
		ampl1 = ampl2;
	}

	static {
		/* Generate sine table. */
		short[] sine = new short[ WAVE_LEN ];
		double t = 0, dt = 2 * Math.PI / WAVE_LEN;
		for( int idx = 0; idx < WAVE_LEN; idx++, t += dt ) {
			sine[ idx ] = ( short ) ( Math.sin( t ) * 27000 );
		}
		/* Generate odd/even harmonics with saw/square spectral envelope.*/
		oddHarmonics = new short[ NUM_TABLES ][];
		evenHarmonics = new short[ NUM_TABLES ][];
		int parts = TABLE_0_PARTIALS;
		for( int tab = 0; tab < NUM_TABLES; tab++ ) {
			short[] odd = oddHarmonics[ tab ] = new short[ WAVE_LEN ];
			short[] even = evenHarmonics[ tab ] = new short[ WAVE_LEN ];
			int part = 1;
			while( part <= parts ) {
				int amp = ( -Maths.FP_TWO << Maths.FP_SHIFT ) / ( -Maths.PI * part );
				for( int idx = 0; idx < WAVE_LEN; idx++ ) {
					odd[ idx ] += ( sine[ ( idx * part ) & WAVE_MASK ] * amp ) >> Maths.FP_SHIFT;
				}
				part++;
				amp = ( -Maths.FP_TWO << Maths.FP_SHIFT ) / ( -Maths.PI * part );
				for( int idx = 0; idx < WAVE_LEN; idx++ ) {
					even[ idx ] += ( sine[ ( idx * part ) & WAVE_MASK ] * amp ) >> Maths.FP_SHIFT;
				}
				part++;
			}
			parts >>= 1;
		}
		/* Generate pulse width modulation tables. */
		pulseWidth = new int[ 256 ];
		evenScale = new int[ 256 ];
		oddScale = new int[ 256 ];
		for( int x = 0; x < 256; x++ ) {
			pulseWidth[ x ] = ( WAVE_LEN << Maths.FP_SHIFT ) / Maths.exp2( x << ( Maths.FP_SHIFT - 7 ) );
			evenScale[ x ] = ( WAVE_LEN << Maths.FP_SHIFT ) / ( 2 * WAVE_LEN - pulseWidth[ x ] );
			oddScale[ x ] = ( WAVE_LEN << Maths.FP_SHIFT ) / pulseWidth[ x ];
		}
	}
}
