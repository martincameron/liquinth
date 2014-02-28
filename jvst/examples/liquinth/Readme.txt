
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
and more overdrive.
The filter envelope now has an attack phase. If this
is set to zero the filter will release instantaneously
as in previous versions.
The pulse-width control now has a modulation depth,
tied to the vibrato rate.
There is now a sub-oscillator which generates a square
wave one octave below the current frequency.

Version a41 has a few code improvements,
but there should be no changes to the performance.

Version a40 brings a pulse-width control (a simple
implementation that is only really useful for bass
sounds) and a "timbre" setting to control the number of
harmonics in the waveform (basically just exposing the
workings of the antialiasing algorithm). A couple of
minor bugs in the volume/pitch interpolation have been
fixed and the code has been made a little clearer in
places.

There have been substantial improvements since version a30.
The oscillators are now multisampled, resulting in a much
cleaner sound with less processor time.
The overdrive and vibrato are also considerably better.

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

Configuring jVSTwRapper

When configuring Liquinth as a VST instrument, your jVSTwrapper
configuration file should contain the following additional entries:

PluginClass=jvst/examples/liquinth/vst/LiquinthVST
PluginUIClass=jvst/examples/liquinth/vst/LiquinthVSTGUI
ClassPath={WrapperPath}\liquinth-a42.jar

Kind Regards,
Martin
