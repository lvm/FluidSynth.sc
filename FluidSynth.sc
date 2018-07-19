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
    |audio_server midi_channels audio_channels|
    // singleton pattern
    if(fluidsynth.isNil){
      fluidsynth = super.new;
      fluidsynth.init(audio_server, midi_channels, audio_channels);
    }
    ^fluidsynth;
  }

  init {
    |audio_server=\jack, midi_channels=16 audio_channels=1|
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

    fluidsynth_args = " -sl" ++ audioServer ++ jackMulti ++ mdchan ++ auchan;
    fluidsynth_pipe = Pipe.new("% %".format(fluidsynth_bin, fluidsynth_args), "w");
    "FluidSynth is running!".postln;
  }

  pr_send {
    |message|
    fluidsynth_pipe.write("%\n".format(message));
    fluidsynth_pipe.flush;
  }

  stop {
    fluidsynth_pipe.close;
    FluidSynth.pr_close;
    "FluidSynth is stopped!".postln;
  }

  /* Make sure that fluidsynth is set to nil once it's stopped so it can be reopened later */
  *pr_close{
    fluidsynth = nil;
  }


  /*
  FluidSynth Commands
  */

  setGain {
    |gain|
    this.pr_send(format("\ngain %", gain.asFloat.clip(0.01, 4.99)));
  }

  setProgram {
    |chan, prog|
    this.pr_send(format("\nprog % %", chan.clip(0,15), prog.clip(0, 127)));
  }

  listChannels {
    this.pr_send("\nchannels");
  }

  listSoundfonts {
    this.pr_send("\nfonts");
  }

  listInstruments {
    |soundfont|
    this.pr_send(format("\ninst %", soundfont));
  }

  loadSoundfont {
    |soundfont|
    if (soundfont.isNil) { Error("TO_DO").throw; };
    this.pr_send(format("\nload %", soundfont));
  }

  unloadSoundfont {
    |soundfont|
    if (soundfont.isNil) { Error("TO_DO").throw; };
    this.pr_send(format("\nunload %", soundfont));
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

    this.pr_send(select_cmd);
  }

}