package com.example.devicemonitor.model;

import java.time.Instant;

/**
 * Одна точка истории для графика.
 */
public record HistoryPoint(Instant at, Integer battery, Double freeGb) {
}
