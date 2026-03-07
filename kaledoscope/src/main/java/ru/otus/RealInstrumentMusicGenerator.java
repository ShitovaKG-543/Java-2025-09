package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class RealInstrumentMusicGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Гаммы для разных инструментов
    private int[] majorScale = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24};
    private int[] minorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24};
    private int[] bluesScale = {0, 3, 5, 6, 7, 10, 12, 15, 17, 18, 19, 22, 24};

    // Инструменты
    private enum InstrumentType {
        PIANO,
        VIOLIN,
        CELLO,
        DOUBLE_BASS,
        GUITAR,
        SAXOPHONE,
        DRUMS
    }

    public static void main(String[] args) {
        RealInstrumentMusicGenerator generator = new RealInstrumentMusicGenerator();

        String filename = "instrumental_music_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ ИНСТРУМЕНТАЛЬНОЙ МУЗЫКИ ===");
        System.out.println("Длительность: 3 минуты");
        System.out.println("Формат: WAV");
        System.out.println("Файл: " + filename);

        try {
            generator.generateMusic(filename);
            generator.convertToMp3(filename);
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateMusic(String filename) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);

        File file = new File(filename);

        AudioInputStream audioStream = new AudioInputStream(
                new InstrumentalMusicInputStream(DURATION_SECONDS * SAMPLE_RATE),
                format,
                DURATION_SECONDS * SAMPLE_RATE);

        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
        System.out.println("✓ Музыка сгенерирована: " + filename);
    }

    private void convertToMp3(String wavFile) {
        String mp3File = wavFile.replace(".wav", ".mp3");

        try {
            ProcessBuilder pb =
                    new ProcessBuilder("ffmpeg", "-i", wavFile, "-codec:a", "libmp3lame", "-qscale:a", "2", mp3File);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✓ Конвертировано в MP3: " + mp3File);
                new File(wavFile).delete();
            }

        } catch (Exception e) {
            System.out.println("  WAV файл сохранен");
        }
    }

    private class InstrumentalMusicInputStream extends java.io.InputStream {
        private final long totalSamples;
        private long samplesGenerated = 0;
        private double time = 0;

        // Музыкальные параметры
        private final int rootNote = 48; // C3
        private final int[] scale = minorScale;
        private final double bpm = 90; // Умеренный темп

        // Оркестр
        private final RealInstrument[] orchestra;

        public InstrumentalMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Создаем оркестр из разных инструментов
            this.orchestra = new RealInstrument[] {
                new Piano("piano", 0.25),
                // new Violin("violin", 0.2),
                // new Cello("cello", 0.2),
                new DoubleBass("double_bass", 0.2),
                new Guitar("guitar", 0.15),
                new Saxophone("saxophone", 0.15),
                // new Drums("drums", 0.15)
            };

            printMusicInfo();
        }

        private void printMusicInfo() {
            System.out.println("  Инструменты:");
            for (RealInstrument instrument : orchestra) {
                System.out.println("    - " + instrument.name);
            }
            System.out.println("  Тональность: Ля минор");
            System.out.println("  Темп: " + (int) bpm + " BPM");
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int result = read(b, 0, 1);
            return result == -1 ? -1 : b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (samplesGenerated >= totalSamples) {
                return -1;
            }

            int bytesPerSample = (BITS_PER_SAMPLE / 8) * CHANNELS;
            int samplesToGenerate = Math.min(len / bytesPerSample, (int) (totalSamples - samplesGenerated));

            for (int i = 0; i < samplesToGenerate; i++) {
                time = (samplesGenerated + i) / (double) SAMPLE_RATE;

                // Собираем звук от всех инструментов
                double sampleValue = 0;
                for (RealInstrument instrument : orchestra) {
                    sampleValue += instrument.generateSample(time);
                }

                // Нормализация
                sampleValue = Math.tanh(sampleValue * 0.6);

                // Стерео панорама
                double pan = Math.sin(time * 0.2) * 0.3;

                short sampleLeft = (short) (sampleValue * (0.7 - pan) * 32767);
                short sampleRight = (short) (sampleValue * (0.7 + pan) * 32767);

                b[off + i * bytesPerSample] = (byte) (sampleLeft & 0xFF);
                b[off + i * bytesPerSample + 1] = (byte) ((sampleLeft >> 8) & 0xFF);
                b[off + i * bytesPerSample + 2] = (byte) (sampleRight & 0xFF);
                b[off + i * bytesPerSample + 3] = (byte) ((sampleRight >> 8) & 0xFF);
            }

            samplesGenerated += samplesToGenerate;
            return samplesToGenerate * bytesPerSample;
        }

        // Базовый класс для инструментов
        private abstract class RealInstrument {
            final String name;
            final double baseVolume;

            RealInstrument(String name, double baseVolume) {
                this.name = name;
                this.baseVolume = baseVolume;
            }

            abstract double generateSample(double time);

            protected double applyEnvelope(double time, double noteStart, double noteDuration) {
                double pos = time - noteStart;
                if (pos < 0 || pos > noteDuration) return 0;

                // ADSR envelope
                double attack = 0.05;
                double decay = 0.1;
                double sustain = 0.7;
                double release = 0.1;

                if (pos < attack) {
                    return pos / attack;
                } else if (pos < attack + decay) {
                    return 1.0 - (1.0 - sustain) * (pos - attack) / decay;
                } else if (pos < noteDuration - release) {
                    return sustain;
                } else {
                    return sustain * (1.0 - (pos - (noteDuration - release)) / release);
                }
            }
        }

        // Пианино
        private class Piano extends RealInstrument {
            Piano(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Аккомпанемент - арпеджио
                for (int i = 0; i < 4; i++) {
                    double noteTime = beatPosition + i * 0.5;
                    int noteIndex = ((int) (noteTime)) % 8;

                    double[] arpeggio = {0, 4, 7, 12, 7, 4, 0, 4};
                    double freq = 440 * Math.pow(2, (rootNote - 57 + arpeggio[noteIndex]) / 12.0);

                    double envelope = applyEnvelope(time, noteTime / (bpm / 60), 0.4);

                    // Пианино - быстрый атака, богатые гармоники
                    for (int h = 1; h <= 5; h++) {
                        value += Math.sin(2 * Math.PI * freq * h * time) * envelope * (0.5 / h);
                    }
                }

                return value * baseVolume * 0.3;
            }
        }

        // Скрипка
        private class Violin extends RealInstrument {
            Violin(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Мелодия
                int noteIndex = ((int) (beatPosition * 2)) % 16;
                double[] melody = {0, 2, 4, 7, 9, 7, 4, 2, 0, 2, 4, 7, 9, 7, 4, 2};

                double freq = 440 * Math.pow(2, (rootNote + 12 - 57 + melody[noteIndex]) / 12.0);

                double envelope = applyEnvelope(time, Math.floor(beatPosition * 2) / 2 / (bpm / 60), 0.8);

                // Скрипка - богатый звук с вибрато
                double vibrato = 1.0 + 0.01 * Math.sin(2 * Math.PI * 5 * time);
                freq *= vibrato;

                // Основной тон и гармоники
                for (int h = 1; h <= 6; h++) {
                    double amp = (0.7 / h) * (1.0 - h * 0.1);
                    if (h == 1) amp = 1.0;
                    value += Math.sin(2 * Math.PI * freq * h * time) * envelope * amp;
                }

                return value * baseVolume * 0.4;
            }
        }

        // Виолончель
        private class Cello extends RealInstrument {
            Cello(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Контрапункт
                int noteIndex = ((int) (beatPosition * 1.5)) % 12;
                double[] counterMelody = {0, 7, 4, 9, 5, 7, 2, 9, 0, 7, 4, 9};

                double freq = 440 * Math.pow(2, (rootNote - 12 - 57 + counterMelody[noteIndex]) / 12.0);

                double envelope = applyEnvelope(time, Math.floor(beatPosition * 1.5) / 1.5 / (bpm / 60), 1.2);

                // Виолончель - теплый, глубокий звук
                for (int h = 1; h <= 4; h++) {
                    double amp = (0.8 / h) * (1.0 - h * 0.15);
                    if (h == 1) amp = 1.0;
                    value += Math.sin(2 * Math.PI * freq * h * time) * envelope * amp;
                }

                return value * baseVolume * 0.35;
            }
        }

        // Контрабас
        private class DoubleBass extends RealInstrument {
            DoubleBass(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Басовая линия
                if (beatPosition % 1.0 < 0.3) {
                    int noteIndex = ((int) beatPosition) % 8;
                    double[] bassLine = {0, 2, 4, 5, 4, 2, 0, 2};

                    double freq = 440 * Math.pow(2, (rootNote - 24 - 57 + bassLine[noteIndex]) / 12.0);

                    double envelope = applyEnvelope(time, Math.floor(beatPosition), 0.4);

                    // Контрабас - низкие частоты, меньше гармоник
                    for (int h = 1; h <= 3; h++) {
                        value += Math.sin(2 * Math.PI * freq * h * time) * envelope * (0.6 / h);
                    }
                }

                return value * baseVolume * 0.45;
            }
        }

        // Гитара
        private class Guitar extends RealInstrument {
            Guitar(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Аккорды
                if (beatPosition % 2.0 < 0.5) {
                    int chordIndex = ((int) (beatPosition / 2)) % 4;
                    double[][] chords = {
                        {0, 4, 7}, // Am
                        {2, 5, 9}, // Em
                        {4, 7, 11}, // F
                        {7, 11, 14} // G
                    };

                    double envelope = applyEnvelope(time, Math.floor(beatPosition / 2) * 2 / (bpm / 60), 0.8);

                    for (double note : chords[chordIndex]) {
                        double freq = 440 * Math.pow(2, (rootNote + 12 - 57 + note) / 12.0);

                        // Гитара - характерный звук с затуханием
                        for (int h = 1; h <= 4; h++) {
                            double amp = (0.5 / h) * (1.0 - h * 0.1);
                            value += Math.sin(2 * Math.PI * freq * h * time) * envelope * amp;
                        }
                    }
                }

                return value * baseVolume * 0.25;
            }
        }

        // Саксофон
        private class Saxophone extends RealInstrument {
            Saxophone(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Импровизация
                if (Math.sin(beatPosition * 0.5) > 0.3) {
                    int noteIndex = ((int) (beatPosition * 1.3)) % 12;
                    double[] saxRiff = {0, 2, 4, 7, 9, 7, 4, 2, 0, 2, 4, 7};

                    double freq = 440 * Math.pow(2, (rootNote + 24 - 57 + saxRiff[noteIndex]) / 12.0);

                    double envelope = applyEnvelope(time, Math.floor(beatPosition * 1.3) / 1.3 / (bpm / 60), 0.6);

                    // Саксофон - яркий звук с множеством гармоник
                    for (int h = 1; h <= 7; h++) {
                        double amp = (0.9 / h) * (1.0 - h * 0.08);
                        if (h % 2 == 0) amp *= 0.7;
                        value += Math.sin(2 * Math.PI * freq * h * time) * envelope * amp;
                    }

                    // Добавляем легкое вибрато
                    value *= 0.9 + 0.1 * Math.sin(2 * Math.PI * 6 * time);
                }

                return value * baseVolume * 0.3;
            }
        }

        // Барабаны
        private class Drums extends RealInstrument {
            Drums(String name, double baseVolume) {
                super(name, baseVolume);
            }

            @Override
            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Бас-барабан (каждую четверть)
                if (beatPosition % 1.0 < 0.1) {
                    double bassFreq = 60;
                    double bassAmp = 0.5 * Math.exp(-(beatPosition % 1.0) * 20);
                    value += Math.sin(2 * Math.PI * bassFreq * time) * bassAmp;
                }

                // Малый барабан (на 2 и 4)
                double beatInMeasure = beatPosition % 4;
                if ((beatInMeasure > 1.9 && beatInMeasure < 2.1) || (beatInMeasure > 3.9 && beatInMeasure < 4.1)) {
                    double snareAmp = 0.4 * Math.exp(-(beatPosition % 0.5) * 15);
                    // Шум для малого барабана
                    for (int j = 0; j < 10; j++) {
                        value += (random.nextDouble() * 2 - 1) * snareAmp * 0.1;
                    }
                }

                // Хай-хэт (каждую восьмую)
                if (beatPosition % 0.5 < 0.05) {
                    double hatAmp = 0.15;
                    for (int j = 0; j < 5; j++) {
                        value += (random.nextDouble() * 2 - 1) * hatAmp * 0.2;
                    }
                }

                return value * baseVolume * 0.5;
            }
        }
    }
}
