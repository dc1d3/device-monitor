const REFRESH_MS = 5000;
const API = '/api/device';
const HISTORY_API = '/api/history';
const SCREEN_API = '/api/screen';

const $ = (id) => document.getElementById(id);

const els = {
    status: $('status'),
    error: $('error'),
    batteryLevel: $('battery-level'),
    batteryFill: $('battery-fill'),
    batteryState: $('battery-state'),
    name: $('device-name'),
    model: $('device-model'),
    manufacturer: $('device-manufacturer'),
    android: $('device-android'),
    storageFree: $('storage-free'),
    storageTotal: $('storage-total'),
    storageUsed: $('storage-used'),
    storageFill: $('storage-fill'),
    chartBattery: $('chart-battery'),
    chartStorage: $('chart-storage'),
    chartBatteryLast: $('chart-battery-last'),
    chartStorageLast: $('chart-storage-last'),
    chartNote: $('chart-note'),
    screenBtn: $('screen-btn'),
    screenImg: $('screen-img'),
    screenHint: $('screen-hint'),
    themeToggle: $('theme-toggle'),
    updated: $('updated'),
    refresh: $('refresh')
};

let lastHistory = [];
let screenUrl = null;

function setStatus(text, kind) {
    els.status.textContent = text;
    els.status.className = 'pill pill--' + kind;
}

function batteryColor(level) {
    if (level <= 20) return 'var(--bad)';
    if (level <= 40) return 'var(--warn)';
    return 'var(--ok)';
}

function dash(value, suffix) {
    return value === null || value === undefined ? '—' : value + (suffix || '');
}

function render(data) {
    const ok = data.connected;

    els.error.hidden = ok;
    if (!ok) {
        els.error.textContent = data.error || 'Телефон недоступен';
    }

    setStatus(ok ? 'Телефон на связи' : 'Нет связи', ok ? 'ok' : 'bad');

    // Заряд
    els.batteryLevel.textContent = dash(data.battery);
    els.batteryFill.style.width = (data.battery ?? 0) + '%';
    els.batteryFill.style.background = data.battery == null
        ? 'var(--muted)'
        : batteryColor(data.battery);
    els.batteryState.textContent = !ok
        ? '—'
        : data.charging ? 'Заряжается' : 'От батареи';

    // Устройство
    els.name.textContent = dash(data.name);
    els.model.textContent = dash(data.model);
    els.manufacturer.textContent = dash(data.manufacturer);
    els.android.textContent = dash(data.androidVersion);

    // Память
    const s = data.storage || {};
    els.storageFree.textContent = dash(s.freeGb);
    els.storageTotal.textContent = dash(s.totalGb);
    els.storageUsed.textContent = dash(s.usedGb);

    const usedPercent = s.totalGb ? Math.round((s.usedGb / s.totalGb) * 100) : 0;
    els.storageFill.style.width = usedPercent + '%';

    els.updated.textContent = 'Обновлено: ' +
        new Date(data.readAt).toLocaleTimeString('ru-RU');
}

async function load(force) {
    try {
        const url = force ? '/api/device/refresh' : API;
        const res = await fetch(url, { method: force ? 'POST' : 'GET' });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        render(await res.json());
    } catch (e) {
        setStatus('Сервер недоступен', 'bad');
        els.error.hidden = false;
        els.error.textContent = 'Не удалось связаться с сервером: ' + e.message;
    }
}

/* ---------- графики ---------- */

function drawSeries(canvas, values, options) {
    const dpr = window.devicePixelRatio || 1;
    const width = canvas.clientWidth || 320;
    const height = canvas.clientHeight || 140;

    canvas.width = Math.round(width * dpr);
    canvas.height = Math.round(height * dpr);

    const ctx = canvas.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);

    // сетка
    ctx.strokeStyle = options.grid;
    ctx.lineWidth = 1;
    for (let i = 0; i <= 2; i++) {
        const y = 6 + ((height - 12) * i) / 2;
        ctx.beginPath();
        ctx.moveTo(6, y);
        ctx.lineTo(width - 6, y);
        ctx.stroke();
    }

    if (values.length < 2) {
        ctx.fillStyle = options.text;
        ctx.font = '13px "Segoe UI", sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('мало данных', width / 2, height / 2);
        return;
    }

    let min = options.min !== undefined ? options.min : Math.min(...values);
    let max = options.max !== undefined ? options.max : Math.max(...values);
    if (max - min < 0.5) max = min + 0.5;
    if (options.min === undefined) min -= (max - min) * 0.1;
    if (options.max === undefined) max += (max - min) * 0.1;

    const padY = 12;
    const padX = 6;
    const x = (i) => padX + ((width - padX * 2) * i) / (values.length - 1);
    const y = (v) => height - padY - ((v - min) / (max - min)) * (height - padY * 2);

    // заливка под линией
    const gradient = ctx.createLinearGradient(0, 0, 0, height);
    gradient.addColorStop(0, options.color + '44');
    gradient.addColorStop(1, options.color + '00');

    ctx.beginPath();
    ctx.moveTo(x(0), y(values[0]));
    for (let i = 1; i < values.length; i++) ctx.lineTo(x(i), y(values[i]));
    ctx.lineTo(width - padX, height - 6);
    ctx.lineTo(padX, height - 6);
    ctx.closePath();
    ctx.fillStyle = gradient;
    ctx.fill();

    // линия
    ctx.beginPath();
    ctx.moveTo(x(0), y(values[0]));
    for (let i = 1; i < values.length; i++) ctx.lineTo(x(i), y(values[i]));
    ctx.strokeStyle = options.color;
    ctx.lineWidth = 2;
    ctx.lineJoin = 'round';
    ctx.stroke();

    // последняя точка
    ctx.beginPath();
    ctx.arc(x(values.length - 1), y(values[values.length - 1]), 3.5, 0, Math.PI * 2);
    ctx.fillStyle = options.color;
    ctx.fill();
}

function chartPalette() {
    const styles = getComputedStyle(document.documentElement);
    const read = (name, fallback) => styles.getPropertyValue(name).trim() || fallback;
    return {
        grid: read('--chart-grid', 'rgba(196,204,218,.85)'),
        text: read('--chart-text', '#7b859a'),
        battery: read('--chart-battery', '#2fae72'),
        storage: read('--chart-storage', '#4a7dff')
    };
}

function updateCharts(points) {
    lastHistory = points || [];
    const palette = chartPalette();

    const battery = lastHistory.map((p) => p.battery).filter((v) => v !== null && v !== undefined);
    const free = lastHistory.map((p) => p.freeGb).filter((v) => v !== null && v !== undefined);

    drawSeries(els.chartBattery, battery, {
        color: palette.battery, grid: palette.grid, text: palette.text, min: 0, max: 100
    });
    drawSeries(els.chartStorage, free, {
        color: palette.storage, grid: palette.grid, text: palette.text
    });

    els.chartBatteryLast.textContent = battery.length ? battery[battery.length - 1] + ' %' : '—';
    els.chartStorageLast.textContent = free.length ? free[free.length - 1].toFixed(1) + ' ГБ' : '—';
    els.chartNote.textContent = lastHistory.length
        ? 'замеров: ' + lastHistory.length
        : 'собираем данные…';
}

async function loadHistory() {
    try {
        const res = await fetch(HISTORY_API);
        if (!res.ok) return;
        const data = await res.json();
        updateCharts(data.points);
    } catch (e) {
        /* график не критичен: молча ждём следующий цикл */
    }
}

/* ---------- скриншот ---------- */

async function takeScreen() {
    els.screenBtn.disabled = true;
    els.screenBtn.textContent = 'Снимаю…';
    try {
        const res = await fetch(SCREEN_API, { cache: 'no-store' });
        if (!res.ok) {
            let message = 'Не удалось снять экран';
            try {
                const body = await res.json();
                // API отдаёт ApiError: человекочитаемый текст лежит в message
                if (body.message) message = body.message;
            } catch (ignored) { /* тело не JSON */ }
            els.screenImg.hidden = true;
            els.screenHint.hidden = false;
            els.screenHint.textContent = message;
            return;
        }
        const blob = await res.blob();
        if (screenUrl) URL.revokeObjectURL(screenUrl);
        screenUrl = URL.createObjectURL(blob);
        els.screenImg.src = screenUrl;
        els.screenImg.hidden = false;
        els.screenHint.hidden = true;
    } catch (e) {
        els.screenImg.hidden = true;
        els.screenHint.hidden = false;
        els.screenHint.textContent = 'Ошибка: ' + e.message;
    } finally {
        els.screenBtn.disabled = false;
        els.screenBtn.textContent = 'Снять экран';
    }
}

/* ---------- тема день / ночь ---------- */

const THEME_KEY = 'device-monitor-theme';

function currentTheme() {
    return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
}

function applyTheme(theme) {
    const dark = theme === 'dark';
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');

    const label = dark ? 'Включить светлую тему' : 'Включить тёмную тему';
    els.themeToggle.setAttribute('aria-label', label);
    els.themeToggle.title = label;
}

applyTheme(currentTheme());

els.themeToggle.addEventListener('click', () => {
    const next = currentTheme() === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    try {
        localStorage.setItem(THEME_KEY, next);
    } catch (e) { /* приватный режим — просто не запомним */ }
    updateCharts(lastHistory);
});

/* ---------- события ---------- */

els.refresh.addEventListener('click', async () => {
    els.refresh.disabled = true;
    await load(true);
    await loadHistory();
    els.refresh.disabled = false;
});

els.screenBtn.addEventListener('click', takeScreen);

window.addEventListener('resize', () => updateCharts(lastHistory));

load(false);
loadHistory();
setInterval(() => {
    load(false);
    loadHistory();
}, REFRESH_MS);
