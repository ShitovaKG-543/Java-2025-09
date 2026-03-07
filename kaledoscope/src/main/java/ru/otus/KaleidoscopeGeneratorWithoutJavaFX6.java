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

public class KaleidoscopeGeneratorWithoutJavaFX6 {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180;
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    private static final String OUTPUT_FILE =
            "kaleidoscope6_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    private final Random random = new Random();
    private final int numberOfShapes = 4 + random.nextInt(4); // 4-8 фигур, меньше для четкости

    // Класс для хранения информации о круге каждой фигуры
    private class ShapeCircle {
        double centerX, centerY; // Центр круга
        double radius; // Радиус круга
        double startAngle; // Начальный угол
        double speed; // Скорость движения
        double direction; // Направление (по часовой/против)

        ShapeCircle(double centerX, double centerY, double radius, double startAngle, double speed, double direction) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.startAngle = startAngle;
            this.speed = speed;
            this.direction = direction;
        }
    }

    private ShapeCircle[] circles; // Массив кругов для каждой фигуры

    public static void main(String[] args) {
        new KaleidoscopeGeneratorWithoutJavaFX6().generateVideo();
    }

    private void generateVideo() {
        System.out.println("=== ГЕНЕРАЦИЯ ВИДЕО С ФИГУРАМИ В КРУГАХ ===");
        System.out.println("Разрешение: " + WIDTH + "x" + HEIGHT);
        System.out.println("FPS: " + FPS);
        System.out.println("Длительность: " + DURATION_SECONDS + " сек");
        System.out.println("Фигур на кадр: " + numberOfShapes);
        System.out.println("Файл: " + OUTPUT_FILE);

        // Инициализируем круги для каждой фигуры
        circles = new ShapeCircle[numberOfShapes];
        for (int i = 0; i < numberOfShapes; i++) {
            // Распределяем круги равномерно по экрану
            double circleCenterX = 200 + random.nextDouble() * (WIDTH - 400);
            double circleCenterY = 150 + random.nextDouble() * (HEIGHT - 300);
            double circleRadius = 100 + random.nextDouble() * 150; // Радиус круга 100-250
            double startAngle = random.nextDouble() * 2 * Math.PI;
            double speed = 0.02 + random.nextDouble() * 0.04; // Медленная скорость
            double direction = random.nextBoolean() ? 1.0 : -1.0;

            circles[i] = new ShapeCircle(circleCenterX, circleCenterY, circleRadius, startAngle, speed, direction);
        }

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

        g2d.setStroke(new java.awt.BasicStroke((float) size / 8));
        g2d.draw(spiral);
    }

    private void drawKaleidoscopeFrame(Graphics2D g2d, int frameNum) {
        // Черный фон
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        double time = frameNum / (double) FPS;

        // Рисуем невидимые круги-ограничители (для отладки можно раскомментировать)
        // g2d.setColor(new Color(40, 40, 40));
        // g2d.setStroke(new java.awt.BasicStroke(1));
        // for (ShapeCircle circle : circles) {
        //     g2d.draw(new Ellipse2D.Double(circle.centerX - circle.radius, circle.centerY - circle.radius,
        //                                   circle.radius * 2, circle.radius * 2));
        // }

        // Для каждой фигуры свой круг
        for (int shapeIdx = 0; shapeIdx < numberOfShapes; shapeIdx++) {
            ShapeCircle circle = circles[shapeIdx];

            // Параметры движения внутри круга
            double t = time * circle.speed;

            // Движение от центра к краю и обратно (синусоида)
            // 0 = центр, 1 = край
            double radiusFactor = 0.5 + 0.5 * Math.sin(t * 2 * Math.PI);

            // Угол внутри круга (вращение)
            double angle = circle.startAngle + t * circle.direction * 2 * Math.PI;

            // Позиция фигуры внутри круга
            double distanceFromCenter = circle.radius * radiusFactor;
            double x = circle.centerX + Math.cos(angle) * distanceFromCenter;
            double y = circle.centerY + Math.sin(angle) * distanceFromCenter;

            // Размер фигуры (пульсирует)
            double size = 40 + Math.sin(t * 3) * 15 + shapeIdx * 5;
            size = Math.max(30, Math.min(100, size));

            // Цвет меняется со временем
            float hue = (float) ((time * 5 + shapeIdx * 40) % 360) / 360.0f;
            float saturation = 0.8f + 0.2f * (float) Math.sin(time * 0.3 + shapeIdx);
            float brightness = 0.8f + 0.2f * (float) Math.cos(time * 0.4 + shapeIdx);

            Color color = Color.getHSBColor(hue, saturation, brightness);
            g2d.setColor(color);

            // Тип фигуры
            int shapeType = (int) ((time * 0.5 + shapeIdx * 2.5) % 9);

            // Сохраняем трансформацию
            AffineTransform old = g2d.getTransform();

            // Небольшое вращение фигуры
            g2d.rotate(time * 0.2 + shapeIdx, x, y);

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

        // Добавляем легкие линии для связи кругов
        g2d.setStroke(new java.awt.BasicStroke(1));
        g2d.setColor(new Color(40, 40, 40, 100));

        for (int i = 0; i < circles.length; i++) {
            for (int j = i + 1; j < circles.length; j++) {
                // Иногда рисуем линии между кругами
                if (Math.sin(time * 0.5 + i * j) > 0.7) {
                    g2d.drawLine((int) circles[i].centerX, (int) circles[i].centerY, (int) circles[j].centerX, (int)
                            circles[j].centerY);
                }
            }
        }
    }
}
