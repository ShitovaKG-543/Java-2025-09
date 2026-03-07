package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class PianoMusicGenerator3 {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Мажорные гаммы (только основные ступени для плавности)
    private int[] cMajorScale = {0, 2, 4, 5, 7, 9, 11, 12}; // До мажор (только первая октава)
    private int[] gMajorScale = {0, 2, 4, 5, 7, 9, 11, 12}; // Соль мажор
    private int[] dMajorScale = {0, 2, 4, 5, 7, 9, 11, 12}; // Ре мажор
    private int[] aMajorScale = {0, 2, 4, 5, 7, 9, 11, 12}; // Ля мажор
    private int[] fMajorScale = {0, 2, 4, 5, 7, 9, 10, 12}; // Фа мажор

    // Минорные гаммы (только основные ступени)
    private int[] aMinorScale = {0, 2, 3, 5, 7, 8, 10, 12}; // Ля минор
    private int[] eMinorScale = {0, 2, 3, 5, 7, 8, 10, 12}; // Ми минор
    private int[] dMinorScale = {0, 2, 3, 5, 7, 8, 10, 12}; // Ре минор
    private int[] cMinorScale = {0, 2, 3, 5, 7, 8, 10, 12}; // До минор

    // Только плавные, приятные стили
    private enum PianoStyle {
        GENTLE_MAJOR, // Нежный мажор
        SOFT_MINOR, // Мягкий минор
        MELLOW_MIX // Спокойное смешение
    }

    public static void main(String[] args) {
        PianoMusicGenerator3 generator = new PianoMusicGenerator3();

        String filename =
                "piano_music_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ ПЛАВНОЙ ФОРТЕПИАННОЙ МУЗЫКИ ===");
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

        public PianoMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный выбор стиля
            this.style = PianoStyle.values()[random.nextInt(PianoStyle.values().length)];

            // Выбор тональности в зависимости от стиля
            int[][] majorScales = {cMajorScale, gMajorScale, dMajorScale, aMajorScale, fMajorScale};
            int[][] minorScales = {aMinorScale, eMinorScale, dMinorScale, cMinorScale};

            switch (style) {
                case GENTLE_MAJOR:
                    this.scale = majorScales[random.nextInt(majorScales.length)];
                    this.isMajor = true;
                    break;
                case SOFT_MINOR:
                    this.scale = minorScales[random.nextInt(minorScales.length)];
                    this.isMajor = false;
                    break;
                case MELLOW_MIX:
                    // Для смешения используем только приятные сочетания
                    if (random.nextBoolean()) {
                        this.scale = majorScales[random.nextInt(majorScales.length)];
                        this.isMajor = true;
                    } else {
                        this.scale = minorScales[random.nextInt(minorScales.length)];
                        this.isMajor = false;
                    }
                    break;
                default:
                    this.scale = majorScales[0];
                    this.isMajor = true;
            }

            // Выбор тональности - средний регистр (никаких высоких нот)
            this.rootNote = 48 + random.nextInt(8); // C3 до G3 (ограничиваем высоту)

            // Медленный, плавный темп
            this.bpm = 50 + random.nextInt(30); // 50-80 BPM

            // Создаем голоса - убираем орнамент, оставляем только основные
            this.melody = new PianoVoice("melody", 0.3, 1.0, true); // Мелодия
            this.harmony = new PianoVoice("harmony", 0.2, 0.8, false); // Аккомпанемент
            this.bass = new PianoVoice("bass", 0.2, 0.4, false); // Бас

            printMusicInfo();
        }

        private void printMusicInfo() {
            System.out.println("  Стиль: " + style);
            System.out.println("  Тональность: " + getKeyName() + (isMajor ? " мажор" : " минор"));
            System.out.println("  Темп: " + (int) bpm + " BPM (медленный)");
            System.out.println("  Фортепиано: плавное, без высоких нот");
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

                // Очень мягкая нормализация
                sampleValue = Math.tanh(sampleValue * 0.6);

                // Легкая стерео панорама
                double pan = Math.sin(time * 0.1) * 0.1;

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

            // Плавные музыкальные паттерны
            double[] notePattern;
            double[] rhythmPattern;
            double[] dynamicPattern;

            PianoVoice(String name, double baseVolume, double octaveMultiplier, boolean isMainMelody) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.octaveMultiplier = octaveMultiplier;
                this.isMainMelody = isMainMelody;

                generateSmoothPatterns();
            }

            private void generateSmoothPatterns() {
                int patternLength = 32; // Короткие паттерны для повторения
                notePattern = new double[patternLength];
                rhythmPattern = new double[patternLength];
                dynamicPattern = new double[patternLength];

                for (int i = 0; i < patternLength; i++) {
                    // Только консонансные интервалы (никаких диссонансов)
                    if (isMainMelody) {
                        // Для мелодии только 1, 3, 5 ступени (самые благозвучные)
                        int[] consonantSteps = {0, 2, 4, 7, 9, 12};
                        notePattern[i] = consonantSteps[random.nextInt(consonantSteps.length)];
                    } else {
                        // Для аккомпанемента только тоника и доминанта
                        int[] harmonySteps = {0, 4, 7, 12};
                        notePattern[i] = harmonySteps[random.nextInt(harmonySteps.length)];
                    }

                    // Плавные ритмы (только длинные ноты)
                    double[] smoothRhythms = {2.0, 1.5, 1.0};
                    rhythmPattern[i] = smoothRhythms[random.nextInt(smoothRhythms.length)];

                    // Мягкая динамика
                    dynamicPattern[i] = 0.5 + 0.3 * Math.sin(i * 0.2);
                }
            }

            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Очень плавная плотность нот
                double noteDensity = isMainMelody ? 0.4 : 0.8;

                double notePosition = beatPosition * noteDensity;
                int patternLength = notePattern.length;
                int noteIndex = ((int) (notePosition)) % patternLength;

                double noteStart = Math.floor(notePosition);
                double noteDuration = rhythmPattern[noteIndex] * 1.2; // Очень длинные ноты
                double noteEnd = noteStart + noteDuration;

                if (notePosition >= noteStart && notePosition < noteEnd) {
                    int scaleIndex = (int) notePattern[noteIndex];
                    scaleIndex = Math.min(scaleIndex, scale.length - 1);

                    // Ограничиваем октаву - не выше 5-й
                    double octaveLimit = Math.min(octaveMultiplier, 1.0);
                    double freq = 220 * Math.pow(2, (rootNote - 48 + scale[scaleIndex]) / 12.0) * octaveLimit;

                    double dynamic = dynamicPattern[noteIndex] * baseVolume;

                    double posInNote = notePosition - noteStart;

                    // Очень мягкий атака
                    double attack = 1.0;
                    if (posInNote < 0.05) {
                        attack = posInNote / 0.05;
                    }

                    // Плавное затухание
                    double decay = Math.exp(-posInNote * 1.0);

                    // Минимум гармоник для чистого звука (только основные)
                    double noteValue = 0;

                    // Основной тон
                    noteValue += Math.sin(2 * Math.PI * freq * time) * 1.0;

                    // Очень тихая октава для теплоты
                    noteValue += Math.sin(2 * Math.PI * freq * 2 * time) * 0.15 * decay;

                    // Квинта чуть слышна
                    noteValue += Math.sin(2 * Math.PI * freq * 1.5 * time) * 0.1 * decay;

                    value += noteValue * dynamic * attack * decay;
                }

                return value;
            }
        }
    }
}
