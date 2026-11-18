package com.example.devicemonitor.datasource;

import com.example.devicemonitor.model.DeviceSnapshot;

/**
 * Источник данных о телефоне.
 * Реализации: {@link AdbDeviceDataSource} (настоящий телефон) и {@link MockDeviceDataSource}
 * (демо без устройства).
 */
public interface DeviceDataSource {

    DeviceSnapshot read();

    /**
     * Снимок экрана в формате PNG.
     *
     * @return байты изображения или {@code null}, если устройство недоступно
     */
    byte[] screenshot();
}
