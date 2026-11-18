package com.example.devicemonitor.config;

import com.example.devicemonitor.datasource.AdbDeviceDataSource;
import com.example.devicemonitor.datasource.DeviceDataSource;
import com.example.devicemonitor.datasource.MockDeviceDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Выбор источника данных: настоящий adb или мок.
 * Меняется одной строкой в application.yml — код остальных слоёв не трогаем.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DeviceDataSource deviceDataSource(DeviceProperties properties) {
        if (properties.mock()) {
            return new MockDeviceDataSource();
        }
        return new AdbDeviceDataSource(properties.adbPath(), Duration.ofMillis(properties.timeoutMs()));
    }
}
