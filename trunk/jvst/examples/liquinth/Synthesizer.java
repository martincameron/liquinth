
package jvst.examples.liquinth;

public interface Synthesizer {
	public void noteOn( int key, int velocity );
	public void allNotesOff( boolean soundOff );
	public int getNumControllers();
	public String getControllerName( int control );
	public int getController( int controller );
	public void setController( int controller, int value );
	public void setModWheel( int value );
	public void setPitchWheel( int value );
	public int mapMIDIController( int controller );
}
