package com.example.devicemonitor.datasource;

import com.example.devicemonitor.model.DeviceSnapshot;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Фейковый источник — приложение работает без телефона.
 * Удобно для демонстрации на чужой машине и для тестов.
 */
public class MockDeviceDataSource implements DeviceDataSource {

    private volatile int battery = ThreadLocalRandom.current().nextInt(60, 95);
    private volatile boolean charging = false;

    @Override
    public DeviceSnapshot read() {
        if (charging) {
            battery = Math.min(100, battery + 1);
        } else {
            battery = Math.max(1, battery - 1);
        }
        charging = ThreadLocalRandom.current().nextInt(100) < 30;

        double total = 128.0;
        double free = 40.0 + ThreadLocalRandom.current().nextInt(0, 20) / 10.0;

        return new DeviceSnapshot(
                true,
                battery,
                charging,
                "Pixel 7 (демо)",
                "Google",
                "Pixel 7",
                "15",
                total,
                Math.round((total - free) * 10.0) / 10.0,
                Math.round(free * 10.0) / 10.0,
                Instant.now(),
                null);
    }

    /** Рисует заглушку вместо реального скриншота. */
    @Override
    public byte[] screenshot() {
        int width = 480;
        int height = 900;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x101319));
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(0x1B2233));
            g.fillRoundRect(40, 120, width - 80, 260, 28, 28);

            g.setColor(new Color(0x3DDC84));
            g.setFont(new Font("Segoe UI", Font.BOLD, 64));
            g.drawString(battery + "%", 80, 250);

            g.setColor(new Color(0x8B93A7));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 26));
            g.drawString("DEMO SCREEN", 80, 310);

            g.setColor(new Color(0x262B36));
            g.fillRoundRect(40, 420, width - 80, 120, 20, 20);
            g.fillRoundRect(40, 570, width - 80, 120, 20, 20);

            g.setColor(new Color(0x4D8DFF));
            g.setFont(new Font("Segoe UI", Font.PLAIN, 22));
            g.drawString("Это заглушка: телефон не подключён", 60, 840);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            g.dispose();
        }
    }
}
