
package jvst.examples.liquinth;

public interface Keyboard {
	public void noteOn( int key, int velocity );
	public void allNotesOff( boolean soundOff );
}
