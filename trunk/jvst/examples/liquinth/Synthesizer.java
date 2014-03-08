
package jvst.examples.liquinth;

public interface Synthesizer {
	public int setSamplingRate( int samplingRate );
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
	public int getModulationControlIdx();
	public int getPortamentoControlIdx();
	public int getWaveformControlIdx();
	public int getAttackControlIdx();
	public int getReleaseControlIdx();
	public int getCutoffControlIdx();
	public int getResonanceControlIdx();
	public int programChange( int programIdx );
	public void getAudio( int[] mixBuf, int length );
}
