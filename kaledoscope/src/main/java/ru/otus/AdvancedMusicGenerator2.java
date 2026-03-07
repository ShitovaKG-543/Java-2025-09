package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class AdvancedMusicGenerator2 {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Музыкальные параметры для разнообразия
    private int[] majorScale = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24};
    private int[] minorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24};
    private int[] pentatonicMajor = {0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24};
    private int[] pentatonicMinor = {0, 3, 5, 7, 10, 12, 15, 17, 19, 22, 24};
    private int[] bluesScale = {0, 3, 5, 6, 7, 10, 12, 15, 17, 18, 19, 22, 24};
    private int[] arabicScale = {0, 1, 4, 5, 7, 8, 11, 12, 13, 16, 17, 19, 20, 23, 24};

    // Жанры
    private enum Genre {
        CLASSICAL,
        ROCK,
        JAZZ,
        ELECTRONIC,
        CINEMATIC,
        AMBIENT,
        WORLD,
        DISCO,
        REGGAE,
        FOLK
    }

    public static void main(String[] args) {
        AdvancedMusicGenerator2 generator = new AdvancedMusicGenerator2();

        String filename =
                "advanced_music_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ БОГАТОЙ МУЗЫКИ ===");
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
                new AdvancedMusicInputStream(DURATION_SECONDS * SAMPLE_RATE), format, DURATION_SECONDS * SAMPLE_RATE);

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

    private class AdvancedMusicInputStream extends java.io.InputStream {
        private final long totalSamples;
        private long samplesGenerated = 0;
        private double time = 0;

        // Музыкальные параметры (уникальные для каждой генерации)
        private final Genre genre;
        private final double bpm;
        private final int rootNote;
        private final int[] scale;
        private final boolean isMajor;

        // Инструменты
        private final Instrument[] instruments;

        public AdvancedMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный выбор жанра
            this.genre = Genre.values()[random.nextInt(Genre.values().length)];

            // Выбор тональности (C, D, E, F, G, A, B)
            this.rootNote = 60 + random.nextInt(12); // Расширил диапазон

            // Выбор гаммы в зависимости от жанра
            int[][] scales = {majorScale, minorScale, pentatonicMajor, pentatonicMinor, bluesScale, arabicScale};
            this.scale = scales[random.nextInt(scales.length)];
            this.isMajor = (scale == majorScale || scale == pentatonicMajor);

            // Темп в зависимости от жанра
            this.bpm = selectBpmForGenre();

            // Создаем инструменты (больше инструментов)
            this.instruments = createInstrumentsForGenre();

            printMusicInfo();
        }

        private double selectBpmForGenre() {
            switch (genre) {
                case CLASSICAL:
                    return 70 + random.nextInt(90); // 70-160
                case ROCK:
                    return 110 + random.nextInt(100); // 110-210
                case JAZZ:
                    return 90 + random.nextInt(100); // 90-190
                case ELECTRONIC:
                    return 120 + random.nextInt(80); // 120-200
                case CINEMATIC:
                    return 60 + random.nextInt(80); // 60-140
                case AMBIENT:
                    return 50 + random.nextInt(60); // 50-110
                case WORLD:
                    return 80 + random.nextInt(100); // 80-180
                case DISCO:
                    return 110 + random.nextInt(40); // 110-150
                case REGGAE:
                    return 70 + random.nextInt(40); // 70-110
                case FOLK:
                    return 90 + random.nextInt(70); // 90-160
                default:
                    return 100 + random.nextInt(60);
            }
        }

        private Instrument[] createInstrumentsForGenre() {
            int numInstruments = 5 + random.nextInt(5); // 5-10 инструментов

            switch (genre) {
                case CLASSICAL:
                    return new Instrument[] {
                        new Instrument("piano", 0.25, "sine", 1.0, true),
                        new Instrument("strings_high", 0.2, "sawtooth", 1.5, true),
                        new Instrument("strings_low", 0.2, "sawtooth", 0.7, true),
                        new Instrument("flute", 0.15, "sine", 1.2, true),
                        new Instrument("bass", 0.2, "sine", 0.5, true),
                        new Instrument("harp", 0.15, "sawtooth", 1.3, true),
                        new Instrument("timpani", 0.15, "sine", 0.3, false),
                        new Instrument("trumpet", 0.15, "square", 1.1, true)
                    };

                case ROCK:
                    return new Instrument[] {
                        new Instrument("guitar_rhythm", 0.3, "square", 1.0, true),
                        new Instrument("guitar_lead", 0.25, "sawtooth", 1.2, true),
                        new Instrument("bass", 0.3, "sawtooth", 0.5, true),
                        new Instrument("drums_kit", 0.3, "noise", 1.0, false),
                        new Instrument("synth", 0.2, "sawtooth", 1.0, true),
                        new Instrument("organ", 0.15, "sine", 0.8, true)
                    };

                case JAZZ:
                    return new Instrument[] {
                        new Instrument("piano", 0.25, "sine", 1.0, true),
                        new Instrument("double_bass", 0.25, "sine", 0.4, true),
                        new Instrument("saxophone", 0.25, "sawtooth", 1.0, true),
                        new Instrument("drums_jazz", 0.2, "noise", 1.0, false),
                        new Instrument("trumpet", 0.2, "square", 1.1, true),
                        new Instrument("clarinet", 0.15, "sine", 0.9, true)
                    };

                case ELECTRONIC:
                    return new Instrument[] {
                        new Instrument("synth_bass", 0.3, "sawtooth", 0.5, true),
                        new Instrument("synth_lead", 0.25, "square", 1.5, true),
                        new Instrument("synth_pad", 0.25, "sine", 1.0, true),
                        new Instrument("drums_electronic", 0.3, "noise", 1.0, false),
                        new Instrument("synth_arp", 0.2, "sawtooth", 1.2, true),
                        new Instrument("synth_fx", 0.15, "noise", 0.8, false)
                    };

                case CINEMATIC:
                    return new Instrument[] {
                        new Instrument("strings", 0.3, "sawtooth", 1.0, true),
                        new Instrument("brass", 0.25, "square", 1.0, true),
                        new Instrument("piano", 0.2, "sine", 1.0, true),
                        new Instrument("timpani", 0.2, "sine", 0.3, false),
                        new Instrument("choir", 0.2, "sawtooth", 1.1, true),
                        new Instrument("harp", 0.15, "sawtooth", 1.2, true),
                        new Instrument("bass_drum", 0.15, "sine", 0.2, false)
                    };

                case WORLD:
                    return new Instrument[] {
                        new Instrument("sitar", 0.25, "sawtooth", 1.0, true),
                        new Instrument("tabla", 0.25, "noise", 1.0, false),
                        new Instrument("flute", 0.2, "sine", 1.2, true),
                        new Instrument("drums_world", 0.2, "noise", 0.8, false),
                        new Instrument("strings_world", 0.2, "sawtooth", 0.7, true),
                        new Instrument("bass_world", 0.15, "sine", 0.5, true)
                    };

                default:
                    return new Instrument[] {
                        new Instrument("piano", 0.3, "sine", 1.0, true),
                        new Instrument("bass", 0.25, "sawtooth", 0.5, true),
                        new Instrument("drums", 0.25, "noise", 1.0, false),
                        new Instrument("synth", 0.2, "square", 1.0, true)
                    };
            }
        }

        private void printMusicInfo() {
            System.out.println("  Жанр: " + genre);
            System.out.println("  Тональность: " + getKeyName());
            System.out.println("  Темп: " + (int) bpm + " BPM");
            System.out.println("  Инструментов: " + instruments.length);
        }

        private String getKeyName() {
            String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            String note = notes[(rootNote - 60) % 12];
            return note + (isMajor ? " мажор" : " минор");
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

                // Генерируем сэмпл от всех инструментов
                double sampleValue = 0;
                for (Instrument instrument : instruments) {
                    sampleValue += instrument.generateSample(time);
                }

                // Нормализация
                sampleValue = Math.tanh(sampleValue * 0.7); // Мягкое ограничение

                // Стерео эффект
                double pan = Math.sin(time * 0.5) * 0.3;

                short sampleLeft = (short) (sampleValue * (0.7 - pan) * 32767);
                short sampleRight = (short) (sampleValue * (0.7 + pan) * 32767);

                // Записываем левый канал
                b[off + i * bytesPerSample] = (byte) (sampleLeft & 0xFF);
                b[off + i * bytesPerSample + 1] = (byte) ((sampleLeft >> 8) & 0xFF);

                // Записываем правый канал
                b[off + i * bytesPerSample + 2] = (byte) (sampleRight & 0xFF);
                b[off + i * bytesPerSample + 3] = (byte) ((sampleRight >> 8) & 0xFF);
            }

            samplesGenerated += samplesToGenerate;
            return samplesToGenerate * bytesPerSample;
        }

        // Класс для представления инструмента
        private class Instrument {
            final String name;
            final double baseVolume;
            final String waveType;
            final double octaveMultiplier;
            final boolean isMelodic;

            // Уникальные паттерны для каждого инструмента
            double[] melodyPattern;
            double[] rhythmPattern;
            double[] harmonyPattern;
            double[] phrasePattern;
            double[] accentPattern;

            Instrument(String name, double baseVolume, String waveType, double octaveMultiplier, boolean isMelodic) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.waveType = waveType;
                this.octaveMultiplier = octaveMultiplier;
                this.isMelodic = isMelodic;

                generatePatterns();
            }

            private void generatePatterns() {
                melodyPattern = new double[32];
                rhythmPattern = new double[16];
                harmonyPattern = new double[8];
                phrasePattern = new double[64];
                accentPattern = new double[8];

                for (int i = 0; i < melodyPattern.length; i++) {
                    melodyPattern[i] = random.nextDouble();
                }
                for (int i = 0; i < rhythmPattern.length; i++) {
                    rhythmPattern[i] = 0.2 + random.nextDouble() * 0.8;
                }
                for (int i = 0; i < harmonyPattern.length; i++) {
                    harmonyPattern[i] = random.nextInt(scale.length);
                }
                for (int i = 0; i < phrasePattern.length; i++) {
                    phrasePattern[i] = 0.3 + random.nextDouble() * 0.7;
                }
                for (int i = 0; i < accentPattern.length; i++) {
                    accentPattern[i] = random.nextDouble() > 0.7 ? 1.5 : 1.0;
                }
            }

            double generateSample(double time) {
                double beatPosition = time * bpm / 60.0;

                // Базовая громкость всегда присутствует (убрал затухание в начале/конце)
                double volumeMultiplier = 1.0;

                // Добавляем небольшие вариации громкости для интереса
                volumeMultiplier *= 0.8 + 0.2 * Math.sin(beatPosition * 0.5);

                double value = 0;

                if (isMelodic) {
                    // Мелодия
                    value += generateMelody(beatPosition) * 0.4;

                    // Гармония
                    value += generateHarmony(beatPosition) * 0.3;

                    // Контрапункт
                    value += generateCounterpoint(beatPosition) * 0.2;
                } else {
                    // Ударные
                    value += generateDrums(beatPosition) * 0.5;

                    // Перкуссия
                    value += generatePercussion(beatPosition) * 0.2;
                }

                // Эффекты
                if (name.contains("fx") || name.contains("synth")) {
                    value += generateEffects(beatPosition) * 0.15;
                }

                return value * baseVolume * volumeMultiplier;
            }

            private double generateMelody(double beatPosition) {
                double value = 0;

                int phraseLength = 16;
                int phraseIndex = ((int) (beatPosition / 2)) % phrasePattern.length;

                // Основная мелодия играет постоянно
                for (int voice = 0; voice < 2; voice++) {
                    int patternIndex = ((int) (beatPosition * (2 + voice))) % melodyPattern.length;
                    int scaleIndex = (int) (melodyPattern[patternIndex] * scale.length) % scale.length;

                    double freq = 440
                            * Math.pow(2, (rootNote - 57 + scale[scaleIndex] + voice * 12) / 12.0)
                            * octaveMultiplier;

                    // Ритмический паттерн
                    int rhythmIndex = patternIndex % rhythmPattern.length;
                    double noteDuration = rhythmPattern[rhythmIndex] * 0.5;

                    double noteStart = Math.floor(beatPosition * 2) / 2.0;
                    double noteEnd = noteStart + noteDuration;

                    if (beatPosition >= noteStart && beatPosition < noteEnd) {
                        double envelope = 1.0;
                        double posInNote = beatPosition - noteStart;

                        if (posInNote < 0.02) {
                            envelope = posInNote / 0.02; // Attack
                        } else if (noteEnd - beatPosition < 0.05) {
                            envelope = (noteEnd - beatPosition) / 0.05; // Release
                        }

                        // Акценты
                        int accentIndex = ((int) beatPosition) % accentPattern.length;
                        envelope *= accentPattern[accentIndex];

                        value += generateWave(freq, time, envelope, waveType);
                    }
                }

                return value * phrasePattern[phraseIndex % phrasePattern.length];
            }

            private double generateHarmony(double beatPosition) {
                double value = 0;

                int chordIndex = ((int) (beatPosition / 2)) % harmonyPattern.length;
                int baseNote = (int) harmonyPattern[chordIndex];

                // Создаем аккорд из 4 нот
                for (int i = 0; i < 4; i++) {
                    int noteOffset = scale[(baseNote + i * 2) % scale.length];
                    double freq = 440 * Math.pow(2, (rootNote - 57 + noteOffset) / 12.0) * octaveMultiplier * 0.5;

                    double amp = 0.25 * (1.0 - i * 0.15);

                    // Аккорд длится целый такт
                    if (beatPosition % 4 < 3.5) {
                        value += generateWave(freq, time, amp, waveType);
                    }
                }

                return value;
            }

            private double generateCounterpoint(double beatPosition) {
                double value = 0;

                if (random.nextDouble() > 0.7) { // 30% времени
                    int noteIndex = ((int) (beatPosition * 3)) % scale.length;
                    double freq = 440 * Math.pow(2, (rootNote - 45 + scale[noteIndex]) / 12.0) * octaveMultiplier;

                    value = generateWave(freq, time, 0.2, "sine");
                }

                return value;
            }

            private double generateDrums(double beatPosition) {
                double value = 0;

                // Бас-барабан (каждую четверть)
                if (beatPosition % 1.0 < 0.1) {
                    double bassFreq = 50 + random.nextDouble() * 20;
                    double bassAmp = 0.6 * Math.exp(-(beatPosition % 1.0) * 20);
                    value += generateWave(bassFreq, time, bassAmp, "sine");
                }

                // Малый барабан (на 2 и 4)
                double beatInMeasure = beatPosition % 4;
                if ((beatInMeasure > 1.9 && beatInMeasure < 2.1) || (beatInMeasure > 3.9 && beatInMeasure < 4.1)) {
                    double snareFreq = 200 + random.nextDouble() * 100;
                    double snareAmp = 0.4 * Math.exp(-(beatPosition % 0.5) * 15);
                    value += generateWave(snareFreq, time, snareAmp, "noise");
                }

                // Хай-хэт (каждую восьмую)
                if (beatPosition % 0.5 < 0.05) {
                    double hatAmp = 0.2;
                    value += generateWave(8000, time, hatAmp, "noise");
                }

                // Добавляем дополнительные ударные для разнообразия
                if (random.nextDouble() > 0.9 && beatPosition % 2 < 0.1) {
                    value += generateWave(100 + random.nextDouble() * 200, time, 0.15, "noise");
                }

                return value;
            }

            private double generatePercussion(double beatPosition) {
                double value = 0;

                // Добавляем экзотические ударные
                if (random.nextDouble() > 0.95) {
                    double percFreq = 200 + random.nextDouble() * 800;
                    double percAmp = 0.1;
                    value += generateWave(percFreq, time, percAmp, "noise") * Math.exp(-random.nextDouble() * 10);
                }

                return value;
            }

            private double generateEffects(double beatPosition) {
                double value = 0;

                // Свип-эффекты
                if (random.nextDouble() > 0.98) {
                    double sweepFreq = 200 + 800 * Math.sin(beatPosition * 0.5);
                    value += generateWave(sweepFreq, time, 0.1, "sawtooth");
                }

                return value;
            }

            private double generateWave(double frequency, double time, double amplitude, String type) {
                double value = 0;
                double t = time * frequency;

                switch (type) {
                    case "sine":
                        value = Math.sin(2 * Math.PI * t);
                        break;
                    case "square":
                        value = Math.signum(Math.sin(2 * Math.PI * t));
                        break;
                    case "sawtooth":
                        value = 2 * (t - Math.floor(t) - 0.5);
                        for (int h = 2; h <= 8; h++) {
                            value += Math.sin(2 * Math.PI * t * h) / h * (1.0 - (h - 2) / 8.0);
                        }
                        break;
                    case "noise":
                        value = random.nextDouble() * 2 - 1;
                        // Фильтруем шум для разных типов ударных
                        if (frequency < 200) {
                            value *= 0.5 + 0.5 * Math.sin(t * 0.1); // Низкочастотный шум
                        }
                        break;
                }

                // Добавляем небольшую случайную модуляцию для живости
                value *= 0.95 + 0.05 * Math.sin(time * 100);

                return value * amplitude;
            }
        }
    }
}
