package ru.otus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.sound.sampled.*;

public class MusicGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 2; // Стерео

    private final Random random = new Random();

    public static void main(String[] args) {
        MusicGenerator generator = new MusicGenerator();

        String filename = "generated_music_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav";

        System.out.println("=== ГЕНЕРАЦИЯ МУЗЫКИ ===");
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
        // Создаем аудио формат
        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                BITS_PER_SAMPLE,
                CHANNELS,
                true, // signed
                false // little-endian
                );

        // Создаем файл
        File file = new File(filename);

        // Открываем аудио поток для записи
        AudioInputStream audioStream = new AudioInputStream(
                new MusicInputStream(DURATION_SECONDS * SAMPLE_RATE), format, DURATION_SECONDS * SAMPLE_RATE);

        // Записываем WAV файл
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
            } else {
                System.out.println("  WAV файл сохранен (FFmpeg не найден)");
            }

        } catch (Exception e) {
            System.out.println("  WAV файл сохранен (FFmpeg не найден)");
        }
    }

    // Класс для генерации аудио данных
    private class MusicInputStream extends java.io.InputStream {
        private final long totalSamples;
        private long samplesGenerated = 0;

        // Музыкальные параметры
        private double time = 0;
        private double bpm = 120;
        private double[] melody; // Ноты для мелодии
        private double[] chords; // Аккорды

        public MusicInputStream(long totalSamples) {
            this.totalSamples = totalSamples;
            generateMusicalStructure();
        }

        private void generateMusicalStructure() {
            // Выбираем тональность (C мажор или A минор)
            boolean isMajor = random.nextBoolean();

            // Основные ноты (в полутонах от C4)
            int[] majorScale = {0, 2, 4, 5, 7, 9, 11, 12}; // До мажор
            int[] minorScale = {0, 2, 3, 5, 7, 8, 10, 12}; // Ля минор
            int[] scale = isMajor ? majorScale : minorScale;

            System.out.println("  Тональность: " + (isMajor ? "До мажор" : "Ля минор"));
            System.out.println("  Темп: " + (int) bpm + " BPM");

            // Генерируем мелодию (32 ноты)
            melody = new double[32];
            for (int i = 0; i < melody.length; i++) {
                int noteIndex = random.nextInt(scale.length - 1);
                double frequency = 440 * Math.pow(2, (scale[noteIndex] - 9) / 12.0);
                melody[i] = frequency;
            }

            // Генерируем аккорды (8 аккордов)
            chords = new double[8];
            for (int i = 0; i < chords.length; i++) {
                int noteIndex = random.nextInt(scale.length - 3);
                double frequency = 440 * Math.pow(2, (scale[noteIndex] - 21) / 12.0);
                chords[i] = frequency;
            }
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

            if (samplesToGenerate == 0) {
                return 0;
            }

            for (int i = 0; i < samplesToGenerate; i++) {
                time = (samplesGenerated + i) / (double) SAMPLE_RATE;

                // Генерируем звук
                double sampleValue = generateSample(time);

                // Конвертируем в 16-bit PCM
                short sample = (short) (sampleValue * 32767);

                // Записываем для левого и правого каналов
                for (int ch = 0; ch < CHANNELS; ch++) {
                    b[off + i * bytesPerSample + ch * 2] = (byte) (sample & 0xFF);
                    b[off + i * bytesPerSample + ch * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                }
            }

            samplesGenerated += samplesToGenerate;
            return samplesToGenerate * bytesPerSample;
        }

        private double generateSample(double time) {
            double value = 0;

            // Основная мелодия - ИСПРАВЛЕНО
            double beatsPerSecond = bpm / 60.0;
            double beatPosition = time * beatsPerSecond;

            // Безопасное вычисление индекса (исправляем ошибку с отрицательным индексом)
            int melodyIndex = ((int) Math.floor(beatPosition * 2)) % melody.length;
            if (melodyIndex < 0) melodyIndex = 0;

            double melodyFreq = melody[melodyIndex];
            double melodyAmp = 0.3 * (0.8 + 0.2 * Math.sin(time * 2));
            value += generateWave(melodyFreq, time, melodyAmp, "sawtooth");

            // Аккорды (пианино)
            if (random.nextInt(10) < 3) {
                int chordIndex = ((int) Math.floor(beatPosition / 2)) % chords.length;
                if (chordIndex < 0) chordIndex = 0;

                double chordFreq = chords[chordIndex];
                double chordAmp = 0.2 * (0.7 + 0.3 * Math.sin(time * 1.5));
                value += generateWave(chordFreq, time, chordAmp, "sine");

                // Добавляем квинту
                value += generateWave(chordFreq * 1.5, time, chordAmp * 0.7, "sine");
            }

            // Бас-барабан (низкие частоты)
            if (time % (60 / bpm) < 0.05) {
                double bassFreq = 60;
                double bassAmp = 0.4 * Math.exp(-(time % (60 / bpm)) * 40);
                value += generateWave(bassFreq, time, bassAmp, "sine");
            }

            // Хай-хэт (высокие частоты)
            if (time % (60 / bpm / 2) < 0.02) {
                double hatFreq = 8000;
                double hatAmp = 0.15 * (0.5 + 0.5 * Math.random());
                value += generateWave(hatFreq, time, hatAmp, "noise");
            }

            // Ограничиваем амплитуду
            return Math.max(-1.0, Math.min(1.0, value));
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
                    // Добавляем гармоники для богатства звука
                    for (int h = 2; h <= 5; h++) {
                        value += Math.sin(2 * Math.PI * t * h) / h;
                    }
                    break;
                case "noise":
                    value = 2 * random.nextDouble() - 1;
                    break;
            }

            // Применяем огибающую (ADSR)
            double envelope = 1.0;
            double notePosition = t % 1.0;
            if (notePosition < 0.05) {
                envelope = notePosition / 0.05; // Attack
            } else if (notePosition > 0.8) {
                envelope = 1.0 - (notePosition - 0.8) / 0.2; // Release
            }

            return value * amplitude * envelope;
        }
    }
}
