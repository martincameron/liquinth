
package jvst.examples.liquinth;

public interface Synthesizer {
	public void noteOn( int key, int velocity );
	public void noteOff( int key );
	public void allNotesOff( boolean soundOff );
	public int getNumControllers();
	public String getControllerName( int controlIdx );
	public int getController( int controlIdx );
	public void setController( int controlIdx, int value );
	public void resetAllControllers();
	public void setPitchWheel( int value );
	public void setModulationController( int controlIdx );
	public int getModulationController();
	public int getPortamentoController();
	public int getWaveformController();
	public int getAttackController();
	public int getReleaseController();
	public int getCutoffController();
	public int getResonanceController();
	public int programChange( int progIdx );
	public String getProgramName( int progIdx );
	public void storeProgram( String name );
	public boolean loadProgram( String program );
	public String saveProgram();
}
