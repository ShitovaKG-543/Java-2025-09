package ru.otus;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class KaleidoscopeGenerator extends Application {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180; // 3 минуты
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    // Уникальное имя файла с датой и временем
    private static final String OUTPUT_FILE =
            "kaleidoscope_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    // Случайные параметры для каждого запуска
    private final Random random = new Random(System.nanoTime()); // Разный seed при каждом запуске
    private final CountDownLatch latch = new CountDownLatch(1);

    // Параметры, которые будут случайными для каждого видео
    private final int baseSegments;
    private final double baseSpeed;
    private final double baseComplexity;
    private final double baseRotationSpeed;
    private final int numberOfShapes;
    private final Color backgroundColor;
    private final String patternType;
    private final double colorShiftSpeed;

    public KaleidoscopeGenerator() {
        // Генерируем случайные параметры для этого видео
        this.baseSegments = 4 + random.nextInt(8); // 4-12 сегментов
        this.baseSpeed = 0.2 + random.nextDouble() * 0.8; // 0.2-1.0
        this.baseComplexity = 5 + random.nextDouble() * 10; // 5-15
        this.baseRotationSpeed = 0.1 + random.nextDouble() * 0.5; // 0.1-0.6
        this.numberOfShapes = 30 + random.nextInt(70); // 30-100 фигур
        this.colorShiftSpeed = 0.1 + random.nextDouble() * 0.5; // 0.1-0.6

        // Случайный фоновый цвет
        this.backgroundColor = Color.hsb(
                random.nextDouble() * 360, random.nextDouble() * 0.3, random.nextDouble() * 0.2 + 0.1 // Темный фон
                );

        // Случайный тип узора
        String[] patterns = {"symmetrical", "spiral", "radial", "chaotic", "waves"};
        this.patternType = patterns[random.nextInt(patterns.length)];

        System.out.println("Параметры видео:");
        System.out.println("  Сегментов: " + baseSegments);
        System.out.println("  Скорость: " + String.format("%.2f", baseSpeed));
        System.out.println("  Сложность: " + String.format("%.2f", baseComplexity));
        System.out.println("  Тип узора: " + patternType);
        System.out.println("  Фигур: " + numberOfShapes);
        System.out.println("  Файл: " + OUTPUT_FILE);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Начинаем генерацию уникального видео калейдоскопа...");
        System.out.println("Разрешение: " + WIDTH + "x" + HEIGHT);
        System.out.println("Длительность: " + DURATION_SECONDS + " секунд");
        System.out.println("Всего кадров: " + TOTAL_FRAMES);

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Scene scene = new Scene(new Group(canvas));
        primaryStage.setScene(scene);

        // Запускаем генерацию видео
        new Thread(() -> generateVideo(gc)).start();

        try {
            latch.await(); // Ждем завершения генерации
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private void generateVideo(GraphicsContext gc) {
        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(OUTPUT_FILE, WIDTH, HEIGHT)) {
            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(FPS);
            recorder.setVideoBitrate(8000000);
            recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
            recorder.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            long startTime = System.currentTimeMillis();

            for (int frameNum = 0; frameNum < TOTAL_FRAMES; frameNum++) {
                // Рисуем кадр калейдоскопа с уникальными параметрами
                drawKaleidoscopeFrame(gc, frameNum);

                // Конвертируем Canvas в BufferedImage
                javafx.scene.image.WritableImage fxImage = gc.getCanvas().snapshot(null, null);
                BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);

                // Записываем кадр в видео
                Frame frame = converter.convert(bufferedImage);
                recorder.record(frame);

                // Прогресс
                if (frameNum % 300 == 0 && frameNum > 0) {
                    double percent = (frameNum * 100.0) / TOTAL_FRAMES;
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long estimatedTotal = (elapsedTime * TOTAL_FRAMES) / frameNum;
                    long remaining = estimatedTotal - elapsedTime;

                    System.out.printf(
                            "Прогресс: %.1f%% | Прошло: %d сек | Осталось: %d сек%n",
                            percent, elapsedTime / 1000, remaining / 1000);
                }
            }

            recorder.stop();

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("Генерация завершена за " + totalTime + " секунд!");
            System.out.println("Видео сохранено в: " + OUTPUT_FILE);

        } catch (Exception e) {
            System.err.println("Ошибка при генерации видео: " + e.getMessage());
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    private void drawKaleidoscopeFrame(GraphicsContext gc, int frameNum) {
        // Очищаем канвас с уникальным фоновым цветом
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Параметры, которые меняются со временем
        double time = frameNum / (double) FPS;

        // Используем базовые параметры, но добавляем вариации со временем
        double segments = baseSegments + Math.sin(time * 0.2) * 2;
        double angleStep = 2 * Math.PI / segments;

        // Центр калейдоскопа
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // Рисуем фигуры в зависимости от типа узора
        for (int i = 0; i < numberOfShapes; i++) {
            // Используем random для создания уникальной структуры
            // но с детерминированным поведением во времени
            int seed = i * 1000;
            Random shapeRandom = new Random(seed + frameNum / 10);

            double patternPhase = time * baseSpeed + i * 0.3;

            // Разные типы узоров
            double distance;
            double angle;

            switch (patternType) {
                case "spiral":
                    distance = 50 + time * 10 + i * 5;
                    angle = time * baseRotationSpeed + i * 0.5;
                    break;
                case "radial":
                    distance = 100 + Math.sin(time + i) * 80;
                    angle = i * 0.5 + time * baseRotationSpeed;
                    break;
                case "chaotic":
                    distance = 50 + shapeRandom.nextDouble() * 200;
                    angle = shapeRandom.nextDouble() * 2 * Math.PI + time;
                    break;
                case "waves":
                    distance = 150 + Math.sin(time * 2 + i) * 100;
                    angle = i * 0.3 + Math.cos(time * 0.5) * 2;
                    break;
                default: // symmetrical
                    distance = 80 + Math.sin(patternPhase * 2) * 50 + Math.cos(i * 0.3) * 30;
                    angle = i * 0.2 + time * baseRotationSpeed;
                    break;
            }

            // Динамический размер
            double size = 10 + Math.sin(patternPhase * 2 + i) * 15 + Math.cos(time + i) * 10;

            // Цвет меняется со временем
            double hue = (patternPhase * 30 + i * 20 + time * colorShiftSpeed * 50) % 360;
            double saturation = 0.5 + Math.sin(patternPhase * 3 + i) * 0.4;
            double brightness = 0.6 + Math.cos(patternPhase * 2 + i * 2) * 0.3;

            Color color = Color.hsb(hue, saturation, brightness);
            gc.setFill(color);

            // Рисуем в каждом сегменте для создания симметрии
            for (int seg = 0; seg < (int) segments; seg++) {
                double segmentAngle = seg * angleStep + angle;

                // Смещение для более сложного узора
                double offsetX = Math.sin(segmentAngle * baseComplexity + time) * 15;
                double offsetY = Math.cos(segmentAngle * baseComplexity + time * 1.3) * 15;

                double x = centerX + Math.cos(segmentAngle) * distance + offsetX;
                double y = centerY + Math.sin(segmentAngle) * distance + offsetY;

                // Разные формы для каждого видео
                int shapeType = (int) ((baseSegments + i + frameNum / 30) % 3);

                switch (shapeType) {
                    case 0:
                        // Круги
                        gc.fillOval(x - size / 2, y - size / 2, size, size);
                        break;
                    case 1:
                        // Квадраты
                        gc.fillRect(x - size / 2, y - size / 2, size, size);
                        break;
                    case 2:
                        // Треугольники
                        double[] xPoints = new double[3];
                        double[] yPoints = new double[3];
                        for (int p = 0; p < 3; p++) {
                            double pointAngle = p * 2 * Math.PI / 3 + time;
                            xPoints[p] = x + Math.cos(pointAngle) * size / 2;
                            yPoints[p] = y + Math.sin(pointAngle) * size / 2;
                        }
                        gc.fillPolygon(xPoints, yPoints, 3);
                        break;
                }
            }
        }

        // Добавляем декоративные линии с уникальным стилем
        gc.setLineWidth(1 + baseSpeed);
        for (int i = 0; i < baseSegments * 2; i++) {
            double lineAngle = i * 2 * Math.PI / (baseSegments * 2) + time * baseRotationSpeed * 0.5;

            double startX = centerX + Math.cos(lineAngle) * 50;
            double startY = centerY + Math.sin(lineAngle) * 50;
            double endX = centerX + Math.cos(lineAngle) * 300;
            double endY = centerY + Math.sin(lineAngle) * 300;

            gc.setStroke(Color.hsb(i * 30 + time * 50, 0.9, 0.8));
            gc.strokeLine(startX, startY, endX, endY);
        }
    }
}
