
package jvst.examples.liquinth;

import java.awt.event.*;

/*
	Will only work properly on US/UK QWERTY layouts.
*/
public class VirtualKeyboard implements KeyListener {
	private static final int[] keys = new int[] {
		KeyEvent.VK_SPACE,
		KeyEvent.VK_ENTER,
		KeyEvent.VK_F1,
		KeyEvent.VK_F2,
		KeyEvent.VK_F3,
		KeyEvent.VK_F4,
		KeyEvent.VK_F5,
		KeyEvent.VK_F6,
		KeyEvent.VK_F7,
		KeyEvent.VK_F8,
		KeyEvent.VK_BACK_SLASH,
		KeyEvent.VK_Z, /* C0 = 11 */
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
		KeyEvent.VK_COMMA,
		KeyEvent.VK_L,
		KeyEvent.VK_PERIOD,
		KeyEvent.VK_SEMICOLON,
		KeyEvent.VK_SLASH,
		KeyEvent.VK_Q, 
		KeyEvent.VK_2,
		KeyEvent.VK_W,
		KeyEvent.VK_3,
		KeyEvent.VK_E,
		KeyEvent.VK_4,
		KeyEvent.VK_R,
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
		KeyEvent.VK_MINUS,
		KeyEvent.VK_OPEN_BRACKET,
		KeyEvent.VK_CLOSE_BRACKET
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
		if( key >= 10 ) { /* Note.*/
			synthesizer.noteOn( octave * 12 + key - 11, 127 );
		} else if( key >= 2 ) { /* Set Octave.*/
			octave = key - 2;
			synthesizer.allNotesOff( false );
		} else { /* All notes off.*/
			synthesizer.allNotesOff( key > 0 );
		}
	}

	public void keyReleased( KeyEvent ke ) {
		int key = getKey( ke.getKeyCode() );
		if( key >= 10 ) { /* Note */
			synthesizer.noteOff( octave * 12 + key - 11 );
		}
	}

	public void keyTyped( KeyEvent ke ) {
	}
}
