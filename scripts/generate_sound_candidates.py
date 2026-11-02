#!/usr/bin/env python3
"""Render original alarm sound sketches and master them with FFmpeg.

Requires numpy and ffmpeg; --install also uses macOS afconvert.
Default output is the audition set; --production --install builds app resources.
Each composition wraps its note tails and ambience across the loop boundary.
"""

import argparse
import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np


RATE = 44100
RNG = np.random.default_rng(20261001)


def hz(note):
    return 440 * 2 ** ((note - 69) / 12)


def attack(t, seconds):
    return 1 - np.exp(-t / seconds)


def mallet(note, length=2.2, bright=False):
    t = np.arange(int(RATE * length)) / RATE
    f = hz(note)
    # A decaying FM transient over a rounded, sustained fundamental.
    mod = (2.6 if bright else 1.2) * np.exp(-t / .09)
    body = np.sin(2 * np.pi * f * t + mod * np.sin(2 * np.pi * f * 3 * t))
    body *= np.exp(-t / (.34 if bright else .6))
    body += .2 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t / .14)
    return body * attack(t, .002 if bright else .009)


def bell(note, length=3.5, glass=False):
    t = np.arange(int(RATE * length)) / RATE
    f = hz(note)
    partials = [(1, 1, 1.1), (2, .3, .65), (3, .12, .28), (4, .055, .16)]
    if glass:
        partials = [(1, 1, 1.45), (2.01, .28, .82), (3.98, .09, .35), (5.03, .035, .14)]
    y = np.zeros_like(t)
    for ratio, level, decay in partials:
        y += level * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t / decay)
    return y * attack(t, .009 if glass else .014)


def pluck(note, length=1.7):
    t = np.arange(int(RATE * length)) / RATE
    f = hz(note)
    y = np.zeros_like(t)
    for k in range(1, 9):
        # Smoothly remove upper harmonics as the note decays.
        y += np.sin(2 * np.pi * f * k * t) / k ** 1.5 * np.exp(-t * (2.5 + k * 1.2))
    return y * attack(t, .004)


def warm_pad(notes, length):
    t = np.arange(int(RATE * length)) / RATE
    y = np.zeros_like(t)
    for note in notes:
        f = hz(note)
        for detune in [-.0018, .0018]:
            y += np.sin(2 * np.pi * f * (1 + detune) * t)
            y += .12 * np.sin(2 * np.pi * f * 2 * (1 + detune) * t)
    env = attack(t, .24) * np.exp(-t / (length * .5))
    env *= np.minimum(1, np.maximum(0, (length - t) / .35))
    return y / (2 * len(notes)) * env


def ping(note, length=.42, strong=False):
    t = np.arange(int(RATE * length)) / RATE
    f = hz(note)
    y = np.sin(2 * np.pi * f * t)
    y += (.28 if strong else .16) * np.sin(2 * np.pi * f * 2 * t)
    y += (.13 if strong else .04) * np.sin(2 * np.pi * f * 3 * t)
    env = attack(t, .004) * np.exp(-t / (.22 if strong else .16))
    env *= np.minimum(1, np.maximum(0, (length - t) / .025))
    return y * env


def click(length=.08, soft=True):
    n = int(RATE * length)
    t = np.arange(n) / RATE
    spectrum = np.fft.rfft(RNG.standard_normal(n))
    freqs = np.fft.rfftfreq(n, 1 / RATE)
    spectrum *= np.exp(-((freqs - (2800 if soft else 4300)) / 1600) ** 2)
    y = np.fft.irfft(spectrum, n=n)
    y /= max(np.max(np.abs(y)), 1e-9)
    return y * attack(t, .0008) * np.exp(-t / (.009 if soft else .019))


def knock(note=55, length=.2):
    t = np.arange(int(RATE * length)) / RATE
    phase = 2 * np.pi * (hz(note) * t + 5 * .035 * (1 - np.exp(-t / .035)))
    y = np.sin(phase) + .18 * np.sin(phase * 2)
    return y * attack(t, .001) * np.exp(-t / .047)


class Loop:
    def __init__(self, tempo, beats=16):
        self.beat = 60 / tempo
        self.seconds = self.beat * beats
        self.samples = np.zeros(round(self.seconds * RATE), dtype=np.float64)

    def add(self, beat, sound, level=1):
        start = round((beat * self.beat + .025) * RATE)
        indices = (start + np.arange(len(sound))) % len(self.samples)
        np.add.at(self.samples, indices, sound * level)

    def finish(self, ambience=.1):
        # Short, filtered echo taps add space without hiding the rhythm.
        dry = self.samples.copy()
        smooth = (dry + np.roll(dry, 1) + np.roll(dry, 2)) / 3
        for delay, level in [(.071, .52), (.113, .37), (.173, .26), (.241, .18), (.337, .12)]:
            self.samples += ambience * level * np.roll(smooth, round(delay * RATE))
        self.samples -= self.samples.mean()
        self.samples /= max(np.max(np.abs(self.samples)), 1e-9)
        self.samples = np.tanh(self.samples * 1.45) / np.tanh(1.45) * .7
        # An 8 ms join prevents a click in either a standalone preview or a loop.
        fade = round(.008 * RATE)
        ramp = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
        self.samples[:fade] *= ramp
        self.samples[-fade:] *= ramp[::-1]
        return self.samples


def daybreak():
    loop = Loop(80)
    for b, n, v in [(0, 76, .8), (1.5, 79, .57), (3, 83, .55), (4, 81, .72),
                     (5.5, 79, .53), (7, 76, .5), (8, 74, .76), (9.5, 78, .57),
                     (11, 81, .54), (12, 79, .74), (13.5, 76, .56), (15, 74, .42)]:
        loop.add(b, mallet(n), v)
    for b, chord in [(0, [64, 67, 71]), (4, [65, 69, 72]), (8, [62, 66, 69]), (12, [64, 67, 71])]:
        loop.add(b, warm_pad(chord, loop.beat * 4.2), .18)
    return loop.finish(.18)


def glass_garden():
    loop = Loop(88)
    melody = [79, 83, 86, 83, 81, 84, 88, 84, 78, 81, 86, 81, 79, 83, 88, 86]
    for b, n in enumerate(melody):
        loop.add(b, bell(n, glass=True), .6 if b % 4 == 0 else .35)
    for b, n in [(0, 67), (4, 69), (8, 66), (12, 67)]:
        loop.add(b, bell(n), .3)
    return loop.finish(.21)


def stepping_stones():
    loop = Loop(100)
    motifs = [[72, 76, 79, 76, 74, 76], [69, 72, 76, 72, 74, 76],
              [71, 74, 79, 74, 76, 79], [72, 76, 81, 79, 76, 74]]
    for bar, notes in enumerate(motifs):
        for off, note, amp in zip([0, .75, 1.5, 2, 2.75, 3.5], notes, [.7, .4, .5, .65, .4, .48]):
            loop.add(bar * 4 + off, mallet(note, bright=True), amp)
        loop.add(bar * 4, knock(60 + bar % 2 * 2), .3)
        loop.add(bar * 4 + 2, knock(60), .2)
    for b in np.arange(.5, 16, 1):
        loop.add(b, click(), .13)
    return loop.finish(.13)


def orbit():
    loop = Loop(108)
    chords = [[72, 76, 79, 83], [69, 72, 76, 79], [65, 69, 72, 76], [67, 71, 74, 79]]
    pattern = [0, 1, 2, 1, 0, 2, 3, 2]
    for bar, chord in enumerate(chords):
        for step, degree in enumerate(pattern):
            loop.add(bar * 4 + step * .5, pluck(chord[degree]), .52 if step % 4 == 0 else .34)
        loop.add(bar * 4, warm_pad([n - 12 for n in chord[:3]], loop.beat * 4), .16)
        for off in [0, 2]:
            loop.add(bar * 4 + off, knock(57), .25)
    for b in np.arange(.5, 16, 1):
        loop.add(b, click(), .1)
    return loop.finish(.19)


def clear_signal():
    loop = Loop(100)
    for bar, pair in enumerate([(76, 81), (76, 83), (78, 83), (76, 81)]):
        for off, note in [(0, pair[0]), (.5, pair[1]), (2, pair[0]), (2.5, pair[1])]:
            loop.add(bar * 4 + off, ping(note, .48, strong=True), .75)
        loop.add(bar * 4, ping(pair[0] - 12, .55), .23)
    return loop.finish(.055)


def rally():
    loop = Loop(116)
    for bar, chord in enumerate([[74, 78, 81], [71, 74, 78], [67, 71, 74], [69, 73, 76]]):
        for off, strength in [(0, .85), (.75, .56), (1.5, .65), (2, .85), (2.75, .56), (3.5, .65)]:
            loop.add(bar * 4 + off, ping(chord[2], .28, strong=True), strength)
            loop.add(bar * 4 + off, pluck(chord[0], .65), strength * .48)
        for off in [0, 1, 2, 3]:
            loop.add(bar * 4 + off, knock(62), .32)
        for off in [1, 3]:
            loop.add(bar * 4 + off, click(.14, soft=False), .2)
    return loop.finish(.09)


CANDIDATES = [
    ('01_daybreak', 'Daybreak', 'Soft, rounded mallets with a warm background.', daybreak),
    ('02_glass_garden', 'Glass Garden', 'Clear, shimmering bells with a simple melodic pattern.', glass_garden),
    ('03_stepping_stones', 'Stepping Stones', 'Wooden mallets and a light, purposeful rhythm.', stepping_stones),
    ('04_orbit', 'Orbit', 'A bright, flowing synth arpeggio with a gentle pulse.', orbit),
    ('05_clear_signal', 'Clear Signal', 'Distinct pairs of clean, bright tones with breathing space.', clear_signal),
    ('06_rally', 'Rally', 'A more insistent rhythmic electronic alarm.', rally),
]


def write_wav(path, samples):
    with wave.open(str(path), 'wb') as stream:
        stream.setnchannels(1)
        stream.setsampwidth(2)
        stream.setframerate(RATE)
        stream.writeframes(np.clip(samples * 32767, -32768, 32767).astype('<i2').tobytes())


def loudness(path, target=-18):
    result = subprocess.run(['ffmpeg', '-hide_banner', '-nostats', '-i', str(path),
                             '-af', f'loudnorm=I={target}:TP=-1.5:LRA=8:print_format=json',
                             '-f', 'null', '-'], check=True, capture_output=True, text=True)
    start = result.stderr.rfind('{')
    return json.loads(result.stderr[start:result.stderr.rfind('}') + 1])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    root = Path(__file__).resolve().parents[1]
    parser.add_argument('--output', type=Path)
    parser.add_argument('--production', action='store_true', help='Master the approved compositions for phone speakers.')
    parser.add_argument('--install', action='store_true', help='Convert production WAVs to iOS CAF resources.')
    args = parser.parse_args()
    if args.install and not args.production:
        parser.error('--install requires --production')
    args.output = args.output or root / ('artwork/sounds/library' if args.production else 'artwork/sounds/candidates/2026-10-01')
    args.output.mkdir(parents=True, exist_ok=True)
    target = -16.5 if args.production else -18
    report = []
    with tempfile.TemporaryDirectory(prefix='mathalarm-sounds-') as temporary:
        for stem, name, description, compose in CANDIDATES:
            if args.production:
                stem = 'alarm_' + stem.split('_', 1)[1]
            raw = Path(temporary) / f'{stem}.wav'
            samples = compose()
            if args.production:
                # Circular filtering preserves the loop while reducing bass that
                # phone speakers cannot reproduce and softening the top octave.
                freqs = np.fft.rfftfreq(len(samples), 1 / RATE)
                response = freqs ** 2 / (freqs ** 2 + 110 ** 2)
                response *= 1 / np.sqrt(1 + (freqs / 8500) ** 8)
                samples = np.fft.irfft(np.fft.rfft(samples) * response, n=len(samples))
                fade = round(.004 * RATE)
                ramp = np.sin(np.linspace(0, np.pi / 2, fade)) ** 2
                samples[:fade] *= ramp
                samples[-fade:] *= ramp[::-1]
            write_wav(raw, samples)
            measured = loudness(raw, target)
            settings = (f'loudnorm=I={target}:TP=-1.5:LRA=8:linear=true:'
                        f'measured_I={measured["input_i"]}:measured_TP={measured["input_tp"]}:'
                        f'measured_LRA={measured["input_lra"]}:measured_thresh={measured["input_thresh"]}:'
                        f'offset={measured["target_offset"]}')
            output = args.output / f'{stem}.wav'
            subprocess.run(['ffmpeg', '-y', '-hide_banner', '-loglevel', 'error', '-i', str(raw),
                            '-af', settings, '-ar', str(RATE), '-ac', '1', '-c:a', 'pcm_s16le',
                            str(output)], check=True)
            verified = loudness(output, target)
            with wave.open(str(output), 'rb') as stream:
                actual = np.frombuffer(stream.readframes(stream.getnframes()), dtype='<i2')
                duration = stream.getnframes() / stream.getframerate()
            item = dict(id=stem, name=name, description=description, filename=output.name,
                        duration_seconds=round(duration, 3), integrated_lufs=float(verified['input_i']),
                        true_peak_dbtp=float(verified['input_tp']),
                        loudness_range_lu=float(verified['input_lra']),
                        peak_sample=int(np.max(np.abs(actual.astype(np.int32)))),
                        clipped_samples=int(np.sum(np.abs(actual.astype(np.int32)) >= 32767)),
                        loop_join_difference=int(actual[-1]) - int(actual[0]))
            assert duration < 30, item
            assert item['clipped_samples'] == 0, item
            assert abs(item['integrated_lufs'] - target) <= .7, item
            assert item['true_peak_dbtp'] <= -1.3, item
            assert abs(item['loop_join_difference']) <= 2, item
            report.append(item)
            if args.install:
                caf = root / 'iosApp/iosApp/Sounds' / f'{stem}.caf'
                subprocess.run(['afconvert', str(output), str(caf), '-f', 'caff', '-d', 'LEI16', '-c', '1'], check=True)
            print(json.dumps(item), flush=True)
    (args.output / ('library.json' if args.production else 'candidates.json')).write_text(json.dumps(dict(
        purpose='Original Math Alarm sound library for iOS.' if args.production else 'Original synthesized sound candidates for audition; not installed in the app.',
        sample_rate=RATE, channels=1, format='16-bit PCM WAV',
        target_lufs=target, candidates=report), indent=2) + '\n')


if __name__ == '__main__':
    main()
