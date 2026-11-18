package com.example.devicemonitor.datasource;

/**
 * Разбор текстового вывода adb.
 *
 * <p>Класс намеренно не зависит ни от Spring, ни от {@link Process}: на вход — строка,
 * на выход — значение. Поэтому все «грабли» (заголовки таблиц, лишние строки,
 * нечисловые значения) закрыты обычными юнит-тестами, без запуска телефона.</p>
 */
public final class AdbOutputParser {

    private AdbOutputParser() {
    }

    /**
     * Возвращает серийник первого авторизованного устройства.
     *
     * <pre>
     * List of devices attached
     * emulator-5554   offline
     * R58M12345       device
     * </pre>
     *
     * @return серийник или {@code null}, если ни одно устройство не готово к работе
     */
    public static String findDeviceSerial(String devicesOutput) {
        if (devicesOutput == null) {
            return null;
        }
        for (String line : devicesOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("List of devices") || trimmed.startsWith("*")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            // parts[1] — состояние: device, offline, unauthorized, ...
            if (parts.length >= 2 && "device".equals(parts[1])) {
                return parts[0];
            }
        }
        return null;
    }

    /**
     * Парсит {@code level: 87} из {@code dumpsys battery}.
     *
     * @return процент заряда или {@code null}, если строки нет или она битая
     */
    public static Integer parseBatteryLevel(String batteryDump) {
        return parseInt(valueAfter(batteryDump, "level:"));
    }

    /**
     * {@code status: 2} — заряжается, {@code status: 5} — заряжен.
     * Остальные значения (1, 3, 4) означают «не заряжается».
     */
    public static boolean isCharging(String batteryDump) {
        Integer status = parseInt(valueAfter(batteryDump, "status:"));
        return status != null && (status == 2 || status == 5);
    }

    /**
     * Разбирает вывод {@code df /data}. Размеры в 1K-блоках:
     *
     * <pre>
     * Filesystem     1K-blocks     Used Available Use% Mounted on
     * /dev/block/dm-52 49767424 27366344  22270008  56% /data
     * </pre>
     *
     * @return размеры в гибибайтах или {@code null}, если подходящей строки нет
     */
    public static Storage parseStorage(String dfOutput) {
        if (dfOutput == null) {
            return null;
        }
        String[] lines = dfOutput.split("\\R");
        // Идём с конца: некоторые прошивки печатают несколько строк,
        // актуальная — последняя.
        for (int i = lines.length - 1; i >= 0; i--) {
            String[] parts = lines[i].trim().split("\\s+");
            if (parts.length < 4) {
                continue;
            }
            try {
                long totalKb = Long.parseLong(parts[1]);
                long usedKb = Long.parseLong(parts[2]);
                long freeKb = Long.parseLong(parts[3]);
                if (totalKb <= 0) {
                    continue;
                }
                return new Storage(toGb(totalKb), toGb(usedKb), toGb(freeKb));
            } catch (NumberFormatException ignored) {
                // заголовок таблицы или строка без чисел — пропускаем
            }
        }
        return null;
    }

    private static String valueAfter(String dump, String key) {
        if (dump == null) {
            return null;
        }
        for (String line : dump.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key)) {
                return trimmed.substring(key.length()).trim();
            }
        }
        return null;
    }

    private static Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double toGb(long kilobytes) {
        return kilobytes / 1024.0 / 1024.0;
    }

    /** Размеры хранилища в гибибайтах. */
    public record Storage(double totalGb, double usedGb, double freeGb) {
    }
}
