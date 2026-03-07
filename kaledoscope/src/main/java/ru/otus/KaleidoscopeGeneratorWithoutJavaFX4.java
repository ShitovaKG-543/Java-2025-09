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

public class KaleidoscopeGeneratorWithoutJavaFX4 {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180;
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    private static final String OUTPUT_FILE =
            "kaleidoscope4_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    private final Random random = new Random();
    // Уменьшаем количество фигур, чтобы они были крупнее
    private final int numberOfShapes = 6 + random.nextInt(6); // 6-12 фигур (было 8-18)

    public static void main(String[] args) {
        new KaleidoscopeGeneratorWithoutJavaFX4().generateVideo();
    }

    private void generateVideo() {
        System.out.println("=== ГЕНЕРАЦИЯ ВИДЕО С КРУПНЫМИ ФИГУРАМИ ===");
        System.out.println("Разрешение: " + WIDTH + "x" + HEIGHT);
        System.out.println("FPS: " + FPS);
        System.out.println("Длительность: " + DURATION_SECONDS + " сек");
        System.out.println("Фигур на кадр: " + numberOfShapes + " (крупные)");
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

        g2d.setStroke(new java.awt.BasicStroke((float) size / 8)); // Более толстая линия
        g2d.draw(spiral);
    }

    private void drawKaleidoscopeFrame(Graphics2D g2d, int frameNum) {
        // Черный фон
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        double time = frameNum / (double) FPS;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // Меньше сегментов для более крупных фигур
        double segments = 4 + Math.sin(time * 0.15) * 2; // 2-6 сегментов (было 5-8)
        double angleStep = 2 * Math.PI / segments;

        // Массивы для хранения плавных параметров каждой фигуры
        double[] baseDistances = new double[numberOfShapes];
        double[] baseAngles = new double[numberOfShapes];
        double[] baseSizes = new double[numberOfShapes];
        double[] speeds = new double[numberOfShapes];
        double[] phases = new double[numberOfShapes];

        // Инициализация при первом кадре с более крупными размерами
        if (frameNum == 0) {
            for (int i = 0; i < numberOfShapes; i++) {
                baseDistances[i] = 80 + random.nextDouble() * 250; // Чуть ближе к центру
                baseAngles[i] = random.nextDouble() * 2 * Math.PI;
                // УВЕЛИЧИВАЕМ РАЗМЕР: базовый размер от 100 до 250 (было 50-150)
                baseSizes[i] = 100 + random.nextDouble() * 150;
                speeds[i] = 0.05 + random.nextDouble() * 0.2; // Медленнее движение
                phases[i] = random.nextDouble() * 2 * Math.PI;
            }
        }

        // Заполняем пространство крупными фигурами
        for (int i = 0; i < numberOfShapes; i++) {
            double t = time * 0.15 + i * 0.2; // Медленнее изменение

            // Динамическое расстояние от центра
            double wave1 = Math.sin(t * 1.5 + phases[i]) * 60;
            double wave2 = Math.cos(t * 1.0 + i) * 50;
            double distance = baseDistances[i] + wave1 + wave2;

            // Ограничиваем расстояние, чтобы крупные фигуры были видны
            distance = Math.max(80, Math.min(WIDTH / 2.2, distance));

            // Плавное изменение угла
            double angle = baseAngles[i] + time * speeds[i] + Math.sin(time * 0.2 + i) * 0.3;

            // УВЕЛИЧИВАЕМ РАЗМЕР: динамическое изменение размера
            double size = baseSizes[i] + Math.sin(time * 0.3 + i * 2) * 40 + Math.cos(time * 0.5 + i) * 30;
            size = Math.max(70, Math.min(300, size)); // Ограничиваем, но крупнее (было 30-200)

            // Яркие цвета
            float hue = (float) ((time * 6 + i * 30 + Math.sin(time * 0.2) * 40) % 360) / 360.0f;
            float saturation = 0.8f + 0.2f * (float) Math.sin(time * 0.3 + i);
            float brightness = 0.9f + 0.1f * (float) Math.cos(time * 0.4 + i);

            Color color = Color.getHSBColor(hue, saturation, brightness);
            g2d.setColor(color);

            // Тип фигуры меняется медленнее
            int shapeType = (int) ((time * 0.5 + i * 2.5) % 9);

            // Создаем несколько копий в каждом сегменте
            for (int seg = 0; seg < (int) segments; seg++) {
                double segmentAngle = seg * angleStep + angle;

                // Смещения для крупных фигур делаем меньше, чтобы они не разлетались
                double offsetX = Math.sin(segmentAngle * 2 + time * 1.0) * 30 + Math.cos(time * 0.8 + i) * 20;
                double offsetY = Math.cos(segmentAngle * 2 + time * 1.0) * 30 + Math.sin(time * 0.8 + i) * 20;

                // Вычисляем позицию
                int x = (int) (centerX + Math.cos(segmentAngle) * distance + offsetX);
                int y = (int) (centerY + Math.sin(segmentAngle) * distance + offsetY);

                // Убеждаемся, что крупная фигура в пределах экрана
                x = Math.max((int) size / 2, Math.min(WIDTH - (int) size / 2, x));
                y = Math.max((int) size / 2, Math.min(HEIGHT - (int) size / 2, y));

                // Сохраняем трансформацию
                AffineTransform old = g2d.getTransform();

                // Плавное вращение
                g2d.rotate(segmentAngle + time * 0.15, x, y);

                // Рисуем крупную фигуру
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

        // Добавляем плавные линии, но более толстые для крупных фигур
        g2d.setStroke(new java.awt.BasicStroke(2.0f));

        for (int i = 0; i < segments * 2; i++) {
            double lineAngle = i * Math.PI / (segments) + time * 0.04;
            float hue = (float) ((time * 10 + i * 20) % 360) / 360.0f;

            int alpha = (int) (80 + 40 * Math.sin(time * 0.2 + i));
            Color lineColor = Color.getHSBColor(hue, 0.5f, 0.6f);
            lineColor = new Color(
                    lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), Math.min(255, Math.max(0, alpha)));
            g2d.setColor(lineColor);

            int x1 = (int) (centerX + Math.cos(lineAngle) * 80);
            int y1 = (int) (centerY + Math.sin(lineAngle) * 80);
            int x2 = (int) (centerX + Math.cos(lineAngle) * 420);
            int y2 = (int) (centerY + Math.sin(lineAngle) * 420);

            g2d.drawLine(x1, y1, x2, y2);
        }

        // Меньше точек, чтобы не загромождать пространство
        g2d.setColor(new Color(255, 255, 255, 20));
        for (int i = 0; i < 30; i++) {
            int x = (int) (centerX + Math.cos(time * 0.3 + i) * 300);
            int y = (int) (centerY + Math.sin(time * 0.3 + i) * 300);
            g2d.fill(new Ellipse2D.Double(x, y, 4, 4));
        }
    }
}
