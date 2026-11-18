package com.example.devicemonitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Метаданные для Swagger UI: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deviceMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Device Monitor API")
                        .version("1.0.0")
                        .description("""
                                Локальный монитор Android-телефона, подключённого по USB.
                                Данные читаются через adb и не покидают машину.
                                """)
                        .license(new License().name("MIT")));
    }
}
