
package liquinth;

public class Oscillator {
	private static final int
		LOG2_NUM_WF = 7,
		LOG2_WF_LEN = 8,
		NUM_WF = 1 << LOG2_NUM_WF,
		WF_LEN = 1 << LOG2_WF_LEN,
		WF_CYCLE = Maths.FP_ONE << LOG2_WF_LEN,
		WF_MASK = WF_CYCLE - 1;

	private static final int[]
		wf_pnt_x = {    0, 128, 128, 256 },
		wf_saw_y = { -128,   0,   0, 128 },
		wf_sqr_y = {  -64, -64,  64,  64 };

	private static byte[][] waveforms;
	private int a5_pitch, waveform, wf_idx;
	private int ampl_1, ampl_2;
	private int wf_step_1, wf_step_2;

	private static byte[] gen_wave( int wave ) {
		int tab_idx, x, c, wc, dy, dx;
		byte[] wf;
		wf = new byte[ WF_LEN ];
		tab_idx = -1;
		dy = dx = c = 0;
		for( x = 0; x < WF_LEN; x++ ) {
			if( x >= wf_pnt_x[ tab_idx + 1 ] ) {
				while( x >= wf_pnt_x[ tab_idx + 1 ] ) tab_idx++;
				wc = wf_saw_y[ tab_idx ];
				c = ( wf_sqr_y[ tab_idx ] - wc ) * wave / NUM_WF + wc;
				wc = wf_saw_y[ tab_idx + 1 ];
				dy = ( wf_sqr_y[ tab_idx + 1 ] - wc ) * wave / NUM_WF + wc;
				dy = dy - c;
				dx = wf_pnt_x[ tab_idx + 1 ] - wf_pnt_x[ tab_idx ];
			}
			wf[ x ] = ( byte ) ( dy * ( x - wf_pnt_x[ tab_idx ] ) / dx + c );
		}
		return wf;
	}

	public Oscillator( int sampling_rate ) {
		int a5_step, idx;
		a5_step = ( 55 << Maths.FP_SHIFT + LOG2_WF_LEN ) / ( sampling_rate >> 3 );
		a5_pitch = Maths.log2( a5_step );
		if( waveforms == null ) {
			waveforms = new byte[ NUM_WF ][];
			for( idx = 0; idx < NUM_WF; idx++ ) {
				waveforms[ idx ] = gen_wave( idx );
			}
			set_wave( 0 );
		}
	}

	public void set_wave( int wave ) {
		wave &= Maths.FP_MASK;
		waveform = wave >> Maths.FP_SHIFT - LOG2_NUM_WF;
	}

	public void set_amplitude( int amplitude, boolean now ) {
		ampl_2 = amplitude;
		if( now ) {
			ampl_1 = ampl_2;
		}
	}

	/* Pitch is in octaves relative to 440hz (A5)*/
	public void set_pitch( int pitch, boolean now ) {
		wf_step_2 = Maths.exp2( a5_pitch + pitch );
		if( now ) {
			wf_step_1 = wf_step_2;
		}
	}

	public int get_phase() {
		return wf_idx;
	}

	public void set_phase( int phase ) {
		wf_idx = phase & WF_MASK;
	}

	public void get_audio( int[] out_buf, int offset, int length ) {
		int widx, step, dstp, ampl, damp;
		int out_idx, out_ep1, y;
		byte[] wf;
		wf = waveforms[ waveform ];
		widx = wf_idx;
		step = wf_step_1 << 8;
		dstp = ( wf_step_2 - wf_step_1 << 8 ) / length;
		ampl = ampl_1 << 8;
		damp = ( ampl_2 - ampl_1 << 8 ) / length;
		out_idx = offset;
		out_ep1 = offset + length;
		while( out_idx < out_ep1 ) {
			y = wf[ widx >> Maths.FP_SHIFT ];
			out_buf[ out_idx++ ] += y * ampl >> Maths.FP_SHIFT;
			widx = ( widx + ( step >> 8 ) ) & WF_MASK;
			step += dstp;
			ampl += damp;
		}
		wf_step_1 = wf_step_2;
		ampl_1 = ampl_2;
		wf_idx = widx;
	}
}
