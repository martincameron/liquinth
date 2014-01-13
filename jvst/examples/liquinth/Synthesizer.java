
package jvst.examples.liquinth;

public interface Synthesizer {
	public String saveProgram( String name );
	public void loadProgram( String program );
	public int programChange( int idx );
	public void noteOn( int key, int velocity );
	public void noteOff( int key );
	public void allNotesOff( boolean soundOff );
	public int getNumControllers();
	public String getControllerName( int control );
	public int getController( int controller );
	public void setController( int controller, int value );
	public void resetAllControllers();
	public void setModWheel( int value );
	public void setPitchWheel( int value );
	public int getPortamentoController();
	public int getWaveformController();
	public int getAttackController();
	public int getReleaseController();
	public int getCutoffController();
	public int getResonanceController();
}
