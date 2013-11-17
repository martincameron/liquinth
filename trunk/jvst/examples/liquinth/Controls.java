
package jvst.examples.liquinth;

public interface Controls {
	public int getNumControllers();
	public String getControllerName( int control );
	public int getController( int controller );
	public void setController( int controller, int value );
	public void setModWheel( int value );
	public void setPitchWheel( int value );
	public int mapMIDIController( int controller );
}
