/*
        FluidSynth class

        (c) 2017 by Mauro <mauro@sdf.org>, Cian O'Connor <cian.oconnor@gmail.com>
        http://cyberpunk.com.ar/

        A very basic fluidsynth "front-end".
        Reference:
        * https://sourceforge.net/p/fluidsynth/wiki/FluidSettings/

        Note: Requires `fluidsynth` installed in the system.
*/

FluidSynth {
  classvar fluidsynth; // Holds the singleton
  classvar <fluidsynth_bin; // command line location on this computer
  classvar <valid_audio_servers; // make sure the server matches.

  var audio_server, channels, commands_file;
  var fluidsynth_args;
  var fluidsynth_pipe;

  *initClass{
    fluidsynth_bin = "which fluidsynth".unixCmdGetStdOut.replace(Char.nl, "").asString;
    valid_audio_servers = [\alsa, \file, \jack, \oss, \pulseaudio];
  }

  *new {
    |audio_server midi_channels audio_channels extra_args|
    // singleton pattern
    if(fluidsynth.isNil){
      fluidsynth = super.new;
      fluidsynth.init(audio_server, midi_channels, audio_channels, extra_args);
    }
    ^fluidsynth;
  }

  init {
    |audio_server=\jack, midi_channels=16 audio_channels=1, extra_args=""|
    var audioServer, jackMulti, mdchan, auchan;

    audioServer = if (
      audio_server.isNil.not and:
      (valid_audio_servers.atIdentityHash(audio_server) == 0),
      { audio_server },
      { "jack" });
    // also, if audioServer is jack enable multi chan.
    jackMulti = if (
      audioServer == "jack",
      { " -o audio.jack.multi=true" },
      { "" });
    // and autoconnect.
    audioServer = if (
      audioServer == "jack",
      { format(" -j -a %", audioServer) },
      { format(" -a %", audioServer) });

    mdchan = if (
      midi_channels.isNil.not and: (midi_channels.isKindOf(Integer)),
      { format(" -K %", midi_channels.asInt.clip(16, 256)) },
      { " -K 16" });

    auchan = if (
      audio_channels.isNil.not and: (audio_channels.isKindOf(Integer)),
      { format(" -L %", audio_channels.asInt.clip(1, 16)) },
      { " -L 1" });

    fluidsynth_args = " -sl" ++ audioServer ++ jackMulti ++ mdchan ++ auchan + extra_args;
    fluidsynth_pipe = Pipe.new("% %".format(fluidsynth_bin, fluidsynth_args), "w");
    "FluidSynth is running!".postln;
  }

  prSend {
    |message|
    fluidsynth_pipe.write("%\n".format(message));
    fluidsynth_pipe.flush;
  }

  stop {
    fluidsynth_pipe.close;
    FluidSynth.prClose;
    "FluidSynth is stopped!".postln;
  }

  /* Make sure that fluidsynth is set to nil once it's stopped so it can be reopened later */
  *prClose{
    fluidsynth = nil;
  }


  /*
  FluidSynth Commands
  */

  // Reverb settings
  reverb {
    |reverb|
    // Turn the reverb on or off
    this.prSend(format("reverb %", reverb));
  }
  rev_preset {
    |preset|
    /*
    Load preset num into the reverb unit

    num:0 roomsize:0.2 damp:0.0 width:0.5 level:0.9
    num:1 roomsize:0.4 damp:0.2 width:0.5 level:0.8
    num:2 roomsize:0.6 damp:0.4 width:0.5 level:0.7
    num:3 roomsize:0.8 damp:0.7 width:0.5 level:0.6
    num:4 roomsize:0.8 damp:1.0 width:0.5 level:0.5
    */
    this.prSend(format("rev_preset %", preset));
  }
  rev_room {
    |room=0.2|
    // Change reverb room size (i.e the reverb time) in the range [0 to 1.0] (default: 0.2)
    this.prSend(format("rev_setroomsize %", room));
  }
  rev_damp {
    |damp=0.0|
    /*
    Change reverb damping in the range [0.0 to 1.0] (default: 0.0)

    When 0.0, no damping.
    Between 0.0 and 1.0, higher frequencies have less reverb time than lower frequencies.
    When 1.0, all frequencies are damped even if room size is at maximum value.
    */
    this.prSend(format("rev_setdamp %", damp));
  }
  rev_width {
    |width=0.5|
    /*
    Change reverb width in the range [0.0 to 100.0] (default: 0.5)
    num value defines how much the right channel output is separated of the left channel output.

    When 0.0, there is no separation (i.e the output is mono).
    When 100.0, the stereo effect is maximum.
    */
    this.prSend(format("rev_width %", width));
  }
  rev_level {
    |level=0.9|
    // Change reverb output level in the range [0.0 to 1.0] (default: 0.9)
    this.prSend(format("rev_setlevel %", level));
  }

  // Channels and Soundfont files

  setGain {
    |gain|
    this.prSend(format("\ngain %", gain.asFloat.clip(0.01, 4.99)));
  }

  setProgram {
    |chan, prog|
    this.prSend(format("\nprog % %", chan.clip(0,15), prog.clip(0, 127)));
  }

  listChannels {
    this.prSend("\nchannels");
  }

  listSoundfonts {
    this.prSend("\nfonts");
  }

  listInstruments {
    |soundfont|
    this.prSend(format("\ninst %", soundfont));
  }

  loadSoundfont {
    |soundfont|
    if (soundfont.isNil) { Error("TO_DO").throw; };
    this.prSend(format("\nload %", soundfont));
  }

  unloadSoundfont {
    |soundfont|
    if (soundfont.isNil) { Error("TO_DO").throw; };
    this.prSend(format("\nunload %", soundfont));
  }

  selectInstruments {
    |instruments|
    var select_cmd = "";
    var values;

    if (instruments.isNil.not and: (instruments.isKindOf(Array))) {
      instruments.collect {
        |inst|
        if (inst.isKindOf(Dictionary)) {
          values = [inst.at(\chan), inst.at(\sfont), inst.at(\bank), inst.at(\prog)];
          select_cmd = select_cmd ++ format("\nselect % % % %", *values);
        }
      };
    };

    this.prSend(select_cmd);
  }

}