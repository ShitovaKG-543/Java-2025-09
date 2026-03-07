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

public class KaleidoscopeGeneratorWithoutJavaFX2 {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int DURATION_SECONDS = 180;
    private static final int FPS = 30;
    private static final int TOTAL_FRAMES = DURATION_SECONDS * FPS;

    private static final String OUTPUT_FILE =
            "kaleidoscope2_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mp4";

    private final Random random = new Random();
    private final int numberOfShapes = 8 + random.nextInt(10);

    public static void main(String[] args) {
        new KaleidoscopeGeneratorWithoutJavaFX2().generateVideo();
    }

    private void generateVideo() {
        System.out.println("=== ГЕНЕРАЦИЯ ВИДЕО С ПЛАВНОЙ СМЕНОЙ ФИГУР ===");
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

    private void drawHeart(Graphics2D g2d, double x, double y, double size, double morphFactor) {
        Path2D heart = new Path2D.Double();

        // Плавный переход от круга к сердцу
        if (morphFactor < 0.3) {
            // Почти круг
            double circleSize = size * (0.8 + morphFactor * 0.7);
            heart.append(new Ellipse2D.Double(x - circleSize / 2, y - circleSize / 2, circleSize, circleSize), false);
        } else if (morphFactor > 0.7) {
            // Почти сердце
            heart.moveTo(x, y - size / 3);
            heart.curveTo(x - size / 2, y - size, x - size, y + size / 3, x, y + size / 1.5);
            heart.curveTo(x + size, y + size / 3, x + size / 2, y - size, x, y - size / 3);
        } else {
            // Плавный переход
            double t = (morphFactor - 0.3) / 0.4;
            heart.moveTo(x, y - size / 3 * (1 - t * 0.3));
            heart.curveTo(
                    x - size / 2 * (1 + t),
                    y - size * (1 - t * 0.2),
                    x - size * (1 - t * 0.3),
                    y + size / 3 * (1 + t),
                    x,
                    y + size / 1.5 * (1 - t * 0.1));
            heart.curveTo(
                    x + size * (1 - t * 0.3),
                    y + size / 3 * (1 + t),
                    x + size / 2 * (1 + t),
                    y - size * (1 - t * 0.2),
                    x,
                    y - size / 3 * (1 - t * 0.3));
        }

        g2d.fill(heart);
    }

    private void drawCat(Graphics2D g2d, double x, double y, double size, double morphFactor) {
        Path2D cat = new Path2D.Double();

        // Голова (круг) - всегда есть
        cat.append(new Ellipse2D.Double(x - size / 2, y - size / 2, size, size), false);

        // Уши появляются плавно
        if (morphFactor > 0.2) {
            double earFactor = Math.min(1.0, (morphFactor - 0.2) / 0.8);

            // Левое ухо
            Path2D ear = new Path2D.Double();
            ear.moveTo(x - size / 2.5, y - size / 1.8);
            ear.lineTo(x - size / 1.8, y - size / 1.2 * earFactor);
            ear.lineTo(x - size / 3.5, y - size / 1.5);
            ear.closePath();
            cat.append(ear, false);

            // Правое ухо
            ear = new Path2D.Double();
            ear.moveTo(x + size / 2.5, y - size / 1.8);
            ear.lineTo(x + size / 1.8, y - size / 1.2 * earFactor);
            ear.lineTo(x + size / 3.5, y - size / 1.5);
            ear.closePath();
            cat.append(ear, false);
        }

        // Глаза появляются плавно
        if (morphFactor > 0.4) {
            double eyeFactor = Math.min(1.0, (morphFactor - 0.4) / 0.6);
            double eyeSize = size / 6 * eyeFactor;
            cat.append(new Ellipse2D.Double(x - size / 4, y - size / 4, eyeSize, eyeSize), false);
            cat.append(new Ellipse2D.Double(x + size / 4 - eyeSize, y - size / 4, eyeSize, eyeSize), false);
        }

        g2d.fill(cat);
    }

    private void drawStar(Graphics2D g2d, double x, double y, double size, int points, double morphFactor) {
        Path2D star = new Path2D.Double();
        double angle = Math.PI / points;

        // Плавное изменение остроты звезды
        double innerRadius = size / 4 * (1 - morphFactor * 0.3);
        double outerRadius = size / 2 * (0.7 + morphFactor * 0.3);

        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            double theta = i * angle + morphFactor * Math.PI / 8; // Плавное вращение

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

    private void drawButterfly(Graphics2D g2d, double x, double y, double size, double morphFactor) {
        Path2D butterfly = new Path2D.Double();

        // Тело
        butterfly.append(new Ellipse2D.Double(x - size / 8, y - size / 2, size / 4, size), false);

        // Крылья с плавным изменением размера
        double wingFactor = 0.5 + morphFactor * 0.5;

        // Левое крыло
        Path2D wing = new Path2D.Double();
        wing.moveTo(x, y - size / 3);
        wing.curveTo(x - size * wingFactor, y - size, x - size * wingFactor, y + size / 2, x, y + size / 3);
        wing.curveTo(x - size / 2, y + size / 4, x - size / 2, y - size / 4, x, y - size / 3);
        butterfly.append(wing, false);

        // Правое крыло
        wing = new Path2D.Double();
        wing.moveTo(x, y - size / 3);
        wing.curveTo(x + size * wingFactor, y - size, x + size * wingFactor, y + size / 2, x, y + size / 3);
        wing.curveTo(x + size / 2, y + size / 4, x + size / 2, y - size / 4, x, y - size / 3);
        butterfly.append(wing, false);

        g2d.fill(butterfly);
    }

    private void drawSpiral(Graphics2D g2d, double x, double y, double size, double turns, double morphFactor) {
        Path2D spiral = new Path2D.Double();

        double maxRadius = size / 2;
        int points = 100;

        // Плавное изменение плотности спирали
        double density = 1.0 + morphFactor * 0.5;

        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double radius = maxRadius * t;
            double angle = t * turns * 2 * Math.PI * density;

            double px = x + radius * Math.cos(angle);
            double py = y + radius * Math.sin(angle);

            if (i == 0) {
                spiral.moveTo(px, py);
            } else {
                spiral.lineTo(px, py);
            }
        }

        g2d.setStroke(new java.awt.BasicStroke((float) (size / 10 * (0.5 + morphFactor * 0.5))));
        g2d.draw(spiral);
    }

    private void drawMorphingShape(
            Graphics2D g2d,
            double x,
            double y,
            double size,
            double time,
            int shapeType1,
            int shapeType2,
            double blend) {
        // Рисуем смесь двух фигур
        if (blend < 0.1) {
            // Почти чистая первая фигура
            drawSingleShape(g2d, x, y, size, time, shapeType1, 1.0);
        } else if (blend > 0.9) {
            // Почти чистая вторая фигура
            drawSingleShape(g2d, x, y, size, time, shapeType2, 1.0);
        } else {
            // Плавный переход через прозрачность и масштабирование
            AffineTransform old = g2d.getTransform();

            // Первая фигура уменьшается и бледнеет
            g2d.scale(1.0 - blend * 0.3, 1.0 - blend * 0.3);
            Color color1 = new Color(
                    g2d.getColor().getRed(),
                    g2d.getColor().getGreen(),
                    g2d.getColor().getBlue(),
                    (int) (255 * (1.0 - blend)));
            g2d.setColor(color1);
            drawSingleShape(g2d, x, y, size, time, shapeType1, 1.0 - blend);

            g2d.setTransform(old);

            // Вторая фигура увеличивается и проявляется
            g2d.scale(0.7 + blend * 0.3, 0.7 + blend * 0.3);
            Color color2 = new Color(
                    g2d.getColor().getRed(),
                    g2d.getColor().getGreen(),
                    g2d.getColor().getBlue(),
                    (int) (255 * blend));
            g2d.setColor(color2);
            drawSingleShape(g2d, x, y, size, time, shapeType2, blend);

            g2d.setTransform(old);
        }
    }

    private void drawSingleShape(
            Graphics2D g2d, double x, double y, double size, double time, int shapeType, double alpha) {
        // Сохраняем текущий цвет с прозрачностью
        Color originalColor = g2d.getColor();
        Color transparentColor = new Color(
                originalColor.getRed(), originalColor.getGreen(), originalColor.getBlue(), (int) (255 * alpha));
        g2d.setColor(transparentColor);

        switch (shapeType) {
            case 0: // Сердечки
                drawHeart(g2d, x, y, size, 1.0);
                break;
            case 1: // Кошки
                drawCat(g2d, x, y, size, 1.0);
                break;
            case 2: // Звезды 5-конечные
                drawStar(g2d, x, y, size, 5, 1.0);
                break;
            case 3: // Бабочки
                drawButterfly(g2d, x, y, size, 1.0);
                break;
            case 4: // Спирали
                drawSpiral(g2d, x, y, size, 3, 1.0);
                break;
            case 5: // Цветы (8-конечные звезды)
                drawStar(g2d, x, y, size, 8, 1.0);
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
                g2d.rotate(-Math.PI / 4, x, y);
                break;
        }

        // Восстанавливаем цвет
        g2d.setColor(originalColor);
    }

    private void drawKaleidoscopeFrame(Graphics2D g2d, int frameNum) {
        // Черный фон
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        double time = frameNum / (double) FPS;
        double centerX = WIDTH / 2.0;
        double centerY = HEIGHT / 2.0;

        // Плавное изменение количества сегментов
        double segments = 6 + Math.sin(time * 0.2) * 3;
        double angleStep = 2 * Math.PI / segments;

        // Плавное изменение типа фигур (используем синусоиды для непрерывного перехода)
        double[] shapeWeights = new double[9];
        for (int i = 0; i < 9; i++) {
            shapeWeights[i] = 0.5 + 0.5 * Math.sin(time * 0.3 + i * 0.8);
        }

        // Нормализуем веса
        double sum = 0;
        for (double w : shapeWeights) sum += w;
        for (int i = 0; i < shapeWeights.length; i++) {
            shapeWeights[i] /= sum;
        }

        for (int i = 0; i < numberOfShapes; i++) {
            // Плавное изменение позиции
            double t = time * 0.15 + i * 0.6;
            double distance = 100 + Math.sin(t) * 80 + Math.cos(t * 1.2) * 60;
            double angle = i * 1.2 + time * 0.25 + Math.sin(time * 0.15 + i) * 0.4;

            // Плавное изменение размера
            double size = 70 + Math.sin(time * 0.6 + i * 2) * 30;

            // Плавное изменение цвета
            float hue = (float) ((time * 8 + i * 25) % 360) / 360.0f;
            float saturation = 0.8f + 0.2f * (float) Math.sin(time * 0.5 + i);
            float brightness = 0.8f + 0.2f * (float) Math.cos(time * 0.4 + i);

            Color color = Color.getHSBColor(hue, saturation, brightness);
            g2d.setColor(color);

            // Определяем две основные фигуры для этого момента времени
            double rand = (i * 0.1 + time * 0.03) % 1.0;
            double cumulative = 0;
            int shapeType1 = 0;
            int shapeType2 = 0;
            double blend = 0;

            for (int j = 0; j < shapeWeights.length; j++) {
                cumulative += shapeWeights[j];
                if (rand <= cumulative) {
                    shapeType1 = j;
                    // Вторая фигура - следующая по весу
                    shapeType2 = (j + 1) % shapeWeights.length;
                    // Степень смешивания зависит от близости к границе
                    double prevCumulative = cumulative - shapeWeights[j];
                    blend = (rand - prevCumulative) / shapeWeights[j];
                    break;
                }
            }

            for (int seg = 0; seg < (int) segments; seg++) {
                double segmentAngle = seg * angleStep + angle;

                // Плавные смещения
                double offsetX = Math.sin(segmentAngle * 2.5 + time * 1.5) * 40;
                double offsetY = Math.cos(segmentAngle * 2.5 + time * 1.5) * 40;

                int x = (int) (centerX + Math.cos(segmentAngle) * distance + offsetX);
                int y = (int) (centerY + Math.sin(segmentAngle) * distance + offsetY);

                // Сохраняем трансформацию
                AffineTransform old = g2d.getTransform();

                // Плавное вращение
                g2d.rotate(segmentAngle + time * 0.15, x, y);

                // Рисуем смесь двух фигур
                drawMorphingShape(g2d, x, y, size, time, shapeType1, shapeType2, blend);

                // Восстанавливаем трансформацию
                g2d.setTransform(old);
            }
        }

        // Плавные линии с изменяющейся прозрачностью
        for (int i = 0; i < 16; i++) {
            double lineAngle = i * Math.PI / 8 + time * 0.08;
            float hue = (float) ((time * 15 + i * 22.5) % 360) / 360.0f;

            // Плавное изменение прозрачности линий
            int alpha = (int) (100 + 55 * Math.sin(time * 0.4 + i));

            Color lineColor = Color.getHSBColor(hue, 0.7f, 0.8f);
            lineColor = new Color(
                    lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), Math.min(255, Math.max(0, alpha)));

            g2d.setColor(lineColor);
            g2d.setStroke(new java.awt.BasicStroke(1.5f + (float) Math.sin(time * 0.5 + i) * 1.2f));

            int x1 = (int) (centerX + Math.cos(lineAngle) * 80);
            int y1 = (int) (centerY + Math.sin(lineAngle) * 80);
            int x2 = (int) (centerX + Math.cos(lineAngle) * 380);
            int y2 = (int) (centerY + Math.sin(lineAngle) * 380);

            g2d.drawLine(x1, y1, x2, y2);
        }
    }
}
