package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class AdvancedMusicGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Музыкальные параметры для разнообразия
    private int[] majorScale = {0, 2, 4, 5, 7, 9, 11, 12};
    private int[] minorScale = {0, 2, 3, 5, 7, 8, 10, 12};
    private int[] pentatonicMajor = {0, 2, 4, 7, 9, 12};
    private int[] pentatonicMinor = {0, 3, 5, 7, 10, 12};
    private int[] bluesScale = {0, 3, 5, 6, 7, 10, 12};

    // Жанры
    private enum Genre {
        CLASSICAL,
        ROCK,
        JAZZ,
        ELECTRONIC,
        CINEMATIC,
        AMBIENT
    }

    public static void main(String[] args) {
        AdvancedMusicGenerator generator = new AdvancedMusicGenerator();

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

        // Структура композиции
        private final Composition composition;

        public AdvancedMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный выбор жанра
            this.genre = Genre.values()[random.nextInt(Genre.values().length)];

            // Выбор тональности (C, D, E, F, G, A, B)
            this.rootNote = 60 + random.nextInt(7); // C4 до B4

            // Выбор гаммы в зависимости от жанра
            int[][] scales = {majorScale, minorScale, pentatonicMajor, pentatonicMinor, bluesScale};
            this.scale = scales[random.nextInt(scales.length)];
            this.isMajor = (scale == majorScale || scale == pentatonicMajor);

            // Темп в зависимости от жанра
            this.bpm = selectBpmForGenre();

            // Создаем инструменты
            this.instruments = createInstrumentsForGenre();

            // Создаем структуру композиции
            this.composition = new Composition();

            printMusicInfo();
        }

        private double selectBpmForGenre() {
            switch (genre) {
                case CLASSICAL:
                    return 60 + random.nextInt(60); // 60-120
                case ROCK:
                    return 100 + random.nextInt(80); // 100-180
                case JAZZ:
                    return 80 + random.nextInt(80); // 80-160
                case ELECTRONIC:
                    return 120 + random.nextInt(60); // 120-180
                case CINEMATIC:
                    return 70 + random.nextInt(50); // 70-120
                case AMBIENT:
                    return 40 + random.nextInt(40); // 40-80
                default:
                    return 100 + random.nextInt(40);
            }
        }

        private Instrument[] createInstrumentsForGenre() {
            switch (genre) {
                case CLASSICAL:
                    return new Instrument[] {
                        new Instrument("piano", 0.3, "sine", 1.0),
                        new Instrument("strings", 0.2, "sawtooth", 0.7),
                        new Instrument("flute", 0.15, "sine", 2.0),
                        new Instrument("bass", 0.2, "sine", 0.5),
                        new Instrument("harp", 0.15, "sawtooth", 1.5)
                    };

                case ROCK:
                    return new Instrument[] {
                        new Instrument("distorted_guitar", 0.4, "square", 1.0),
                        new Instrument("bass_guitar", 0.3, "sawtooth", 0.5),
                        new Instrument("drums", 0.2, "noise", 1.0),
                        new Instrument("synth", 0.2, "sawtooth", 1.2)
                    };

                case JAZZ:
                    return new Instrument[] {
                        new Instrument("piano", 0.25, "sine", 1.0),
                        new Instrument("double_bass", 0.2, "sine", 0.4),
                        new Instrument("saxophone", 0.25, "sawtooth", 1.3),
                        new Instrument("drums", 0.15, "noise", 1.0),
                        new Instrument("trumpet", 0.15, "square", 1.2)
                    };

                case ELECTRONIC:
                    return new Instrument[] {
                        new Instrument("synth_bass", 0.3, "sawtooth", 0.5),
                        new Instrument("synth_lead", 0.25, "square", 2.0),
                        new Instrument("synth_pad", 0.2, "sine", 1.0),
                        new Instrument("drums", 0.25, "noise", 1.0)
                    };

                case CINEMATIC:
                    return new Instrument[] {
                        new Instrument("strings", 0.3, "sawtooth", 0.8),
                        new Instrument("brass", 0.25, "square", 1.0),
                        new Instrument("piano", 0.2, "sine", 1.0),
                        new Instrument("timpani", 0.15, "sine", 0.3),
                        new Instrument("choir", 0.2, "sawtooth", 1.2)
                    };

                case AMBIENT:
                    return new Instrument[] {
                        new Instrument("pad", 0.4, "sine", 0.5),
                        new Instrument("texture", 0.3, "noise", 1.0),
                        new Instrument("bass", 0.2, "sine", 0.3),
                        new Instrument("bell", 0.15, "sawtooth", 2.0)
                    };

                default:
                    return new Instrument[] {
                        new Instrument("piano", 0.3, "sine", 1.0), new Instrument("bass", 0.2, "sawtooth", 0.5)
                    };
            }
        }

        private void printMusicInfo() {
            System.out.println("  Жанр: " + genre);
            System.out.println("  Тональность: " + getKeyName());
            System.out.println("  Темп: " + (int) bpm + " BPM");
            System.out.println("  Инструментов: " + instruments.length);
            System.out.println("  Структура: " + composition.getStructure());
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
                    sampleValue += instrument.generateSample(time, composition);
                }

                // Нормализация
                sampleValue = Math.tanh(sampleValue); // Мягкое ограничение

                short sample = (short) (sampleValue * 32767);

                for (int ch = 0; ch < CHANNELS; ch++) {
                    b[off + i * bytesPerSample + ch * 2] = (byte) (sample & 0xFF);
                    b[off + i * bytesPerSample + ch * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }
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

            // Паттерны для этого инструмента
            double[] melodyPattern;
            double[] rhythmPattern;
            double[] harmonyPattern;

            Instrument(String name, double baseVolume, String waveType, double octaveMultiplier) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.waveType = waveType;
                this.octaveMultiplier = octaveMultiplier;

                // Генерируем уникальные паттерны для каждого инструмента
                generatePatterns();
            }

            private void generatePatterns() {
                melodyPattern = new double[16];
                rhythmPattern = new double[8];
                harmonyPattern = new double[4];

                for (int i = 0; i < melodyPattern.length; i++) {
                    melodyPattern[i] = random.nextDouble();
                }
                for (int i = 0; i < rhythmPattern.length; i++) {
                    rhythmPattern[i] = 0.3 + random.nextDouble() * 0.7;
                }
                for (int i = 0; i < harmonyPattern.length; i++) {
                    harmonyPattern[i] = random.nextInt(scale.length);
                }
            }

            double generateSample(double time, Composition comp) {
                double value = 0;

                double beatPosition = time * bpm / 60.0;
                int measure = (int) (beatPosition / 4);

                // Разные инструменты играют в разных частях композиции
                double volumeMultiplier = comp.getInstrumentVolume(name, measure);
                if (volumeMultiplier < 0.01) return 0;

                // Основная мелодия
                if (name.contains("piano")
                        || name.contains("lead")
                        || name.contains("flute")
                        || name.contains("saxophone")) {
                    value += generateMelody(beatPosition) * 0.5;
                }

                // Гармония/аккорды
                if (name.contains("strings")
                        || name.contains("pad")
                        || name.contains("synth") && !name.contains("bass")) {
                    value += generateHarmony(beatPosition) * 0.3;
                }

                // Бас
                if (name.contains("bass") || name.contains("timpani")) {
                    value += generateBass(beatPosition) * 0.4;
                }

                // Ударные
                if (name.contains("drums")) {
                    value += generateDrums(beatPosition) * 0.3;
                }

                return value * baseVolume * volumeMultiplier;
            }

            private double generateMelody(double beatPosition) {
                double value = 0;

                int patternLength = melodyPattern.length;
                int noteIndex = ((int) (beatPosition * 2)) % patternLength;

                // Используем паттерн для выбора ноты
                int scaleIndex = (int) (melodyPattern[noteIndex] * scale.length) % scale.length;
                double freq = 440 * Math.pow(2, (rootNote - 57 + scale[scaleIndex]) / 12.0) * octaveMultiplier;

                // Длительность ноты
                double noteDuration = 0.25 + rhythmPattern[noteIndex % rhythmPattern.length] * 0.5;
                double noteStart = Math.floor(beatPosition * 2) / 2.0;
                double noteEnd = noteStart + noteDuration;

                if (beatPosition >= noteStart && beatPosition < noteEnd) {
                    double envelope = 1.0;
                    if (beatPosition - noteStart < 0.05) {
                        envelope = (beatPosition - noteStart) / 0.05; // Attack
                    } else if (noteEnd - beatPosition < 0.1) {
                        envelope = (noteEnd - beatPosition) / 0.1; // Release
                    }

                    value = generateWave(freq, time, envelope, waveType);
                }

                return value;
            }

            private double generateHarmony(double beatPosition) {
                double value = 0;

                int chordIndex = ((int) (beatPosition / 2)) % harmonyPattern.length;
                int baseNote = (int) harmonyPattern[chordIndex];

                // Создаем аккорд из 3-4 нот
                for (int i = 0; i < 3; i++) {
                    int noteOffset = scale[(baseNote + i * 2) % scale.length];
                    double freq = 440 * Math.pow(2, (rootNote - 57 + noteOffset) / 12.0) * octaveMultiplier * 0.5;

                    double amp = 0.3 * (1.0 - i * 0.2);
                    value += generateWave(freq, time, amp, waveType);
                }

                return value;
            }

            private double generateBass(double beatPosition) {
                double value = 0;

                int noteIndex = ((int) beatPosition) % scale.length;
                double freq = 440 * Math.pow(2, (rootNote - 69 + scale[noteIndex]) / 12.0) * octaveMultiplier;

                // Бас играет на сильные доли
                if (beatPosition % 1.0 < 0.1) {
                    value = generateWave(freq, time, 0.8, waveType);
                }

                return value;
            }

            private double generateDrums(double beatPosition) {
                double value = 0;

                // Бас-барабан на каждую четверть
                if (beatPosition % 1.0 < 0.05) {
                    value += generateWave(60, time, 0.5, "sine") * Math.exp(-(beatPosition % 1.0) * 40);
                }

                // Малый барабан на 2 и 4
                if (Math.abs(beatPosition % 2 - 1.0) < 0.05) {
                    value += generateWave(200, time, 0.3, "noise") * Math.exp(-(beatPosition % 0.5) * 20);
                }

                // Хай-хэт на каждую восьмую
                if (beatPosition % 0.5 < 0.02) {
                    value += generateWave(8000, time, 0.1, "noise") * 0.5;
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
                        for (int h = 2; h <= 5; h++) {
                            value += Math.sin(2 * Math.PI * t * h) / h;
                        }
                        break;
                    case "noise":
                        value = random.nextDouble() * 2 - 1;
                        break;
                }

                return value * amplitude;
            }
        }

        // Класс для структуры композиции
        private class Composition {
            private final int totalMeasures = (int) (DURATION_SECONDS * bpm / 60 / 4);
            private final double[][] instrumentActivity;
            private final String structure;

            Composition() {
                instrumentActivity = new double[instruments.length][totalMeasures];

                // Создаем структуру: intro, verse, chorus, bridge, outro
                StringBuilder sb = new StringBuilder();

                int intro = totalMeasures / 8;
                int verse = totalMeasures / 4;
                int chorus = totalMeasures / 4;
                int bridge = totalMeasures / 8;
                int outro = totalMeasures / 8;

                sb.append("Intro(").append(intro).append(") ");
                sb.append("Verse(").append(verse).append(") ");
                sb.append("Chorus(").append(chorus).append(") ");
                if (random.nextBoolean()) {
                    sb.append("Bridge(").append(bridge).append(") ");
                }
                sb.append("Outro(").append(outro).append(")");

                structure = sb.toString();

                // Заполняем активность инструментов
                int measure = 0;

                // Intro - только некоторые инструменты
                for (int m = 0; m < intro; m++, measure++) {
                    for (int i = 0; i < instruments.length; i++) {
                        instrumentActivity[i][measure] = (i < instruments.length / 3) ? 0.3 : 0.0;
                    }
                }

                // Verse - основные инструменты
                for (int m = 0; m < verse; m++, measure++) {
                    for (int i = 0; i < instruments.length; i++) {
                        instrumentActivity[i][measure] = (i < instruments.length - 1) ? 0.7 : 0.3;
                    }
                }

                // Chorus - все инструменты на полную
                for (int m = 0; m < chorus; m++, measure++) {
                    for (int i = 0; i < instruments.length; i++) {
                        instrumentActivity[i][measure] = 1.0;
                    }
                }

                // Bridge - некоторые инструменты
                if (random.nextBoolean()) {
                    for (int m = 0; m < bridge; m++, measure++) {
                        for (int i = 0; i < instruments.length; i++) {
                            instrumentActivity[i][measure] = (i % 2 == 0) ? 0.5 : 0.2;
                        }
                    }
                }

                // Outro - затихание
                for (int m = 0; m < outro && measure < totalMeasures; m++, measure++) {
                    double fadeOut = 1.0 - (double) m / outro;
                    for (int i = 0; i < instruments.length; i++) {
                        instrumentActivity[i][measure] = fadeOut * 0.5;
                    }
                }
            }

            double getInstrumentVolume(String instrumentName, int measure) {
                if (measure >= totalMeasures) return 0;

                for (int i = 0; i < instruments.length; i++) {
                    if (instruments[i].name.equals(instrumentName)) {
                        return instrumentActivity[i][Math.min(measure, totalMeasures - 1)];
                    }
                }
                return 0;
            }

            String getStructure() {
                return structure;
            }
        }
    }
}
