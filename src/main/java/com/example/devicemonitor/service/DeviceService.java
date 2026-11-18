package com.example.devicemonitor.service;

import com.example.devicemonitor.config.DeviceProperties;
import com.example.devicemonitor.datasource.DeviceDataSource;
import com.example.devicemonitor.model.DeviceSnapshot;
import com.example.devicemonitor.model.HistoryPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Отдаёт последнее состояние телефона и кэширует его на app.device.cache-ms.
 * Благодаря кэшу несколько открытых вкладок не заставляют adb работать без остановки.
 */
@Service
public class DeviceService {

    /** Храним последние 120 замеров: при опросе раз в 5 секунд это ~10 минут. */
    private static final int MAX_HISTORY = 120;

    private final DeviceDataSource dataSource;
    private final long cacheMs;

    private volatile DeviceSnapshot cached;
    private volatile long cachedAt;

    private final Deque<HistoryPoint> history = new ArrayDeque<>();

    public DeviceService(DeviceDataSource dataSource, DeviceProperties properties) {
        this.dataSource = dataSource;
        this.cacheMs = properties.cacheMs();
    }

    /** Последний снимок: из кэша, если он свежий, иначе — новый запрос к телефону. */
    public DeviceSnapshot current() {
        DeviceSnapshot snapshot = cached;
        if (snapshot != null && System.currentTimeMillis() - cachedAt < cacheMs) {
            return snapshot;
        }
        synchronized (this) {
            if (cached != null && System.currentTimeMillis() - cachedAt < cacheMs) {
                return cached;
            }
            return load();
        }
    }

    /** Принудительно перечитать данные (кнопка «Обновить»). */
    public DeviceSnapshot refresh() {
        synchronized (this) {
            return load();
        }
    }

    /** История замеров, старые — первыми. */
    public List<HistoryPoint> history() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /** PNG-снимок экрана телефона или {@code null}, если снять не удалось. */
    public byte[] screenshot() {
        return dataSource.screenshot();
    }

    private DeviceSnapshot load() {
        DeviceSnapshot snapshot = dataSource.read();
        cached = snapshot;
        cachedAt = System.currentTimeMillis();
        if (snapshot.connected() && snapshot.batteryLevel() != null) {
            record(snapshot);
        }
        return snapshot;
    }

    private void record(DeviceSnapshot snapshot) {
        synchronized (history) {
            history.addLast(new HistoryPoint(
                    snapshot.readAt(),
                    snapshot.batteryLevel(),
                    snapshot.storageFreeGb()));
            while (history.size() > MAX_HISTORY) {
                history.removeFirst();
            }
        }
    }
}
