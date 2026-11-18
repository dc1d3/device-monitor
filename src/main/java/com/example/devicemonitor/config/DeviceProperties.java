package com.example.devicemonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки из application.yml (секция app.device).
 *
 * @param mock      true — отдавать фейковые данные, телефон не нужен
 * @param adbPath   путь к adb.exe; "adb" — искать в PATH
 * @param timeoutMs сколько ждать ответа от adb
 * @param cacheMs   сколько секунд отдавать последний снимок, не дёргая телефон
 */
@ConfigurationProperties(prefix = "app.device")
public record DeviceProperties(
        boolean mock,
        String adbPath,
        long timeoutMs,
        long cacheMs
) {

    public DeviceProperties {
        if (adbPath == null || adbPath.isBlank()) {
            adbPath = "adb";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 4000;
        }
        if (cacheMs < 0) {
            cacheMs = 5000;
        }
    }
}
