package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class PianoMusicGenerator2 {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Мажорные гаммы (светлые, радостные)
    private int[] cMajorScale = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24}; // До мажор
    private int[] gMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Соль мажор
    private int[] dMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ре мажор
    private int[] aMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ля мажор
    private int[] eMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ми мажор
    private int[] fMajorScale = {0, 2, 4, 5, 7, 9, 10, 12, 14, 16, 17, 19, 21, 22, 24}; // Фа мажор

    // Минорные гаммы (меланхоличные, но красивые)
    private int[] aMinorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24}; // Ля минор
    private int[] eMinorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24}; // Ми минор
    private int[] dMinorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24}; // Ре минор
    private int[] gMinorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24}; // Соль минор
    private int[] cMinorScale = {0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20, 22, 24}; // До минор

    // Позитивные стили для фортепиано
    private enum PianoStyle {
        POSITIVE_MAJOR, // Светлый, радостный мажор
        DREAMY_MAJOR, // Мечтательный мажор
        GENTLE_MINOR, // Нежный минор (красивый, не грустный)
        HOPEFUL_MINOR, // Надежный минор (светлая грусть)
        JOYFUL_MIX, // Радостное смешение
        PEACEFUL_MIX // Мирное, спокойное смешение
    }

    public static void main(String[] args) {
        PianoMusicGenerator2 generator = new PianoMusicGenerator2();

        String filename =
                "piano_music_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ КРАСИВОЙ ПОЗИТИВНОЙ МЕЛОДИИ ===");
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
                new PianoMusicInputStream(DURATION_SECONDS * SAMPLE_RATE), format, DURATION_SECONDS * SAMPLE_RATE);

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

    private class PianoMusicInputStream extends java.io.InputStream {
        private final long totalSamples;
        private long samplesGenerated = 0;
        private double time = 0;

        // Музыкальные параметры
        private final PianoStyle style;
        private final int rootNote;
        private final int[] scale;
        private final boolean isMajor;
        private final double bpm;

        // Фортепианные партии
        private final PianoVoice melody; // Основная мелодия
        private final PianoVoice harmony; // Гармонический аккомпанемент
        private final PianoVoice bass; // Басовая линия
        private final PianoVoice ornament; // Украшения и переливы

        public PianoMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный выбор стиля
            this.style = PianoStyle.values()[random.nextInt(PianoStyle.values().length)];

            // Выбор тональности в зависимости от стиля
            int[][] majorScales = {cMajorScale, gMajorScale, dMajorScale, aMajorScale, eMajorScale, fMajorScale};
            int[][] minorScales = {aMinorScale, eMinorScale, dMinorScale, gMinorScale, cMinorScale};

            switch (style) {
                case POSITIVE_MAJOR:
                case DREAMY_MAJOR:
                    this.scale = majorScales[random.nextInt(majorScales.length)];
                    this.isMajor = true;
                    break;
                case GENTLE_MINOR:
                case HOPEFUL_MINOR:
                    this.scale = minorScales[random.nextInt(minorScales.length)];
                    this.isMajor = false;
                    break;
                case JOYFUL_MIX:
                case PEACEFUL_MIX:
                    // Случайно выбираем мажор или минор для смешения
                    this.scale = random.nextBoolean()
                            ? majorScales[random.nextInt(majorScales.length)]
                            : minorScales[random.nextInt(minorScales.length)];
                    this.isMajor = random.nextBoolean();
                    break;
                default:
                    this.scale = majorScales[0];
                    this.isMajor = true;
            }

            // Выбор тональности (разные октавы для красоты)
            this.rootNote = 48 + random.nextInt(12); // C3 до B3

            // Красивый, протяжный темп
            this.bpm = selectBeautifulBpm();

            // Создаем голоса с красивыми настройками
            this.melody = new PianoVoice("melody", 0.35, 2.0, true); // Мелодия
            this.harmony = new PianoVoice("harmony", 0.2, 1.0, false); // Аккомпанемент
            this.bass = new PianoVoice("bass", 0.2, 0.5, false); // Бас
            this.ornament = new PianoVoice("ornament", 0.15, 1.5, false); // Украшения

            printMusicInfo();
        }

        private double selectBeautifulBpm() {
            switch (style) {
                case POSITIVE_MAJOR:
                    return 70 + random.nextInt(30); // 70-100 (бодрый мажор)
                case DREAMY_MAJOR:
                    return 55 + random.nextInt(30); // 55-85 (мечтательный)
                case GENTLE_MINOR:
                    return 50 + random.nextInt(30); // 50-80 (нежный минор)
                case HOPEFUL_MINOR:
                    return 60 + random.nextInt(30); // 60-90 (надежный)
                case JOYFUL_MIX:
                    return 65 + random.nextInt(35); // 65-100 (радостный)
                case PEACEFUL_MIX:
                    return 45 + random.nextInt(35); // 45-80 (мирный)
                default:
                    return 60 + random.nextInt(30);
            }
        }

        private void printMusicInfo() {
            System.out.println("  Стиль: " + style);
            System.out.println("  Тональность: " + getKeyName() + (isMajor ? " мажор" : " минор"));
            System.out.println("  Темп: " + (int) bpm + " BPM (протяжный)");
            System.out.println("  Фортепиано: позитивное соло");
        }

        private String getKeyName() {
            String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
            String note = notes[(rootNote - 48) % 12];
            return note;
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

                // Собираем звук от всех голосов фортепиано
                double sampleValue = 0;
                sampleValue += melody.generateSample(time);
                sampleValue += harmony.generateSample(time);
                sampleValue += bass.generateSample(time);
                sampleValue += ornament.generateSample(time);

                // Мягкая нормализация
                sampleValue = Math.tanh(sampleValue * 0.7);

                // Красивая стерео панорама
                double pan = Math.sin(time * 0.1) * 0.15;

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

        // Класс для фортепианного голоса
        private class PianoVoice {
            final String name;
            final double baseVolume;
            final double octaveMultiplier;
            final boolean isMainMelody;

            // Красивые музыкальные паттерны
            double[] notePattern;
            double[] rhythmPattern;
            double[] dynamicPattern;
            double[] expressionPattern;

            PianoVoice(String name, double baseVolume, double octaveMultiplier, boolean isMainMelody) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.octaveMultiplier = octaveMultiplier;
                this.isMainMelody = isMainMelody;

                generateBeautifulPatterns();
            }

            private void generateBeautifulPatterns() {
                int patternLength = 64; // Длинные паттерны
                notePattern = new double[patternLength];
                rhythmPattern = new double[patternLength];
                dynamicPattern = new double[patternLength];
                expressionPattern = new double[patternLength];

                for (int i = 0; i < patternLength; i++) {
                    // Красивые ноты в зависимости от тональности
                    if (isMainMelody) {
                        if (isMajor) {
                            // Для мажора - светлые, радостные ступени
                            int[] majorBeautifulSteps = {0, 2, 4, 5, 7, 9, 12, 14};
                            notePattern[i] = majorBeautifulSteps[random.nextInt(majorBeautifulSteps.length)];
                        } else {
                            // Для минора - красивые, меланхоличные ступени
                            int[] minorBeautifulSteps = {0, 3, 5, 7, 8, 10, 12, 15};
                            notePattern[i] = minorBeautifulSteps[random.nextInt(minorBeautifulSteps.length)];
                        }
                    } else {
                        // Для аккомпанемента - гармоничные интервалы
                        notePattern[i] = random.nextInt(scale.length / 2);
                    }

                    // Протяжные ритмы (длинные ноты)
                    if (isMainMelody) {
                        // Мелодия играет целыми, половинными
                        double[] beautifulRhythms = {2.0, 1.5, 1.0, 2.0, 1.0};
                        rhythmPattern[i] = beautifulRhythms[random.nextInt(beautifulRhythms.length)];
                    } else {
                        rhythmPattern[i] = 1.0 + random.nextDouble() * 0.5;
                    }

                    // Красивая динамика
                    dynamicPattern[i] = 0.5 + 0.5 * Math.sin(i * 0.2);
                    expressionPattern[i] = 0.7 + 0.3 * Math.sin(i * 0.4);
                }
            }

            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Плотность нот
                double noteDensity;
                switch (name) {
                    case "melody":
                        noteDensity = 0.6; // Мелодия редкая
                        break;
                    case "harmony":
                        noteDensity = 1.2; // Аккорды
                        break;
                    case "bass":
                        noteDensity = 0.7; // Бас
                        break;
                    case "ornament":
                        noteDensity = 2.0; // Украшения
                        break;
                    default:
                        noteDensity = 1.0;
                }

                // Плавная генерация нот
                double notePosition = beatPosition * noteDensity;
                int patternLength = notePattern.length;
                int noteIndex = ((int) (notePosition * 1.5)) % patternLength;

                double noteStart = Math.floor(notePosition * 1.5) / 1.5;
                double noteDuration = rhythmPattern[noteIndex] * 0.9;
                double noteEnd = noteStart + noteDuration;

                if (notePosition >= noteStart && notePosition < noteEnd) {
                    int scaleIndex = (int) notePattern[noteIndex];
                    scaleIndex = Math.min(scaleIndex, scale.length - 1);

                    double freq = 440 * Math.pow(2, (rootNote - 57 + scale[scaleIndex]) / 12.0) * octaveMultiplier;

                    double dynamic = dynamicPattern[noteIndex] * baseVolume;
                    if (isMainMelody) {
                        dynamic *= expressionPattern[noteIndex];
                    }

                    double pedal = 1.0;
                    if (name.equals("harmony") || name.equals("ornament")) {
                        pedal = 0.8 + 0.2 * Math.exp(-(notePosition - noteStart) * 1.5);
                    }

                    double noteValue = 0;
                    double posInNote = notePosition - noteStart;

                    double attack = 1.0;
                    if (posInNote < 0.03) {
                        attack = posInNote / 0.03;
                    }

                    // Богатые гармоники для красивого звука
                    for (int h = 1; h <= 6; h++) {
                        double harmonicAmp;
                        if (h == 1) harmonicAmp = 1.0;
                        else if (h == 2) harmonicAmp = 0.55;
                        else if (h == 3) harmonicAmp = 0.35;
                        else if (h == 4) harmonicAmp = 0.2;
                        else harmonicAmp = 0.1 / (h - 3);

                        if (h > 3) harmonicAmp *= Math.exp(-posInNote * 4);

                        noteValue += Math.sin(2 * Math.PI * freq * h * time) * harmonicAmp;
                    }

                    double decay = Math.exp(-posInNote * 1.3);
                    value += noteValue * dynamic * attack * decay * pedal;
                }

                // Легкий резонанс
                if (name.equals("harmony") && random.nextDouble() > 0.98) {
                    value += value * 0.03 * Math.sin(time * 70);
                }

                return value;
            }
        }
    }
}
