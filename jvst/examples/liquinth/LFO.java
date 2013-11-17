
package jvst.examples.liquinth;

/*
	Low Frequency Oscillator ...
*/
public class LFO {
	private int tickLen, phase;
	private int cycleLen, depth;

	public LFO( int samplingRate ) {
		tickLen = samplingRate / 1000;
		setCycleLen( 1000 );
	}

	public int getPhase() {
		return phase;
	}

	public void setPhase( int ph ) {
		phase = ph & ( Maths.FP_TWO - 1 );
	}

	public void setCycleLen( int millis ) {
		cycleLen = tickLen * millis;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth( int dep ) {
		depth = dep & Maths.FP_MASK;
	}

	public int getAmplitude() {
		return ( Maths.sine( phase ) * depth ) >> Maths.FP_SHIFT;
	}

	public void update( int length ) {
		phase += Maths.FP_TWO * length / cycleLen;
		phase &= Maths.FP_TWO - 1;
	}
}

