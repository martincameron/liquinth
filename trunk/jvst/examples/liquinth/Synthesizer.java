
package jvst.examples.liquinth;

public interface Synthesizer {
	public char getVersion();
	public void noteOn( int key, int velocity );
	public void noteOff( int key );
	public void allNotesOff( boolean soundOff );
	public int getNumControllers();
	public String getControllerName( int controlIdx );
	public int getController( int controlIdx );
	public void setController( int controlIdx, int value );
	public void resetAllControllers();
	public void setPitchWheel( int value );
	public int getModulationController();
	public int getPortamentoController();
	public int getWaveformController();
	public int getAttackController();
	public int getReleaseController();
	public int getCutoffController();
	public int getResonanceController();	
	public void getAudio( int[] mixBuf, int length );
}
