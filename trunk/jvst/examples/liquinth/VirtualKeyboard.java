
package jvst.examples.liquinth;

import java.awt.event.*;

/*
	Will only work correctly on UK keyboards :/
*/
public class VirtualKeyboard implements KeyListener {
	private static final int[] keys = new int[] {
		KeyEvent.VK_ENTER,
		KeyEvent.VK_SPACE,
		KeyEvent.VK_F1,
		KeyEvent.VK_F2,
		KeyEvent.VK_F3,
		KeyEvent.VK_F4,
		KeyEvent.VK_F5,
		KeyEvent.VK_F6,
		KeyEvent.VK_F7,
		KeyEvent.VK_F8,

		KeyEvent.VK_Z, /* C0 = 10 */
		KeyEvent.VK_S,
		KeyEvent.VK_X,
		KeyEvent.VK_D,
		KeyEvent.VK_C,
		KeyEvent.VK_V,
		KeyEvent.VK_G,
		KeyEvent.VK_B,
		KeyEvent.VK_H,
		KeyEvent.VK_N,
		KeyEvent.VK_J,
		KeyEvent.VK_M,

		KeyEvent.VK_Q, /* C1 = 22 */
		KeyEvent.VK_2,
		KeyEvent.VK_W,
		KeyEvent.VK_3,
		KeyEvent.VK_E,
		KeyEvent.VK_R,
		KeyEvent.VK_5,
		KeyEvent.VK_T,
		KeyEvent.VK_6,
		KeyEvent.VK_Y,
		KeyEvent.VK_7,
		KeyEvent.VK_U,

		KeyEvent.VK_I,
		KeyEvent.VK_9,
		KeyEvent.VK_O,
		KeyEvent.VK_0,
		KeyEvent.VK_P,
	};

	private Synthesizer synthesizer;
	private int octave;

	public VirtualKeyboard( Synthesizer syn ) {
		synthesizer = syn;
		octave = 4;
	}

	private int getKey( int keyCode ) {
		int key = -1;
		for( int idx = 0; idx < keys.length; idx++ ) {
			if( keys[ idx ] == keyCode ) {
				key = idx;
			}
		}
		return key;
	}

	public void keyPressed( KeyEvent ke ) {
		int key = getKey( ke.getKeyCode() );
		if( key >= 10 ) { /* Note */
			synthesizer.noteOn( octave * 12 + key - 10, 127 );
		} else if( key >= 2 ) {
			/* Set Octave */
			octave = key - 2;
			synthesizer.allNotesOff( false );
		} else {
			/* Space or Enter (all sound off). */
			synthesizer.allNotesOff( key == 0 );
		}
	}

	public void keyReleased( KeyEvent ke ) {
		int key = getKey( ke.getKeyCode() );
		if( key >= 10 ) { /* Note */
			synthesizer.noteOn( octave * 12 + key - 10, 0 );
		}
	}

	public void keyTyped( KeyEvent ke ) {
	}
}
