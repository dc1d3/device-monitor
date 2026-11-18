package com.example.devicemonitor.service;

import com.example.devicemonitor.config.DeviceProperties;
import com.example.devicemonitor.datasource.DeviceDataSource;
import com.example.devicemonitor.model.DeviceSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет кэш и историю сервиса на подставном источнике данных.
 */
class DeviceServiceTest {

    @Test
    void repeatedCallsWithinCacheUseOneRead() {
        CountingSource source = new CountingSource();
        DeviceService service = new DeviceService(source, properties(10_000));

        DeviceSnapshot first = service.current();
        DeviceSnapshot second = service.current();

        assertThat(source.reads).isEqualTo(1);
        assertThat(second).isSameAs(first);
    }

    @Test
    void refreshBypassesCache() {
        CountingSource source = new CountingSource();
        DeviceService service = new DeviceService(source, properties(10_000));

        service.current();
        DeviceSnapshot refreshed = service.refresh();

        assertThat(source.reads).isEqualTo(2);
        assertThat(refreshed.batteryLevel()).isEqualTo(2);
    }

    @Test
    void historyKeepsOnlyLast120Points() {
        CountingSource source = new CountingSource();
        DeviceService service = new DeviceService(source, properties(0));

        for (int i = 0; i < 130; i++) {
            service.refresh();
        }

        assertThat(service.history()).hasSize(120);
        assertThat(service.history().get(0).battery()).isEqualTo(11);
        assertThat(service.history().get(119).battery()).isEqualTo(130);
    }

    @Test
    void offlineSnapshotDoesNotPolluteHistory() {
        CountingSource source = new CountingSource();
        source.connected = false;
        DeviceService service = new DeviceService(source, properties(0));

        DeviceSnapshot snapshot = service.refresh();

        assertThat(snapshot.connected()).isFalse();
        assertThat(service.history()).isEmpty();
    }

    private DeviceProperties properties(long cacheMs) {
        return new DeviceProperties(false, "adb", 4000, cacheMs);
    }

    /** Источник, который считает обращения и отдаёт заряд, равный номеру обращения. */
    private static final class CountingSource implements DeviceDataSource {

        private int reads;
        private boolean connected = true;

        @Override
        public DeviceSnapshot read() {
            reads++;
            if (!connected) {
                return DeviceSnapshot.offline("телефон не подключён");
            }
            return new DeviceSnapshot(
                    true,
                    reads,
                    false,
                    "Тестовый телефон",
                    "Test",
                    "T-1",
                    "15",
                    100.0,
                    50.0,
                    50.0,
                    Instant.now(),
                    null);
        }

        @Override
        public byte[] screenshot() {
            return null;
        }
    }
}
