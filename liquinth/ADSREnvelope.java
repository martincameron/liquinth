
package liquinth;

/*
	Not used, not tested :)
*/
public class ADSREnvelope {
	private static final int
		 FINISH = 0,
		 ATTACK = 1,
		  DECAY = 2,
		SUSTAIN = 3,
		RELEASE = 4;

	private boolean key_on;
	private int tick_len, phase;
	private int attack_delta, decay_delta, release_delta;
	private int sustain_amp, amplitude;

	public ADSREnvelope( int sampling_rate ) {
		tick_len = sampling_rate / 1000;
		set_attack_time( 0 );
		set_decay_time( 0 );
		set_release_time( 0 );
		set_sustain_level( Maths.FP_ONE );
	}

	public void set_attack_time( int millis ) {
		attack_delta = Maths.FP_ONE;
		if( millis > 0 && millis < Maths.FP_ONE ) {
			attack_delta = Maths.FP_ONE / millis;
		}
	}

	public void set_decay_time( int millis ) {
		decay_delta = Maths.FP_ONE;
		if( millis > 0 && millis < Maths.FP_ONE ) {
			decay_delta = Maths.FP_ONE / millis;
		}
	}

	public void set_release_time( int millis ) {
		release_delta = Maths.FP_ONE;
		if( millis > 0 && millis < Maths.FP_ONE ) {
			release_delta = Maths.FP_ONE / millis;
		}
	}

	public void set_sustain_level( int amp ) {
		if( amp < 0 ) amp = 0;
		if( amp > Maths.FP_ONE ) amp = Maths.FP_ONE;
		sustain_amp = amp;
	}

	public boolean is_key_on() {
		return key_on;
	}

	public void key_on() {
		key_on = true;
		phase = ATTACK;
		if( attack_delta >= Maths.FP_ONE ) {
			phase = DECAY;
			amplitude = Maths.FP_ONE;
			if( decay_delta >= Maths.FP_ONE ) {
				phase = SUSTAIN;
				amplitude = sustain_amp;
			}
		}
	}

	public void key_off( boolean silence ) {
		key_on = false;
		phase = RELEASE;
		if( release_delta >= Maths.FP_ONE || silence ) {
			amplitude = 0;
			phase = FINISH;
		}
	}

	public int get_amplitude() {
		return amplitude;
	}

	public void update( int length ) {
		switch( phase ) {
			case ATTACK:
				amplitude = amplitude + attack_delta * length / tick_len;
				if( amplitude >= Maths.FP_ONE ) {
					amplitude = Maths.FP_ONE;
					phase = DECAY;
				}
				break;
			case DECAY:
				amplitude = amplitude - decay_delta * length / tick_len;
				if( amplitude <= sustain_amp ) {
					amplitude = sustain_amp;
					phase = SUSTAIN;
				}
				break;
			case RELEASE:
				amplitude = amplitude - release_delta * length / tick_len;
				if( amplitude < 0 ) {
					amplitude = 0;
					phase = FINISH;
				}
				break;
		}
	}
}
