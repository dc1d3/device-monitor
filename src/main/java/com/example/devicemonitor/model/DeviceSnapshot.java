package com.example.devicemonitor.model;

import java.time.Instant;

/**
 * Снимок состояния телефона в один момент времени.
 * Не зависит от того, откуда пришли данные (adb или мок), поэтому сервис и контроллер
 * ничего не знают про adb.
 */
public record DeviceSnapshot(
        boolean connected,
        Integer batteryLevel,
        boolean charging,
        String name,
        String manufacturer,
        String model,
        String androidVersion,
        Double storageTotalGb,
        Double storageUsedGb,
        Double storageFreeGb,
        Instant readAt,
        String error
) {

    public static DeviceSnapshot offline(String error) {
        return new DeviceSnapshot(false, null, false, null, null, null, null,
                null, null, null, Instant.now(), error);
    }
}
