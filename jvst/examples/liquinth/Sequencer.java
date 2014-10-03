
package jvst.examples.liquinth;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Sequencer {
	private Synthesizer synthesizer;

	public Sequencer( Synthesizer synthesizer ) {
		this.synthesizer = synthesizer;
	}

	/* Save a simple sequence that just sets the controllers. */
	public void savePatch( OutputStream output ) throws IOException {
		writeString( "(" + synthesizer.getVersion() + ")\n", output );
		for( int idx = 0, end = synthesizer.getNumControllers(); idx < end; idx++ ) {
			writeString( synthesizer.getControllerKey( idx ) + synthesizer.getController( idx ) + "\n", output );
		}
	}

	/* Convert the input sequence to a wave file. */
	public void saveWave( String sequence, OutputStream outputStream, int tickLen ) throws IOException {
		byte[] inputBuf = sequence.getBytes( "US-ASCII" );
		int sampleRate = synthesizer.getSamplingRate();
		int numSamples = runSequence( new java.io.ByteArrayInputStream( inputBuf ), null, tickLen );
		writeString( "RIFF", outputStream );
		writeInt( outputStream, numSamples * 2 + 36 ); // Wave chunk length.
		writeString( "WAVE", outputStream );
		writeString( "fmt ", outputStream );
		writeInt( outputStream, 16 ); // Format chunk length.
		writeShort( outputStream, 1 ); // PCM format.
		writeShort( outputStream, 1 ); // Mono.
		writeInt( outputStream, sampleRate );
		writeInt( outputStream, sampleRate * 2 ); // Bytes per sec.
		writeShort( outputStream, 2 ); // Frame size.
		writeShort( outputStream, 16 ); // Bits per sample.
		writeString( "data", outputStream );
		writeInt( outputStream, numSamples * 2 ); // PCM data length.
		runSequence( new java.io.ByteArrayInputStream( inputBuf ), outputStream, tickLen );
	}

	/*	Run the sequence contained within the specified InputStream into the Synthesizer.
		The sequence consists of commands in ASCII text separated by whitespace:
			(Comments are in brackets.)
			(Set controller with key "wf" to 127.) wf127
			(Key On 3 keys.) :a-5 :c-5 :e-5
			(Wait 50 ticks.) +50
			(Key Off.) /a-5 /c-5 /e-5
			(Wait 200 ticks.) +200
	   The resulting audio is written to the output (as 16 bit little-endian mono PCM) if it is not null.
	   The tickLen parameter determines the number of samples per tick of the sequence.
	   The total length of the sequence in samples is returned. */
	public synchronized int runSequence( InputStream inputStream, OutputStream outputStream, int tickLen ) throws IOException {
		int line = 1, numSamples = 0, chr = inputStream.read();
		char[] inputBuf = new char[ 8 ];
		int[] audioBuf = new int[ tickLen ];
		byte[] outputBuf = new byte[ tickLen * 2 ];
		synthesizer.allNotesOff( true );
		while( chr > 0 ) {
			while( ( chr > 0 && chr <= 32 ) || chr == '(' ) {
				if( chr == 10 ) line++;
				if( chr == '(' ) {
					while( chr > 0 && chr != ')' ) {
						if( chr == 10 ) line++;
						chr = inputStream.read();
					}
				}
				chr = inputStream.read();
			}
			int len = 0;
			while( chr > 32 && chr != '(' ) {
				if( len < inputBuf.length ) {
					inputBuf[ len++ ] = ( char ) chr;
				} else {
					throw new IllegalArgumentException( "Error on line " + line + ": Token '" + new String( inputBuf, 0, len ) + "' too long." );
				}
				chr = inputStream.read();
			}
			if( len > 0 ) {
				try {
					int ticks = runCommand( new String( inputBuf, 0, len ) );
					if( outputStream != null ) {
						for( int tick = 0; tick < ticks; tick++ ) {
							synthesizer.getAudio( audioBuf, tickLen );
							for( int outputIdx = 0; outputIdx < outputBuf.length; outputIdx += 2 ) {
								int amp = audioBuf[ outputIdx >> 1 ];
								outputBuf[ outputIdx ] = ( byte ) amp;
								outputBuf[ outputIdx + 1 ] = ( byte ) ( amp >> 8 );
							}
							outputStream.write( outputBuf, 0, outputBuf.length );
						}
					}
					numSamples += tickLen * ticks;
				} catch( IllegalArgumentException exception ) {
					throw new IllegalArgumentException( "Error on line " + line + ": " + exception.getMessage() );
				}
			}
		}
		synthesizer.allNotesOff( true );
		return numSamples;
	}

	/*	Run a single command into the Synthesizer.
		If the command is a wait, the number of ticks is returned. */
	public synchronized int runCommand( String command ) {
		int ticks = 0;
		char chr = command.charAt( 0 );
		if( chr == '+' ) {
			ticks = parseNumber( command.substring( 1 ) );
		} else if( chr >= '/' && chr <= ':' ) {
			int key = parseNote( command.substring( 1 ) );
			int velocity = ( chr - '0' ) * 13;
			if( velocity > 0 ) {
				synthesizer.noteOn( key, velocity < 127 ? velocity : 127 );
			} else {
				synthesizer.noteOff( key );
			}
		} else {
			int idx = 0, end = command.length();
			while( idx < end ) {
				chr = command.charAt( idx );
				if( chr <= '9' ) break;
				idx++;
			}
			if( idx == 0 ) {
				throw new IllegalArgumentException( "Invalid command: '" + command + "'.");
			}
			int ctrlIndex = synthesizer.getControllerIdx( command.substring( 0, idx ) );
			int ctrlValue = parseNumber( command.substring( idx ) );
			synthesizer.setController( ctrlIndex, ctrlValue );
		}
		return ticks;
	}

	/* Convert MIDI key (1-116) to a note string such as "c-5" for use in sequences. */
	public static String keyToNote( int key ) {
		char[] chars = new char[ 3 ];
		String keys = "c-c#d-d#e-f-f#g-g#a-a#b-c-";
		chars[ 0 ] = ( key > 0 && key < 117 ) ? keys.charAt( ( key % 12 ) * 2 ) : '-';
		chars[ 1 ] = ( key > 0 && key < 117 ) ? keys.charAt( ( key % 12 ) * 2 + 1 ) : '-';
		chars[ 2 ] = ( key > 0 && key < 117 ) ? ( char ) ( '0' + key / 12 ) : '-';
		return new String( chars );
	}

	private static int parseNote( String note ) {
		if( note.length() != 3 ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		String keys = "c-c#d-d#e-f-f#g-g#a-a#b-c-";
		int keysIdx = keys.indexOf( note.charAt( 0 ) );
		if( keysIdx < 0 ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		char chr = note.charAt( 1 );
		if( chr == '#' && keys.charAt( keysIdx + 2 ) == keys.charAt( keysIdx ) ) {
			keysIdx += 2;
		} else if( chr != '-' ) {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
		chr = note.charAt( 2 );
		if( chr >= '0' && chr <= '9' ) {
			return ( keysIdx / 2 ) + ( chr - '0' ) * 12;
		} else {
			throw new IllegalArgumentException( "Invalid note: '" + note + "'." );
		}
	}

	private static int parseNumber( String token ) {
		int num = 0;
		for( int idx = 0, end = token.length(); idx < end; idx++ ) {
			char chr = token.charAt( idx );
			if( chr >= '0' && chr <= '9' ) {
				num = num * 10 + chr - '0';
			} else {
				throw new IllegalArgumentException( "Value '" + token + "' is not a number." );
			}
		}
		return num;
	}

	private static void writeInt( OutputStream output, int value ) throws IOException {
		writeShort( output, value );
		writeShort( output, value >> 16 );
	}

	private static void writeShort( OutputStream output, int value ) throws IOException {
		output.write( ( byte ) value );
		output.write( ( byte ) ( value >> 8 ) );
	}

	private static void writeString( String string, OutputStream output ) throws IOException {
		for( int idx = 0, end = string.length(); idx < end; idx++ ) {
			output.write( ( byte ) string.charAt( idx ) );
		}
	}
}
