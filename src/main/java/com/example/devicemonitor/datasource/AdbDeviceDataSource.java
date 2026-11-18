package com.example.devicemonitor.datasource;

import com.example.devicemonitor.model.DeviceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Читает данные с телефона через adb (Android platform-tools).
 *
 * <p>Здесь только запуск процессов и сборка снимка. Весь разбор вывода вынесен
 * в {@link AdbOutputParser} — его можно тестировать без телефона и без adb.</p>
 */
public class AdbDeviceDataSource implements DeviceDataSource {

    private static final Logger log = LoggerFactory.getLogger(AdbDeviceDataSource.class);

    private final String adbPath;
    private final Duration timeout;

    public AdbDeviceDataSource(String adbPath, Duration timeout) {
        this.adbPath = adbPath;
        this.timeout = timeout;
    }

    @Override
    public DeviceSnapshot read() {
        try {
            String serial = AdbOutputParser.findDeviceSerial(run(List.of(adbPath, "devices")));
            if (serial == null) {
                return DeviceSnapshot.offline(
                        "Телефон не найден. Подключите кабель и разрешите отладку по USB.");
            }

            String batteryDump = shell(serial, "dumpsys", "battery");
            Integer level = AdbOutputParser.parseBatteryLevel(batteryDump);
            boolean charging = AdbOutputParser.isCharging(batteryDump);

            String manufacturer = prop(serial, "ro.product.manufacturer");
            String model = prop(serial, "ro.product.model");
            String android = prop(serial, "ro.build.version.release");
            String name = displayName(setting(serial, "global", "device_name"), manufacturer, model);

            AdbOutputParser.Storage storage =
                    AdbOutputParser.parseStorage(shell(serial, "df", "/data"));

            return new DeviceSnapshot(
                    true,
                    level,
                    charging,
                    name,
                    manufacturer,
                    model,
                    android,
                    storage == null ? null : roundGb(storage.totalGb()),
                    storage == null ? null : roundGb(storage.usedGb()),
                    storage == null ? null : roundGb(storage.freeGb()),
                    Instant.now(),
                    null);
        } catch (Exception e) {
            log.warn("Не удалось прочитать данные через adb: {}", e.getMessage());
            return DeviceSnapshot.offline("Ошибка adb: " + e.getMessage());
        }
    }

    /**
     * Снимок экрана через {@code adb exec-out screencap -p}.
     * stderr отбрасывается, чтобы служебные сообщения adb не попали в PNG.
     */
    @Override
    public byte[] screenshot() {
        try {
            String serial = AdbOutputParser.findDeviceSerial(run(List.of(adbPath, "devices")));
            if (serial == null) {
                return null;
            }
            CommandResult result = execute(new ProcessBuilder(
                    adbPath, "-s", serial, "exec-out", "screencap", "-p")
                    .redirectError(ProcessBuilder.Redirect.DISCARD));
            return result.exitCode() == 0 && result.output().length > 8 ? result.output() : null;
        } catch (Exception e) {
            log.warn("Не удалось снять экран: {}", e.getMessage());
            return null;
        }
    }

    private String shell(String serial, String... command) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(adbPath, "-s", serial, "shell"));
        args.addAll(List.of(command));
        return run(args);
    }

    private String prop(String serial, String property) {
        try {
            return shell(serial, "getprop", property).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String setting(String serial, String namespace, String key) {
        try {
            return shell(serial, "settings", "get", namespace, key).trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Имя из настроек; если производитель его не задал — собираем из модели.
     */
    private String displayName(String deviceName, String manufacturer, String model) {
        if (deviceName != null && !deviceName.isBlank() && !"null".equals(deviceName)) {
            return deviceName;
        }
        String fallback = ((manufacturer == null ? "" : manufacturer + " ")
                + (model == null ? "" : model)).trim();
        return fallback.isBlank() ? null : fallback;
    }

    private String run(List<String> command) throws IOException, InterruptedException {
        CommandResult result = execute(new ProcessBuilder(command).redirectErrorStream(true));
        String output = new String(result.output(), StandardCharsets.UTF_8);
        if (result.exitCode() != 0) {
            throw new IOException("adb завершился с кодом " + result.exitCode() + ": " + output.trim());
        }
        return output;
    }

    /**
     * Запускает процесс и параллельно читает stdout в виртуальном потоке.
     *
     * <p>Это важно для скриншота: PNG весит сотни килобайт, и если сначала ждать
     * завершения процесса, он упрётся в буфер pipe. Параллельное чтение убирает
     * и эту проблему, и риск зависнуть навсегда, если adb перестал отвечать.</p>
     */
    private CommandResult execute(ProcessBuilder builder) throws IOException, InterruptedException {
        Process process = builder.start();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> drain(process, buffer));

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            // не ждём читателя бесконечно: поток уже не нужен, а запрос не должен зависнуть
            reader.join(1000);
            throw new IOException("adb не ответил за " + timeout.toMillis() + " мс");
        }
        reader.join();
        return new CommandResult(buffer.toByteArray(), process.exitValue());
    }

    private static void drain(Process process, ByteArrayOutputStream buffer) {
        try (InputStream in = process.getInputStream()) {
            in.transferTo(buffer);
        } catch (IOException ignored) {
            // процесс завершили принудительно — читать больше нечего
        }
    }

    /** Сырой вывод команды и её код возврата. */
    private record CommandResult(byte[] output, int exitCode) {
    }

    private double roundGb(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
