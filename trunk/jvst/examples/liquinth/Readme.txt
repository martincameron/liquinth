
Liquinth (c)2014 mumart@gmail.com

Liquinth is a relatively simple polysynth for Java.
It can be used either standalone from the JAR file,
or as a VST instrument via jVSTwRapper.

Please let me know how you get on!

Changes

Version a42 has been almost completely overhauled.
The overall sound quality is vastly improved, with
a much fatter-sounding pulse-width implementation, more
responsive envelopes, a more natural amplitude curve,
more overdrive, and a delay effect.

The filter envelope now has an attack phase. If this
is set to zero the filter will release instantaneously
as in previous versions.

There are now two filters which can be detuned to give
two resonant peaks for a phaser-like effect.

The filter and pulse-width controls now have a modulation
depth, tied to the vibrato rate.

There is now a sub-oscillator which generates a square
wave one octave below the current frequency.

Patches can be saved and restored from the GUI, and also
exported as WAV files containing a sustained note or chord.
Patch files may also include more complex sequences which can
be converted to WAV files using the command-line interface.

Usage

The synthesizer may be run as a standalone application
by executing the JAR file. This will output 48khz audio
with a latency of approximately 25ms.

You can play notes using the computer keyboard if one
of the sliders has focus. Only QWERTY keyboards currently
work correctly. You can set the octave of the computer
keyboard using the function keys, and if any notes get
stuck you can release them with the space bar or the
return key.

When using MIDI input the sliders are assigned to modulation
controllers 20 onwards. You can also use the radio buttons
next to the sliders assign them to MIDI controller 1, which
is usually the default for the first modulation wheel.
Some of the standard MIDI controllers are also mapped automatically,
such as Attack, Release, and Filter Cutoff.

To use the built-in sequencer it is necessary to hand-edit a patch
file containing the notes to be played, such as the example below,
and issue a command such as "java -jar liquinth.jar test.pat test.wav"
to perform the conversion:

(Example patch/sequence. Comments may be put in brackets.)
	od32  (Set volume/overdrive.)
	fc127 (Set filter cut-off.)
	aa32  (Set Volume Attack.)
	ar127 (Set Volume Release.)
	dt2   (Set detune.)
	:c-5  (Key on A-5.)
	+500  (Wait 500ms.)
	:e-5  (Key on B-5.)
	+500  (Wait 500ms.)
	:g-5  (Key on C-5.)
	+500  (Wait 500ms.)
	/c-5  (Key off A-5.)
	/e-5  (Key off B-5.)
	/g-5  (Key off C-5.)
	+4000 (Wait 4s)
(End.)

Configuring jVSTwRapper

When configuring Liquinth as a VST instrument, your jVSTwrapper
configuration file should contain the following additional entries:

PluginClass=jvst/examples/liquinth/vst/LiquinthVST
PluginUIClass=jvst/examples/liquinth/vst/LiquinthVSTGUI
ClassPath={WrapperPath}\liquinth-a42.jar

Cheers!
Martin
