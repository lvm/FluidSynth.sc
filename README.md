# FluidSynth.sc

Not quite a `fluidsynth` implementation but some sort of _front-end_.  

## Installation

```
Quarks.install("https://github.com/lvm/FluidSynth.sc");
```

## Usage

```
(
~fluid = FluidSynth();
// or
// ~fluid = FluidSynth(audio_server, num_channels);


~fluid.setGain(1);
~fluid.loadSoundfont("~/filename.sf2");
~fluid.listSoundfonts;
~fluid.listInstruments(1);
~fluid.selectInstruments([
  (\chan: 2, \sfont: 1, \bank: 2, \prog: 4),
  (\chan: 9, \sfont: 1, \bank: 128, \prog: 2),
  (\chan: 10, \sfont: 1, \bank: 128, \prog: 1),
]);
~fluid.listChannels;

~fluid.unloadSoundfont(1);
~fluid.stop;
)
```

## Bugs?

[Here](https://github.com/lvm/FluidSynth.sc/issues)

## Authors

(c) 2017 by Mauro Lizaur, Cian O'Connor

## License

See [LICENSE](LICENSE)
