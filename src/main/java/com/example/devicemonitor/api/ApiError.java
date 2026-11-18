package com.example.devicemonitor.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Единый формат ошибки API. Spring по умолчанию отдаёт разные структуры
 * для 404, 405 и 500 — здесь они приведены к одному виду.
 */
@Schema(description = "Ошибка API")
public record ApiError(
        @Schema(description = "Время ошибки", example = "2026-11-18T11:02:31Z")
        Instant timestamp,

        @Schema(description = "HTTP-код", example = "405")
        int status,

        @Schema(description = "Короткое название статуса", example = "Method Not Allowed")
        String error,

        @Schema(description = "Понятное описание для человека",
                example = "Метод не поддерживается для этого адреса")
        String message,

        @Schema(description = "Путь запроса", example = "/api/device")
        String path
) {

    public static ApiError of(int status, String message, String path) {
        return new ApiError(Instant.now(), status, reason(status), message, path);
    }

    private static String reason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 415 -> "Unsupported Media Type";
            case 500 -> "Internal Server Error";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };
    }
}
