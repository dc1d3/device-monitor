package com.example.devicemonitor.api;

import com.example.devicemonitor.model.HistoryPoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ответ /api/history: точки для графика, старые — первыми.
 */
@Schema(description = "История замеров для графиков")
public record HistoryResponse(
        @Schema(description = "Точки в хронологическом порядке")
        List<HistoryPoint> points
) {
}
