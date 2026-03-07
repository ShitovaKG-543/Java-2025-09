package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class PianoMusicGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    // Мажорные гаммы (красивые и протяжные)
    private int[] cMajorScale = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24}; // До мажор
    private int[] gMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Соль мажор
    private int[] dMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ре мажор
    private int[] aMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ля мажор
    private int[] eMajorScale = {0, 2, 4, 6, 7, 9, 11, 12, 14, 16, 18, 19, 21, 23, 24}; // Ми мажор
    private int[] fMajorScale = {0, 2, 4, 5, 7, 9, 10, 12, 14, 16, 17, 19, 21, 22, 24}; // Фа мажор

    // Стили фортепианной музыки (только мажорные, красивые)
    private enum PianoStyle {
        ROMANTIC, // Романтичный (очень красивый, протяжный)
        CINEMATIC, // Кинематографичный (эпичный, красивый)
        MEDITATIVE, // Медитативный (спокойный, протяжный)
        BALLAD, // Баллада (красивая, мелодичная)
        NOCTURNE, // Ноктюрн (ночной, мечтательный)
        BERCEUSE // Колыбельная (нежная, успокаивающая)
    }

    public static void main(String[] args) {
        PianoMusicGenerator generator = new PianoMusicGenerator();

        String filename =
                "piano_music_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ КРАСИВОЙ МАЖОРНОЙ МЕЛОДИИ ===");
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

        // Музыкальные параметры (все мажорные и красивые)
        private final PianoStyle style;
        private final int rootNote;
        private final int[] scale;
        private final double bpm;

        // Фортепианные партии
        private final PianoVoice melody; // Основная мелодия (самая красивая)
        private final PianoVoice harmony; // Гармонический аккомпанемент
        private final PianoVoice bass; // Басовая линия
        private final PianoVoice ornament; // Украшения и переливы

        public PianoMusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;

            // Случайный выбор стиля (только красивые)
            this.style = PianoStyle.values()[random.nextInt(PianoStyle.values().length)];

            // Выбор мажорной тональности (самые красивые)
            int[][] majorScales = {cMajorScale, gMajorScale, dMajorScale, aMajorScale, eMajorScale, fMajorScale};
            this.scale = majorScales[random.nextInt(majorScales.length)];

            // Выбор тональности (разные октавы для красоты)
            this.rootNote = 48 + random.nextInt(12); // C3 до B3

            // Очень красивый, протяжный темп
            this.bpm = selectBeautifulBpm();

            // Создаем голоса с красивыми настройками
            this.melody = new PianoVoice("melody", 0.35, 2.0, true); // Мелодия (основная)
            this.harmony = new PianoVoice("harmony", 0.2, 1.0, false); // Аккомпанемент
            this.bass = new PianoVoice("bass", 0.2, 0.5, false); // Бас
            this.ornament = new PianoVoice("ornament", 0.15, 1.5, false); // Украшения

            printMusicInfo();
        }

        private double selectBeautifulBpm() {
            switch (style) {
                case ROMANTIC:
                    return 60 + random.nextInt(30); // 60-90 (очень красиво)
                case CINEMATIC:
                    return 70 + random.nextInt(30); // 70-100 (эпично)
                case MEDITATIVE:
                    return 50 + random.nextInt(30); // 50-80 (медитативно)
                case BALLAD:
                    return 55 + random.nextInt(30); // 55-85 (баллада)
                case NOCTURNE:
                    return 45 + random.nextInt(30); // 45-75 (ноктюрн)
                case BERCEUSE:
                    return 40 + random.nextInt(30); // 40-70 (колыбельная)
                default:
                    return 60 + random.nextInt(30);
            }
        }

        private void printMusicInfo() {
            System.out.println("  Стиль: " + style);
            System.out.println("  Тональность: " + getKeyName() + " мажор");
            System.out.println("  Темп: " + (int) bpm + " BPM (протяжный)");
            System.out.println("  Фортепиано: красивое соло");
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

                // Мягкая нормализация для красивого звука
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
            double[] expressionPattern; // Для выразительности

            PianoVoice(String name, double baseVolume, double octaveMultiplier, boolean isMainMelody) {
                this.name = name;
                this.baseVolume = baseVolume;
                this.octaveMultiplier = octaveMultiplier;
                this.isMainMelody = isMainMelody;

                generateBeautifulPatterns();
            }

            private void generateBeautifulPatterns() {
                int patternLength = 48; // Длинные паттерны для разнообразия
                notePattern = new double[patternLength];
                rhythmPattern = new double[patternLength];
                dynamicPattern = new double[patternLength];
                expressionPattern = new double[patternLength];

                for (int i = 0; i < patternLength; i++) {
                    // Красивые ноты в мажорной гамме (избегаем диссонансов)
                    if (isMainMelody) {
                        // Для мелодии выбираем самые красивые ступени: 1, 3, 5, 6, 8
                        int[] beautifulSteps = {0, 2, 4, 5, 7, 9, 12};
                        notePattern[i] = beautifulSteps[random.nextInt(beautifulSteps.length)];
                    } else {
                        // Для аккомпанемента можно больше разнообразия
                        notePattern[i] = random.nextInt(scale.length / 2);
                    }

                    // Протяжные ритмы (длинные ноты)
                    if (isMainMelody) {
                        // Мелодия играет целыми, половинными и четвертями
                        double[] beautifulRhythms = {2.0, 1.5, 1.0, 1.0, 0.75};
                        rhythmPattern[i] = beautifulRhythms[random.nextInt(beautifulRhythms.length)];
                    } else {
                        // Аккомпанемент более размеренный
                        rhythmPattern[i] = 1.0 + random.nextDouble();
                    }

                    // Красивая динамика (крещендо и диминуэндо)
                    dynamicPattern[i] = 0.5 + 0.5 * Math.sin(i * 0.3);

                    // Выразительность (для мелодии)
                    expressionPattern[i] = 0.7 + 0.3 * Math.sin(i * 0.5);
                }
            }

            double generateSample(double time) {
                double value = 0;
                double beatPosition = time * bpm / 60.0;

                // Плотность нот (очень плавная)
                double noteDensity;
                switch (name) {
                    case "melody":
                        noteDensity = 0.5; // Мелодия редкая и красивая
                        break;
                    case "harmony":
                        noteDensity = 1.0; // Аккорды размеренные
                        break;
                    case "bass":
                        noteDensity = 0.5; // Бас редкий
                        break;
                    case "ornament":
                        noteDensity = 2.0; // Украшения чаще
                        break;
                    default:
                        noteDensity = 1.0;
                }

                // Плавная генерация нот
                double notePosition = beatPosition * noteDensity;
                int patternLength = notePattern.length;
                int noteIndex = ((int) (notePosition * 2)) % patternLength;

                // Проверяем, должна ли играть нота
                double noteStart = Math.floor(notePosition * 2) / 2;
                double noteDuration = rhythmPattern[noteIndex] * 0.8; // Длинные ноты
                double noteEnd = noteStart + noteDuration;

                // Добавляем небольшие паузы для выразительности
                if (notePosition >= noteStart && notePosition < noteEnd) {
                    // Вычисляем частоту ноты
                    int scaleIndex = (int) notePattern[noteIndex];
                    double freq = 440 * Math.pow(2, (rootNote - 57 + scale[scaleIndex] / 2) / 12.0) * octaveMultiplier;

                    // Красивая динамика
                    double dynamic = dynamicPattern[noteIndex] * baseVolume;

                    // Выразительность для мелодии
                    if (isMainMelody) {
                        dynamic *= expressionPattern[noteIndex];
                    }

                    // Педальный эффект
                    double pedal = 1.0;
                    if (name.equals("harmony") || name.equals("ornament")) {
                        pedal = 0.8 + 0.2 * Math.exp(-(notePosition - noteStart) * 1.5);
                    }

                    // Фортепианный звук
                    double noteValue = 0;
                    double posInNote = notePosition - noteStart;

                    // Мягкий атака (для красивого звука)
                    double attack = 1.0;
                    if (posInNote < 0.03) {
                        attack = posInNote / 0.03;
                    }

                    // Красивые гармоники для богатого звука
                    for (int h = 1; h <= 6; h++) {
                        double harmonicAmp;
                        if (h == 1) harmonicAmp = 1.0; // Основной тон
                        else if (h == 2) harmonicAmp = 0.6; // Октава
                        else if (h == 3) harmonicAmp = 0.4; // Квинта
                        else if (h == 4) harmonicAmp = 0.25; // Двойная октава
                        else harmonicAmp = 0.15 / (h - 3); // Высшие гармоники

                        // Мягкое затухание высоких частот
                        if (h > 3) harmonicAmp *= Math.exp(-posInNote * 3);

                        noteValue += Math.sin(2 * Math.PI * freq * h * time) * harmonicAmp;
                    }

                    // Красивое затухание
                    double decay = Math.exp(-posInNote * 1.2);

                    value += noteValue * dynamic * attack * decay * pedal;
                }

                // Эффект резонанса для педали
                if (name.equals("harmony") && random.nextDouble() > 0.95) {
                    value += value * 0.05 * Math.sin(time * 80);
                }

                return value;
            }
        }
    }
}
