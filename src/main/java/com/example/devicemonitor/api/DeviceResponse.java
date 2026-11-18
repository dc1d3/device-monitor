package com.example.devicemonitor.api;

import com.example.devicemonitor.model.DeviceSnapshot;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * То, что уходит в JSON. Отдельный DTO, чтобы внутренняя модель
 * могла меняться независимо от контракта API.
 */
@Schema(description = "Состояние телефона на момент замера")
public record DeviceResponse(
        @Schema(description = "Телефон подключён и отвечает на команды", example = "true")
        boolean connected,

        @Schema(description = "Заряд в процентах", example = "90")
        Integer battery,

        @Schema(description = "Идёт зарядка", example = "true")
        boolean charging,

        @Schema(description = "Понятное имя устройства", example = "realme Note 50")
        String name,

        @Schema(description = "Производитель", example = "realme")
        String manufacturer,

        @Schema(description = "Модель", example = "RMX3834")
        String model,

        @Schema(description = "Версия Android", example = "14")
        String androidVersion,

        @Schema(description = "Данные накопителя")
        Storage storage,

        @Schema(description = "Время замера", example = "2026-11-18T11:02:31Z")
        Instant readAt,

        @Schema(description = "Описание ошибки, если connected=false")
        String error
) {

    @Schema(description = "Занятое и свободное место в гибибайтах")
    public record Storage(
            @Schema(description = "Всего, ГБ", example = "47.5")
            Double totalGb,

            @Schema(description = "Занято, ГБ", example = "26.1")
            Double usedGb,

            @Schema(description = "Свободно, ГБ", example = "21.2")
            Double freeGb
    ) {
    }

    public static DeviceResponse from(DeviceSnapshot snapshot) {
        Storage storage = snapshot.storageTotalGb() == null
                ? null
                : new Storage(snapshot.storageTotalGb(), snapshot.storageUsedGb(), snapshot.storageFreeGb());

        return new DeviceResponse(
                snapshot.connected(),
                snapshot.batteryLevel(),
                snapshot.charging(),
                snapshot.name(),
                snapshot.manufacturer(),
                snapshot.model(),
                snapshot.androidVersion(),
                storage,
                snapshot.readAt(),
                snapshot.error());
    }
}
