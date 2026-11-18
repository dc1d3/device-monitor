package com.example.devicemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeviceMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceMonitorApplication.class, args);
    }
}
