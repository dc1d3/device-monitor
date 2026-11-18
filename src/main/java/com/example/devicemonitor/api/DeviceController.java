package com.example.devicemonitor.api;

import com.example.devicemonitor.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Тонкий контроллер: только HTTP-маппинг, вся логика — в сервисе.
 * Ошибки источника данных не превращаются в 500, а приходят как connected=false + error.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Устройство", description = "Состояние Android-телефона, история замеров и скриншот экрана")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/device")
    @Operation(
            summary = "Текущее состояние телефона",
            description = "Отдаёт закэшированный на несколько секунд снимок. "
                    + "Если телефон не найден, возвращает 200 с connected=false и текстом ошибки.")
    public DeviceResponse device() {
        return DeviceResponse.from(deviceService.current());
    }

    @PostMapping("/device/refresh")
    @Operation(
            summary = "Перечитать данные",
            description = "Игнорирует кэш и заново опрашивает телефон. Используется кнопкой «Обновить».")
    public DeviceResponse refresh() {
        return DeviceResponse.from(deviceService.refresh());
    }

    @GetMapping("/history")
    @Operation(
            summary = "История замеров",
            description = "До 120 последних точек в памяти, старые — первыми. Нужна для графиков.")
    public HistoryResponse history() {
        return new HistoryResponse(deviceService.history());
    }

    @GetMapping(value = "/screen", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Скриншот экрана",
            description = "PNG-снимок через `adb exec-out screencap -p`. "
                    + "Если снять не удалось — 503 и JSON с описанием ошибки.")
    @ApiResponse(responseCode = "200", description = "PNG-изображение",
            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "503", description = "Телефон недоступен",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Object> screen() {
        byte[] png = deviceService.screenshot();
        if (png == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiError.of(503,
                            "Экран недоступен: телефон не найден или не разрешил отладку.",
                            "/api/screen"));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(png);
    }
}
