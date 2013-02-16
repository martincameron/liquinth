
package jvst.examples.liquinth;

public interface AudioSource {
	public void setSamplingRate( int samplingRate );
	public int[] allocateMixBuf( int frames );
	public void getAudio( int[] mixBuf, int length );
}
