
package jvst.examples.liquinth;

/*
	Low Frequency Oscillator ...
*/
public class LFO {
	private int tickLen, cycleLen, phase;

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

	public int getAmplitude() {
		return Maths.sine( phase );
	}

	public void update( int length ) {
		phase += Maths.FP_TWO * length / cycleLen;
		phase &= Maths.FP_TWO - 1;
	}
}
