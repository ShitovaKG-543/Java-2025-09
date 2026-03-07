package ru.otus;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

public class KaleidoscopeGeneratorWithoutJavaFX3 {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180;
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    private static final String OUTPUT_FILE =
            "kaleidoscope3_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    private final Random random = new Random();
    private final int numberOfShapes = 12 + random.nextInt(8); // Больше фигур для заполнения

    public static void main(String[] args) {
        new KaleidoscopeGeneratorWithoutJavaFX3().generateVideo();
    }

    private void generateVideo() {
        System.out.println("=== ГЕНЕРАЦИЯ ВИДЕО С ПЛАВНЫМ ПЕРЕМЕЩЕНИЕМ ФИГУР ===");
        System.out.println("Разрешение: " + WIDTH + "x" + HEIGHT);
        System.out.println("FPS: " + FPS);
        System.out.println("Длительность: " + DURATION_SECONDS + " сек");
        System.out.println("Фигур на кадр: " + numberOfShapes);
        System.out.println("Файл: " + OUTPUT_FILE);

        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(OUTPUT_FILE, WIDTH, HEIGHT)) {
            recorder.setVideoCodec(AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setFrameRate(FPS);
            recorder.setVideoBitrate(8000000);
            recorder.setPixelFormat(AV_PIX_FMT_YUV420P);
            recorder.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();

            long startTime = System.currentTimeMillis();

            for (int frameNum = 0; frameNum < TOTAL_FRAMES; frameNum++) {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
                Graphics2D g2d = image.createGraphics();

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                drawKaleidoscopeFrame(g2d, frameNum);

                g2d.dispose();

                Frame frame = converter.convert(image);
                recorder.record(frame);

                if (frameNum % 30 == 0) {
                    double percent = (frameNum * 100.0) / TOTAL_FRAMES;
                    System.out.printf("Прогресс: %.1f%%%n", percent);
                }
            }

            recorder.stop();

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("Генерация завершена за " + totalTime + " секунд!");
            System.out.println("Видео сохранено в: " + OUTPUT_FILE);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawHeart(Graphics2D g2d, double x, double y, double size) {
        Path2D heart = new Path2D.Double();
        heart.moveTo(x, y - size / 3);
        heart.curveTo(x - size / 2, y - size, x - size, y + size / 3, x, y + size / 1.5);
        heart.curveTo(x + size, y + size / 3, x + size / 2, y - size, x, y - size / 3);
        g2d.fill(heart);
    }

    private void drawCat(Graphics2D g2d, double x, double y, double size) {
        Path2D cat = new Path2D.Double();
        cat.append(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size), false);

        Path2D ear = new Path2D.Double();
        ear.moveTo(x - size / 2.5, y - size / 1.8);
        ear.lineTo(x - size / 1.8, y - size / 1.2);
        ear.lineTo(x - size / 3.5, y - size / 1.5);
        ear.closePath();
        cat.append(ear, false);

        ear = new Path2D.Double();
        ear.moveTo(x + size / 2.5, y - size / 1.8);
        ear.lineTo(x + size / 1.8, y - size / 1.2);
        ear.lineTo(x + size / 3.5, y - size / 1.5);
        ear.closePath();
        cat.append(ear, false);

        cat.append(new Ellipse2D.Double(x - size / 4, y - size / 4, size / 6, size / 6), false);
        cat.append(new Ellipse2D.Double(x + size / 4 - size / 6, y - size / 4, size / 6, size / 6), false);

        g2d.fill(cat);
    }

    private void drawStar(Graphics2D g2d, double x, double y, double size, int points) {
        Path2D star = new Path2D.Double();
        double angle = Math.PI / points;

        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? size / 2 : size / 4;
            double theta = i * angle;
            double px = x + r * Math.sin(theta);
            double py = y - r * Math.cos(theta);

            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        g2d.fill(star);
    }

    private void drawButterfly(Graphics2D g2d, double x, double y, double size) {
        Path2D butterfly = new Path2D.Double();
        butterfly.append(new Ellipse2D.Double(x - size / 8, y - size / 2, size / 4, size), false);

        Path2D wing = new Path2D.Double();
        wing.moveTo(x, y - size / 3);
        wing.curveTo(x - size, y - size, x - size, y + size / 2, x, y + size / 3);
        wing.curveTo(x - size / 2, y + size / 4, x - size / 2, y - size / 4, x, y - size / 3);
        butterfly.append(wing, false);

        wing = new Path2D.Double();
        wing.moveTo(x, y - size / 3);
        wing.curveTo(x + size, y - size, x + size, y + size / 2, x, y + size / 3);
        wing.curveTo(x + size / 2, y + size / 4, x + size / 2, y - size / 4, x, y - size / 3);
        butterfly.append(wing, false);

        g2d.fill(butterfly);
    }

    private void drawSpiral(Graphics2D g2d, double x, double y, double size, double turns) {
        Path2D spiral = new Path2D.Double();
        double maxRadius = size / 2;
        int points = 100;

        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double radius = maxRadius * t;
            double angle = t * turns * 2 * Math.PI;
            double px = x + radius * Math.cos(angle);
            double py = y + radius * Math.sin(angle);

            if (i == 0) {
                spiral.moveTo(px, py);
            } else {
                spiral.lineTo(px, py);
            }
        }

        g2d.setStroke(new java.awt.BasicStroke((float) size / 10));
        g2d.draw(spiral);
    }

    private void drawKaleidoscopeFrame(Graphics2D g2d, int frameNum) {
        // Черный фон
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        double time = frameNum / (double) FPS;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // Плавное изменение количества сегментов
        double segments = 5 + Math.sin(time * 0.15) * 3;
        double angleStep = 2 * Math.PI / segments;

        // Массивы для хранения плавных параметров каждой фигуры
        double[] baseDistances = new double[numberOfShapes];
        double[] baseAngles = new double[numberOfShapes];
        double[] baseSizes = new double[numberOfShapes];
        double[] speeds = new double[numberOfShapes];
        double[] phases = new double[numberOfShapes];

        // Инициализация при первом кадре
        if (frameNum == 0) {
            for (int i = 0; i < numberOfShapes; i++) {
                baseDistances[i] = 50 + random.nextDouble() * 300; // Разные расстояния
                baseAngles[i] = random.nextDouble() * 2 * Math.PI; // Случайные углы
                baseSizes[i] = 50 + random.nextDouble() * 100; // Разные размеры
                speeds[i] = 0.1 + random.nextDouble() * 0.4; // Разные скорости
                phases[i] = random.nextDouble() * 2 * Math.PI; // Случайные фазы
            }
        }

        // Заполняем все пространство фигурами
        for (int i = 0; i < numberOfShapes; i++) {
            // Плавное движение по спирали и волнам
            double t = time * 0.2 + i * 0.3;

            // Динамическое расстояние от центра с несколькими волнами
            double wave1 = Math.sin(t * 2 + phases[i]) * 80;
            double wave2 = Math.cos(t * 1.3 + i) * 60;
            double wave3 = Math.sin(t * 0.7 + i * 2) * 100;
            double distance = baseDistances[i] + wave1 + wave2 + wave3;

            // Ограничиваем расстояние, чтобы фигуры были видны
            distance = Math.max(50, Math.min(WIDTH / 1.5, distance));

            // Плавное изменение угла с разными скоростями
            double angle = baseAngles[i] + time * speeds[i] + Math.sin(time * 0.3 + i) * 0.5;

            // Плавное изменение размера
            double size = baseSizes[i] + Math.sin(time * 0.5 + i * 2) * 30 + Math.cos(time * 0.8 + i) * 20;
            size = Math.max(30, Math.min(200, size)); // Ограничиваем размер

            // Плавное изменение цвета
            float hue = (float) ((time * 8 + i * 25 + Math.sin(time * 0.3) * 30) % 360) / 360.0f;
            float saturation = 0.7f + 0.3f * (float) Math.sin(time * 0.4 + i);
            float brightness = 0.8f + 0.2f * (float) Math.cos(time * 0.5 + i);

            Color color = Color.getHSBColor(hue, saturation, brightness);
            g2d.setColor(color);

            // Тип фигуры тоже меняется плавно (но для простоты оставим случайным)
            int shapeType = (int) ((time * 0.8 + i * 2.3) % 9);

            // Создаем несколько копий в каждом сегменте для симметрии
            for (int seg = 0; seg < (int) segments; seg++) {
                double segmentAngle = seg * angleStep + angle;

                // Плавные смещения для создания сложных узоров
                double offsetX = Math.sin(segmentAngle * 2 + time * 1.5) * 40 + Math.cos(time * 1.2 + i) * 30;
                double offsetY = Math.cos(segmentAngle * 2 + time * 1.5) * 40 + Math.sin(time * 1.2 + i) * 30;

                // Вычисляем позицию
                int x = (int) (centerX + Math.cos(segmentAngle) * distance + offsetX);
                int y = (int) (centerY + Math.sin(segmentAngle) * distance + offsetY);

                // Убеждаемся, что фигура в пределах экрана
                x = Math.max(20, Math.min(WIDTH - 20, x));
                y = Math.max(20, Math.min(HEIGHT - 20, y));

                // Сохраняем трансформацию
                AffineTransform old = g2d.getTransform();

                // Плавное вращение
                g2d.rotate(segmentAngle + time * 0.2, x, y);

                // Рисуем фигуру
                switch (shapeType) {
                    case 0: // Сердечки
                        drawHeart(g2d, x, y, size);
                        break;
                    case 1: // Кошки
                        drawCat(g2d, x, y, size);
                        break;
                    case 2: // Звезды 5-конечные
                        drawStar(g2d, x, y, size, 5);
                        break;
                    case 3: // Бабочки
                        drawButterfly(g2d, x, y, size);
                        break;
                    case 4: // Спирали
                        drawSpiral(g2d, x, y, size, 3);
                        break;
                    case 5: // Цветы (звезды 8-конечные)
                        drawStar(g2d, x, y, size, 8);
                        break;
                    case 6: // Квадраты
                        g2d.fill(new Rectangle2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                    case 7: // Круги
                        g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                    case 8: // Ромбы
                        g2d.rotate(Math.PI / 4, x, y);
                        g2d.fill(new Rectangle2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                }

                // Восстанавливаем трансформацию
                g2d.setTransform(old);
            }
        }

        // Добавляем плавные линии, заполняющие пространство
        g2d.setStroke(new java.awt.BasicStroke(1.5f));

        for (int i = 0; i < segments * 3; i++) {
            double lineAngle = i * Math.PI / (segments * 1.5) + time * 0.05;
            float hue = (float) ((time * 12 + i * 15) % 360) / 360.0f;

            // Плавное изменение прозрачности линий
            int alpha = (int) (100 + 55 * Math.sin(time * 0.3 + i));
            Color lineColor = Color.getHSBColor(hue, 0.6f, 0.7f);
            lineColor = new Color(
                    lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), Math.min(255, Math.max(0, alpha)));
            g2d.setColor(lineColor);

            // Линии разной длины, заполняющие всё пространство
            int x1 = (int) (centerX + Math.cos(lineAngle) * 50);
            int y1 = (int) (centerY + Math.sin(lineAngle) * 50);
            int x2 = (int) (centerX + Math.cos(lineAngle) * 450);
            int y2 = (int) (centerY + Math.sin(lineAngle) * 450);

            g2d.drawLine(x1, y1, x2, y2);
        }

        // Добавляем мелкие точки для заполнения пространства
        g2d.setColor(new Color(255, 255, 255, 30));
        for (int i = 0; i < 50; i++) {
            int x = (int) (centerX + Math.cos(time * 0.5 + i) * 350);
            int y = (int) (centerY + Math.sin(time * 0.5 + i) * 350);
            g2d.fill(new Ellipse2D.Double(x, y, 3, 3));
        }
    }
}
