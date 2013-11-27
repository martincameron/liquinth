
package jvst.examples.liquinth;

public class Oscillator {
	private static final int NUM_TABLES = 9;
	private static final int TABLE_0_PARTIALS = 768;
	private static final int WAVE_LEN = 1 << 11;
	private static final int WAVE_MASK = WAVE_LEN - 1;
	private static final int PHASE_MASK = ( WAVE_LEN << ( Maths.FP_SHIFT + 1 ) ) - 1;

	private static short[][] oddHarmonics, evenHarmonics;

	private int a5Pitch, evnAmp, minTab, pulseWidth;
	private int ampl1, ampl2, pitch1, pitch2, phase;

	public Oscillator( int samplingRate ) {
		int a5Step = 440 * ( ( WAVE_LEN << Maths.FP_SHIFT ) / samplingRate );
		a5Pitch = Maths.log2( a5Step );
	}

	/* Set the level of the even harmonics from 0 (square) to Maths.FP_ONE (sawtooth). */
	public void setEvenHarmonics( int level ) {
		evnAmp = level;
	}

	/* Set the pulse width of the oscillator.
	   The parameter determines the frequency of the odd cycles relative to
	   the current pitch. The frequency of the even cycles are adjusted to
	   maintain the same overall wavelength. */
	public void setPulseWidth( int octaves ) {
		if( octaves <= -Maths.FP_ONE ) octaves = 1 - Maths.FP_ONE;
		pulseWidth = octaves;
	}

	/* Set the harmonic complexity of the oscillator. */
	public void setComplexity( int value ) {
		minTab = NUM_TABLES - 1 - ( ( value * NUM_TABLES ) >> Maths.FP_SHIFT );
	}

	public void setAmplitude( int amplitude, boolean now ) {
		ampl2 = amplitude;
		if( now ) ampl1 = ampl2;
	}

	/* Pitch is in octaves relative to 440hz (A5)*/
	public void setPitch( int pitch, boolean now ) {
		pitch2 = pitch;
		if( now ) pitch1 = pitch2;
	}

	public int getPhase() {
		return phase;
	}

	public void setPhase( int phase ) {
		this.phase = phase & PHASE_MASK;
	}

	public void getAudio( int[] outBuf, int offset, int length ) {
		int table = ( a5Pitch + pitch1 + pulseWidth ) >> Maths.FP_SHIFT;
		if( table < minTab ) table = minTab;
		if( table >= NUM_TABLES ) table = NUM_TABLES - 1;
		int stepA1 = Maths.exp2( a5Pitch + pitch1 + pulseWidth ) << 4;
		int stepA2 = Maths.exp2( a5Pitch + pitch2 + pulseWidth ) << 4;
		int dstepA = ( stepA2 - stepA1 ) / length;
		int stepB1 = Maths.exp2( a5Pitch + pitch1 - Maths.log2( Maths.FP_TWO - Maths.exp2( -pulseWidth ) ) ) << 4;
		int stepB2 = Maths.exp2( a5Pitch + pitch2 - Maths.log2( Maths.FP_TWO - Maths.exp2( -pulseWidth ) ) ) << 4;		
		int dstepB = ( stepB2 - stepB1 ) / length;
		int ampl = ampl1 << 16;
		int damp = ( ( ampl2 << 16 ) - ampl ) / length;
		int phase = this.phase;
		short[] oddTab = oddHarmonics[ table ];
		short[] evnTab = evenHarmonics[ table ];
		for( int end = offset + length; offset < end; offset++ ) {
			int x = phase >> Maths.FP_SHIFT;
			int y = oddTab[ x & WAVE_MASK ] + ( ( evnTab[ x & WAVE_MASK ] * evnAmp ) >> Maths.FP_SHIFT );
			outBuf[ offset ] += ( y * ( ampl >> 16 ) ) >> Maths.FP_SHIFT;
			phase = ( phase + ( ( x & WAVE_LEN ) > 0 ? ( stepB1 >> 4 ) : ( stepA1 >> 4 ) ) ) & PHASE_MASK;
			ampl += damp;
			stepA1 += dstepA;
			stepB1 += dstepB;
		}
		this.phase = phase;
		ampl1 = ampl2;
		pitch1 = pitch2;
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
	}
}
