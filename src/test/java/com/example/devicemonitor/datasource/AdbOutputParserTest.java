package com.example.devicemonitor.datasource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Юнит-тесты разбора вывода adb: без Spring, без телефона, без процессов.
 */
class AdbOutputParserTest {

    @Test
    void findsOnlyAuthorizedDevice() {
        String output = """
                * daemon not running; starting now at tcp:5037
                * daemon started successfully
                List of devices attached
                emulator-5554\toffline
                R58M12345\tunauthorized
                R58M99999\tdevice
                """;

        assertThat(AdbOutputParser.findDeviceSerial(output)).isEqualTo("R58M99999");
    }

    @Test
    void returnsNullWhenNoDeviceIsReady() {
        String output = """
                List of devices attached
                emulator-5554\toffline
                """;

        assertThat(AdbOutputParser.findDeviceSerial(output)).isNull();
        assertThat(AdbOutputParser.findDeviceSerial("")).isNull();
        assertThat(AdbOutputParser.findDeviceSerial(null)).isNull();
    }

    @Test
    void parsesBatteryLevelAndChargingStatus() {
        String dump = """
                Current Battery Service state:
                  AC powered: false
                  USB powered: true
                  status: 2
                  level: 90
                  scale: 100
                """;

        assertThat(AdbOutputParser.parseBatteryLevel(dump)).isEqualTo(90);
        assertThat(AdbOutputParser.isCharging(dump)).isTrue();
    }

    @Test
    void fullBatteryCountsAsCharging() {
        assertThat(AdbOutputParser.isCharging("  status: 5\n  level: 100")).isTrue();
    }

    @Test
    void dischargingBatteryIsNotCharging() {
        assertThat(AdbOutputParser.isCharging("  status: 3\n  level: 55")).isFalse();
    }

    @Test
    void brokenBatteryValuesDoNotThrow() {
        assertThat(AdbOutputParser.parseBatteryLevel("  level: unknown")).isNull();
        assertThat(AdbOutputParser.parseBatteryLevel(null)).isNull();
        assertThat(AdbOutputParser.isCharging("  status: charging")).isFalse();
        assertThat(AdbOutputParser.isCharging(null)).isFalse();
    }

    @Test
    void parsesRealmeDfOutput() {
        // Так выглядит df /data на realme Note 50: точкой монтирования может быть
        // совсем другой путь, поэтому ориентируемся на числовые колонки.
        String output = """
                Filesystem       1K-blocks     Used Available Use% Mounted on
                /dev/block/dm-52  49767424 27366344  22270008  56% /storage/emulated/0/Android/obb
                """;

        AdbOutputParser.Storage storage = AdbOutputParser.parseStorage(output);

        assertThat(storage).isNotNull();
        assertThat(storage.totalGb()).isCloseTo(47.5, within(0.1));
        assertThat(storage.usedGb()).isCloseTo(26.1, within(0.1));
        assertThat(storage.freeGb()).isCloseTo(21.2, within(0.1));
    }

    @Test
    void takesLastMatchingDfLine() {
        String output = """
                Filesystem     1K-blocks    Used Available Use% Mounted on
                /dev/block/dm-6 115534624 45678912  69855712  40% /data
                /dev/block/dm-7  10485760  1048576   9437184  10% /storage
                """;

        AdbOutputParser.Storage storage = AdbOutputParser.parseStorage(output);

        assertThat(storage).isNotNull();
        assertThat(storage.totalGb()).isCloseTo(10.0, within(0.01));
        assertThat(storage.freeGb()).isCloseTo(9.0, within(0.01));
    }

    @Test
    void returnsNullForHeaderOnlyOrEmptyDf() {
        assertThat(AdbOutputParser.parseStorage(
                "Filesystem 1K-blocks Used Available Use% Mounted on")).isNull();
        assertThat(AdbOutputParser.parseStorage("")).isNull();
        assertThat(AdbOutputParser.parseStorage(null)).isNull();
    }
}
