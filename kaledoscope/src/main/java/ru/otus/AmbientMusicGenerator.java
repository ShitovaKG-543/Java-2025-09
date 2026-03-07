package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class AmbientMusicGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Медленные, протяжные гаммы
    private int[] modalScales = {0, 2, 4, 7, 9, 11, 12, 14, 16, 19, 21, 23, 24 // Миксолидийский
    };

    private int[] ambientScales = {0, 5, 7, 12, 17, 19, 24, 29, 31, 36 // Кварто-квинтовые интервалы
    };

    private int[] droneScales = {0, 12, 24, 36, 48, 60, 72, 84, 96 // Дроны по октавам
    };

    private int[] pentatonicAmbient = {0, 5, 7, 12, 17, 19, 24, 29, 31, 36, 41, 43, 48};

    // Режимы для эмбиента
    private enum AmbientMode {
        DEEP_AMBIENT, // Глубокий эмбиент
        CINEMATIC, // Кинематографичный
        MEDITATIVE, // Медитативный
        SPACE, // Космический
        UNDERWATER, // Подводный
        DREAM, // Сновидения
        DRONE, // Дроны
        TEXTURE // Текстурный
    }

    public static void main(String[] args) {
        AmbientMusicGenerator generator = new AmbientMusicGenerator();

        String filename =
                "ambient_music_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ МЕДЛЕННОЙ ИНСТРУМЕНТАЛЬНОЙ МУЗЫКИ ===");
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
                new AmbientMusicInputStream(DURATION_SECONDS * SAMPLE_RATE), format, DURATION_SECONDS * SAMPLE_RATE);

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

    private class AmbientMusicInputStream extends java.io.InputStream {
        private final long totalSamples;
        private long samplesGenerated = 0;
        private double time = 0;

        // Медленные параметры
        private final AmbientMode mode;
        private final double bpm; // Очень медленно
        private final int rootNote;
        private final int[] scale;

        // Инструменты для эмбиента
        private final AmbientInstrument[] instruments;

        public AmbientMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный режим
            this.mode = AmbientMode.values()[random.nextInt(AmbientMode.values().length)];

            // Очень низкие ноты для глубины
            this.rootNote = 36 + random.nextInt(24); // C2 до C4

            // Выбор гаммы
            int[][] scales = {modalScales, ambientScales, droneScales, pentatonicAmbient};
            this.scale = scales[random.nextInt(scales.length)];

            // Очень медленный темп (30-70 BPM)
            this.bpm = selectBpmForMode();

            // Создаем инструменты
            this.instruments = createInstrumentsForMode();

            printMusicInfo();
        }

        private double selectBpmForMode() {
            switch (mode) {
                case DEEP_AMBIENT:
                    return 30 + random.nextInt(20); // 30-50
                case CINEMATIC:
                    return 50 + random.nextInt(30); // 50-80
                case MEDITATIVE:
                    return 35 + random.nextInt(25); // 35-60
                case SPACE:
                    return 25 + random.nextInt(30); // 25-55
                case UNDERWATER:
                    return 20 + random.nextInt(25); // 20-45
                case DREAM:
                    return 40 + random.nextInt(30); // 40-70
                case DRONE:
                    return 10 + random.nextInt(20); // 10-30 (очень медленно)
                case TEXTURE:
                    return 15 + random.nextInt(25); // 15-40
                default:
                    return 40 + random.nextInt(30);
            }
        }

        private AmbientInstrument[] createInstrumentsForMode() {
            switch (mode) {
                case DEEP_AMBIENT:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("deep_pad", 0.3, "sine", 0.25, 8.0),
                        new AmbientInstrument("bass_drone", 0.25, "sine", 0.125, 12.0),
                        new AmbientInstrument("texture", 0.2, "noise", 1.0, 6.0),
                        new AmbientInstrument("bell", 0.15, "sawtooth", 2.0, 4.0),
                        new AmbientInstrument("choir", 0.2, "sawtooth", 1.0, 10.0)
                    };

                case CINEMATIC:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("strings", 0.3, "sawtooth", 0.5, 8.0),
                        new AmbientInstrument("brass", 0.2, "square", 0.3, 6.0),
                        new AmbientInstrument("piano", 0.2, "sine", 1.0, 5.0),
                        new AmbientInstrument("timpani", 0.15, "sine", 0.2, 4.0),
                        new AmbientInstrument("atmosphere", 0.25, "noise", 1.0, 10.0)
                    };

                case MEDITATIVE:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("tibetan_bowl", 0.3, "sine", 0.5, 12.0),
                        new AmbientInstrument("flute", 0.25, "sine", 1.0, 6.0),
                        new AmbientInstrument("drone", 0.3, "sawtooth", 0.2, 15.0),
                        new AmbientInstrument("chimes", 0.15, "sawtooth", 2.0, 5.0),
                        new AmbientInstrument("voice", 0.2, "sine", 1.0, 8.0)
                    };

                case SPACE:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("space_pad", 0.35, "sawtooth", 0.3, 12.0),
                        new AmbientInstrument("synth_drone", 0.3, "sine", 0.15, 15.0),
                        new AmbientInstrument("stars", 0.2, "noise", 3.0, 4.0),
                        new AmbientInstrument("nebula", 0.25, "sawtooth", 0.7, 8.0),
                        new AmbientInstrument("pulse", 0.15, "square", 0.1, 3.0)
                    };

                case UNDERWATER:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("water", 0.3, "noise", 1.0, 8.0),
                        new AmbientInstrument("bubble", 0.2, "sine", 0.8, 5.0),
                        new AmbientInstrument("deep_drone", 0.35, "sine", 0.1, 15.0),
                        new AmbientInstrument("whale", 0.25, "sawtooth", 0.5, 10.0),
                        new AmbientInstrument("echo", 0.2, "sine", 0.3, 7.0)
                    };

                case DREAM:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("dream_pad", 0.3, "sine", 0.5, 10.0),
                        new AmbientInstrument("harp", 0.2, "sawtooth", 1.0, 6.0),
                        new AmbientInstrument("glockenspiel", 0.2, "sine", 2.0, 4.0),
                        new AmbientInstrument("soft_drone", 0.25, "sawtooth", 0.2, 12.0),
                        new AmbientInstrument("wind", 0.15, "noise", 1.0, 8.0)
                    };

                case DRONE:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("drone_low", 0.4, "sawtooth", 0.1, 20.0),
                        new AmbientInstrument("drone_mid", 0.35, "sawtooth", 0.5, 18.0),
                        new AmbientInstrument("drone_high", 0.3, "sawtooth", 2.0, 15.0),
                        new AmbientInstrument("harmonic", 0.2, "sine", 1.0, 12.0),
                        new AmbientInstrument("resonance", 0.25, "square", 0.3, 16.0)
                    };

                case TEXTURE:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("granular", 0.3, "noise", 1.0, 8.0),
                        new AmbientInstrument("pulse", 0.2, "square", 0.2, 5.0),
                        new AmbientInstrument("glitch", 0.15, "noise", 2.0, 4.0),
                        new AmbientInstrument("drone", 0.3, "sawtooth", 0.4, 12.0),
                        new AmbientInstrument("fog", 0.25, "sine", 0.8, 10.0)
                    };

                default:
                    return new AmbientInstrument[] {
                        new AmbientInstrument("pad", 0.4, "sine", 0.5, 10.0),
                        new AmbientInstrument("drone", 0.3, "sawtooth", 0.2, 12.0)
                    };
            }
        }

        private void printMusicInfo() {
            System.out.println("  Режим: " + mode);
            System.out.println("  Тональность: " + getKeyName());
            System.out.println("  Темп: " + (int) bpm + " BPM (очень медленно)");
            System.out.println("  Инструментов: " + instruments.length);
        }

        private String getKeyName() {
            String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            String note = notes[(rootNote - 36) % 12];
            return note + " минор/модальный";
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
                for (AmbientInstrument instrument : instruments) {
                    sampleValue += instrument.generateSample(time);
                }

                // Очень мягкая нормализация для эмбиента
                sampleValue = Math.tanh(sampleValue * 0.5);

                // Медленное панорамирование
                double pan = Math.sin(time * 0.1) * 0.4;

                short sampleLeft = (short) (sampleValue * (0.6 - pan) * 32767);
                short sampleRight = (short) (sampleValue * (0.6 + pan) * 32767);

                b[off + i * bytesPerSample] = (byte) (sampleLeft & 0xFF);
                b[off + i * bytesPerSample + 1] = (byte) ((sampleLeft >> 8) & 0xFF);
                b[off + i * bytesPerSample + 2] = (byte) (sampleRight & 0xFF);
                b[off + i * bytesPerSample + 3] = (byte) ((sampleRight >> 8) & 0xFF);
            }

            samplesGenerated += samplesToGenerate;
            return samplesToGenerate * bytesPerSample;
        }

        // Класс для эмбиент-инструментов
        private class AmbientInstrument {
            final String name;
            final double baseVolume;
            final String waveType;
            final double octaveMultiplier;
            final double noteDuration; // Очень долгие ноты (в секундах)

            // Медленные, развивающиеся паттерны
            double[] droneNotes;
            double[] evolution;
            double[] filterCutoff;

            AmbientInstrument(
                    String name, double baseVolume, String waveType, double octaveMultiplier, double noteDuration) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.waveType = waveType;
                this.octaveMultiplier = octaveMultiplier;
                this.noteDuration = noteDuration;

                generateAmbientPatterns();
            }

            private void generateAmbientPatterns() {
                droneNotes = new double[8];
                evolution = new double[1024];
                filterCutoff = new double[256];

                // Дроновые ноты (очень низкие частоты)
                for (int i = 0; i < droneNotes.length; i++) {
                    int scaleIndex = random.nextInt(scale.length / 2);
                    droneNotes[i] = scale[scaleIndex];
                }

                // Медленная эволюция звука
                for (int i = 0; i < evolution.length; i++) {
                    evolution[i] = 0.5 + 0.5 * Math.sin(i * 0.01);
                }

                // Медленные изменения фильтра
                for (int i = 0; i < filterCutoff.length; i++) {
                    filterCutoff[i] = 0.3 + 0.7 * Math.sin(i * 0.02);
                }
            }

            double generateSample(double time) {
                double beatPosition = time * bpm / 60.0;

                // Очень медленные изменения громкости
                double volume = baseVolume * (0.7 + 0.3 * Math.sin(time * 0.1));

                // Индекс эволюции
                int evolutionIndex = (int) (time * 10) % evolution.length;
                double evolutionFactor = evolution[evolutionIndex];

                double value = 0;

                // Дрон (основной тон)
                int droneIndex = ((int) (time / noteDuration)) % droneNotes.length;
                double droneFreq =
                        440 * Math.pow(2, (rootNote - 57 + droneNotes[droneIndex]) / 12.0) * octaveMultiplier;

                // Дрон звучит постоянно, очень медленно меняясь
                double droneValue = generateWave(droneFreq, time, 0.8, waveType);

                // Добавляем гармоники для богатства
                for (int h = 1; h <= 3; h++) {
                    droneValue += generateWave(droneFreq * (h + 1), time, 0.3 / h, waveType);
                }

                value += droneValue * 0.5;

                // Периодические акценты (очень редкие)
                if (Math.sin(time * 0.2) > 0.95 && random.nextDouble() > 0.7) {
                    int accentNote = random.nextInt(scale.length);
                    double accentFreq =
                            440 * Math.pow(2, (rootNote - 45 + scale[accentNote]) / 12.0) * octaveMultiplier * 2;
                    double accentAmp = 0.3 * Math.exp(-(time % 5) * 0.5);
                    value += generateWave(accentFreq, time, accentAmp, "sine");
                }

                // Текстурный слой
                if (waveType.equals("noise")) {
                    double noiseValue = generateWave(0, time, 0.3, "noise");
                    // Фильтруем шум для мягкости
                    noiseValue *= 0.5 + 0.5 * Math.sin(time * 50);
                    value += noiseValue;
                }

                // Медленные биения
                value *= 0.8 + 0.2 * Math.sin(time * 0.5);

                // Применяем фильтр
                int filterIndex = (int) (time * 5) % filterCutoff.length;
                value *= filterCutoff[filterIndex];

                return value * volume * evolutionFactor;
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
                        // Мягкие гармоники для эмбиента
                        for (int h = 2; h <= 4; h++) {
                            value += Math.sin(2 * Math.PI * t * h) / (h * 1.5);
                        }
                        break;
                    case "noise":
                        value = random.nextDouble() * 2 - 1;
                        // Фильтруем шум для мягкости
                        value *= 0.5 + 0.5 * Math.sin(time * 200);
                        break;
                }

                return value * amplitude;
            }
        }
    }
}
