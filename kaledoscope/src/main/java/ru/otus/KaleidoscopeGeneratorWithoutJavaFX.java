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

public class KaleidoscopeGeneratorWithoutJavaFX {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180;
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    private static final String OUTPUT_FILE =
            "kaleidoscope1_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    private final Random random = new Random();
    private final int numberOfShapes = 8 + random.nextInt(10); // Меньше фигур, но они заполняют экран

    public static void main(String[] args) {
        new KaleidoscopeGeneratorWithoutJavaFX().generateVideo();
    }

    private void generateVideo() {
        System.out.println("=== ГЕНЕРАЦИЯ ВИДЕО С НЕСТАНДАРТНЫМИ ФИГУРАМИ ===");
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

        // Рисуем сердечко с помощью кривых Безье
        heart.moveTo(x, y - size / 3);
        heart.curveTo(x - size / 2, y - size, x - size, y + size / 3, x, y + size / 1.5);
        heart.curveTo(x + size, y + size / 3, x + size / 2, y - size, x, y - size / 3);

        g2d.fill(heart);
    }

    private void drawCat(Graphics2D g2d, double x, double y, double size) {
        // Рисуем силуэт кота
        Path2D cat = new Path2D.Double();

        // Голова (круг)
        cat.append(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size), false);

        // Уши
        Path2D ear = new Path2D.Double();
        // Левое ухо
        ear.moveTo(x - size / 2.5, y - size / 1.8);
        ear.lineTo(x - size / 1.8, y - size / 1.2);
        ear.lineTo(x - size / 3.5, y - size / 1.5);
        ear.closePath();
        cat.append(ear, false);

        // Правое ухо
        ear = new Path2D.Double();
        ear.moveTo(x + size / 2.5, y - size / 1.8);
        ear.lineTo(x + size / 1.8, y - size / 1.2);
        ear.lineTo(x + size / 3.5, y - size / 1.5);
        ear.closePath();
        cat.append(ear, false);

        // Глаза (маленькие кружки)
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

        // Тело
        butterfly.append(new Ellipse2D.Double(x - size / 8, y - size / 2, size / 4, size), false);

        // Левое крыло
        Path2D wing = new Path2D.Double();
        wing.moveTo(x, y - size / 3);
        wing.curveTo(x - size, y - size, x - size, y + size / 2, x, y + size / 3);
        wing.curveTo(x - size / 2, y + size / 4, x - size / 2, y - size / 4, x, y - size / 3);
        butterfly.append(wing, false);

        // Правое крыло
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

        // Динамическое количество сегментов
        int segments = 6 + (int) (Math.sin(time * 0.3) * 3);
        double angleStep = 2 * Math.PI / segments;

        // Фигуры заполняют весь экран
        for (int i = 0; i < numberOfShapes; i++) {
            // Фигуры распределяются по всему экрану
            double distance = 50 + (i * 70) % (WIDTH / 2); // Равномерное распределение
            double angle = i * 1.5 + time * 0.5;

            // Размер пропорционален расстоянию от центра
            int size = 80 + (int) (Math.sin(time * 2 + i) * 30);

            // Яркие цвета
            float hue = (float) ((time * 20 + i * 30) % 360) / 360.0f;
            Color color = Color.getHSBColor(hue, 0.9f, 0.9f);
            g2d.setColor(color);

            // Выбираем тип фигуры (включая нестандартные)
            int shapeType = (int) ((time * 5 + i * 3) % 9); // 9 разных типов

            for (int seg = 0; seg < segments; seg++) {
                double segmentAngle = seg * angleStep + angle;

                // Случайное смещение для создания сложных узоров
                double offsetX = Math.sin(segmentAngle * 3 + time * 2) * 50;
                double offsetY = Math.cos(segmentAngle * 3 + time * 2) * 50;

                int x = (int) (centerX + Math.cos(segmentAngle) * distance + offsetX);
                int y = (int) (centerY + Math.sin(segmentAngle) * distance + offsetY);

                // Сохраняем трансформацию
                AffineTransform old = g2d.getTransform();

                // Поворачиваем фигуру для большей динамики
                g2d.rotate(segmentAngle + time, x, y);

                switch (shapeType) {
                    case 0: // Сердечки
                        drawHeart(g2d, x, y, size);
                        break;
                    case 1: // Кошки
                        drawCat(g2d, x, y, size);
                        break;
                    case 2: // Звезды
                        drawStar(g2d, x, y, size, 5);
                        break;
                    case 3: // Бабочки
                        drawButterfly(g2d, x, y, size);
                        break;
                    case 4: // Спирали
                        drawSpiral(g2d, x, y, size, 3);
                        break;
                    case 5: // Цветы (простая звезда с 8 лучами)
                        drawStar(g2d, x, y, size, 8);
                        break;
                    case 6: // Квадраты
                        g2d.fill(new Rectangle2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                    case 7: // Круги
                        g2d.fill(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                    case 8: // Ромбы (повернутые квадраты)
                        g2d.rotate(Math.PI / 4, x, y);
                        g2d.fill(new Rectangle2D.Double(x - size / 2, y - size / 2, size, size));
                        break;
                }

                // Восстанавливаем трансформацию
                g2d.setTransform(old);
            }
        }

        // Добавляем текстурные элементы
        g2d.setStroke(new java.awt.BasicStroke(2));

        for (int i = 0; i < segments * 2; i++) {
            double lineAngle = i * Math.PI / segments + time * 0.1;
            float hue = (float) ((time * 30 + i * 20) % 360) / 360.0f;
            g2d.setColor(Color.getHSBColor(hue, 0.8f, 0.8f));

            int x1 = (int) (centerX + Math.cos(lineAngle) * 100);
            int y1 = (int) (centerY + Math.sin(lineAngle) * 100);
            int x2 = (int) (centerX + Math.cos(lineAngle) * 400);
            int y2 = (int) (centerY + Math.sin(lineAngle) * 400);

            g2d.drawLine(x1, y1, x2, y2);
        }
    }
}
