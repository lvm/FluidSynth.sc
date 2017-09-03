/*
        FluidSynth class

        (c) 2017 by Mauro <mauro@sdf.org>
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
  //var fluid_output; // Don't think this does anything.

  *initClass{
    fluidsynth_bin = "which fluidsynth".unixCmdGetStdOut.replace(Char.nl, "").asString;
    valid_audio_servers = [\alsa, \file, \jack, \oss, \pulseaudio];
  }

  *new {
    |audio_server channels commands_file|
    // singleton pattern
    if(fluidsynth.isNil){
      fluidsynth = super.new;
      fluidsynth.init(audio_server, channels, commands_file);
    }
    ^fluidsynth;
  }

  init {|audio_server=\jack, channels=16, commands_file=""|
    var audioServer, chan, cmds;

    // also, if audioServer is jack autoconnect.
    audioServer = if (audio_server==\jack){
      " -j -a " ++ audio_server;
    }{
      " -a " ++ audio_server;
    };

    chan = if (channels >=16 && channels <= 256){
      " -K " ++ channels
    }{
      error("channels should be an integer between 16 and 256");
    };

    cmds = "";
    if (File.exists(commands_file.standardizePath)){
        " -f " ++ commands_file.standardizePath
    };

    fluidsynth_args = " -sl" ++ audioServer ++ chan ++ " " ++ cmds;
    fluidsynth_pipe = Pipe.new("% %".format(fluidsynth_bin, fluidsynth_args), "w");
/*    fluid_output = Routine { // Fairly sure this isn't doing anything.
      var line =     fluidsynth_pipe.getLine;
      while(
        { line.notNil },
        {
          line.postln;
            line =     fluidsynth_pipe.getLine;
        }
      );
      fluidsynth_pipe.close;
    };*/
    "FluidSynth is running!".postln;
  }

  send {|message|
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

}

FluidCommands {

  *setGain {
    |gain|

    ^format("\ngain %", gain.asFloat.clip(0.01, 4.99));
  }

  *listChannels {
    ^"\nchannels";
  }

  *listSoundfonts {
    ^"\nfonts";
  }

  *listInstruments {
    |soundfont|

    ^format("\ninst %", soundfont);
  }

  *loadSoundfont {
    |soundfont|

    if (soundfont.isNil) {
      Error("TO_DO").throw;
    };

    ^format("\nload %", soundfont);
  }

  *unloadSoundfont {
    |soundfont|

    if (soundfont.isNil) {
      Error("TO_DO").throw;
    };

    ^format("\nunload %", soundfont);
  }

  *selectInstruments {
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
    }

    ^select_cmd;
  }

  *save {
    |filename, commands|
    var f;

    if (
      filename.isNil.not and: (commands.isNil.not),
      {
        f = File(filename.standardizePath, "w");
        f.write(commands ++ "\n");
        f.close;
      },
      {
        Error("TO_DO");
      }
    );
    ^"FluidSynth Commands Saved";
  }
}