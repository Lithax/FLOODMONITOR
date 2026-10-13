// Flood Monitor web client (jQuery). Talks to the REST API and understands both JSON and XML.
$(function () {
    var API = '/api/v1';
    var state = { format: 'json', stations: [], timer: null, busy: false, seq: 0 };

    var LEVEL_CLASSES = {
        NORMAL: 'bg-green-100 text-green-800',
        WARNING: 'bg-orange-100 text-orange-800',
        CRITICAL: 'bg-red-100 text-red-800',
        UNKNOWN: 'bg-slate-200 text-slate-700'
    };

    // ---------- parsing: JSON and XML end up as the same plain objects ----------

    function xmlToObj(el) {
        var kids = $(el).children();
        if (kids.length === 0) return $(el).text().trim();
        var obj = {};
        kids.each(function () {
            var name = this.nodeName;
            var value = xmlToObj(this);
            if (name in obj) {
                if (!$.isArray(obj[name])) obj[name] = [obj[name]];
                obj[name].push(value);
            } else {
                obj[name] = value;
            }
        });
        return obj;
    }

    function isXml(text) { return text.trim().charAt(0) === '<'; }

    // body -> single object
    function parseObject(text) {
        if (isXml(text)) return xmlToObj($.parseXML(text).documentElement);
        return JSON.parse(text);
    }

    // body -> array (every child of the XML root is one item, JSON is an array or {key: [..]})
    function parseList(text) {
        if (isXml(text)) {
            return $($.parseXML(text).documentElement).children().map(function () { return [xmlToObj(this)]; }).get();
        }
        var data = JSON.parse(text);
        if ($.isArray(data)) return data;
        var key = Object.keys(data).filter(function (k) { return $.isArray(data[k]); })[0];
        return key ? data[key] : [];
    }

    // timestamp can be an ISO string or an object {epochSeconds, nanosecondsOfSecond}
    function toDate(ts) {
        if (ts && typeof ts === 'object' && ts.epochSeconds !== undefined) {
            return new Date(Number(ts.epochSeconds) * 1000 + Number(ts.nanosecondsOfSecond || 0) / 1e6);
        }
        if (typeof ts === 'number') return new Date(ts < 1e11 ? ts * 1000 : ts);
        return new Date(ts);
    }

    function normMeasurement(m) {
        return {
            timestamp: toDate(m.timestamp),
            waterLevel: Number(m.waterLevel),
            flowRate: Number(m.flowRate),
            rainfall: Number(m.rainfall),
            temperature: Number(m.temperature),
            batteryLevel: Number(m.batteryLevel),
            status: m.status,
            warningLevel: m.warningLevel
        };
    }

    function normStation(s) {
        var active = s.isActive !== undefined ? s.isActive : s.active;
        return {
            id: s.id,
            name: s.name,
            river: s.river,
            isActive: active === true || active === 'true',
            status: s.status,
            normal: Number(s.normalWaterLevel),
            warning: Number(s.warningWaterLevel),
            critical: Number(s.criticalWaterLevel)
        };
    }

    // ---------- requests ----------

    function request(path, params) {
        return $.ajax({
            url: API + path,
            data: params,
            dataType: 'text',
            headers: { Accept: state.format === 'xml' ? 'application/xml' : 'application/json' }
        });
    }

    function errorMessage(xhr) {
        var detail = '';
        try {
            var err = parseObject(xhr.responseText);
            detail = err.message || '';
        } catch (e) { /* body is empty or not parseable */ }
        if (xhr.status === 0) return 'Server nicht erreichbar. Läuft die Anwendung?';
        if (!detail) {
            var fallback = { 400: 'Ungültige Anfrage.', 404: 'Nicht gefunden.', 406: 'Dieses Format wird nicht unterstützt.', 500: 'Interner Serverfehler.' };
            detail = fallback[xhr.status] || xhr.statusText;
        }
        return 'Fehler ' + xhr.status + ': ' + detail;
    }

    function showError(msg) {
        $('#errorText').text(msg);
        $('#error').removeClass('hidden');
    }

    function clearError() { $('#error').addClass('hidden'); }

    // ---------- rendering ----------

    function pad(n) { return String(n).padStart(2, '0'); }

    function fmtDate(d) {
        if (isNaN(d)) return '-';
        return d.toLocaleDateString('de-AT') + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    }

    function fmtNum(n, digits) { return isNaN(n) ? '-' : n.toFixed(digits); }

    function badge(level) {
        var cls = LEVEL_CLASSES[level] || LEVEL_CLASSES.UNKNOWN;
        return $('<span>').addClass('inline-block rounded-full px-2 py-0.5 text-xs font-semibold ' + cls).text(level || 'UNKNOWN');
    }

    function selectedStation() {
        var id = String($('#station').val());
        return state.stations.filter(function (s) { return String(s.id) === id; })[0];
    }

    function renderStations() {
        var $sel = $('#station').empty();
        $.each(state.stations, function (_, s) {
            $('<option>').val(s.id).text(s.name + ' (' + s.river + ')' + (s.isActive ? '' : ' [inaktiv]')).appendTo($sel);
        });
    }

    function renderCurrent(m) {
        var st = selectedStation();
        var $c = $('#current').empty().removeClass('text-slate-500');
        if (!m) {
            $c.addClass('text-slate-500').text('Für diese Station gibt es noch keine Messung.');
            return;
        }
        var items = [
            ['Station', st ? st.name + ' (' + st.river + ')' : '-'],
            ['Zeitpunkt', fmtDate(m.timestamp)],
            ['Wasserstand', fmtNum(m.waterLevel, 1) + ' cm'],
            ['Durchfluss', fmtNum(m.flowRate, 1) + ' m³/s'],
            ['Niederschlag', fmtNum(m.rainfall, 1) + ' mm/h'],
            ['Temperatur', fmtNum(m.temperature, 1) + ' °C'],
            ['Akku', fmtNum(m.batteryLevel, 1) + ' %'],
            ['Status', m.status]
        ];
        var $grid = $('<div class="grid grid-cols-2 sm:grid-cols-4 gap-3">').appendTo($c);
        $.each(items, function (_, it) {
            $('<div>').append($('<div class="text-xs text-slate-500">').text(it[0]), $('<div class="font-medium">').text(it[1])).appendTo($grid);
        });
        $('<div>').append($('<div class="text-xs text-slate-500">').text('Warnstufe'), badge(m.warningLevel)).appendTo($grid);
        if (st) {
            $('<div class="mt-3 text-xs text-slate-500">').text('Schwellwerte: normal ' + st.normal + ' cm, Warnung ab ' + st.warning + ' cm, kritisch ab ' + st.critical + ' cm').appendTo($c);
        }
    }

    function renderRows(list) {
        var st = selectedStation();
        var $body = $('#rows').empty();
        $('#count').text('(' + list.length + ' Einträge)');
        if (list.length === 0) {
            $('<tr>').append($('<td colspan="8" class="py-4 text-slate-500">').text('Keine Messwerte für diese Filter.')).appendTo($body);
            return;
        }
        // newest first
        list.sort(function (a, b) { return b.timestamp - a.timestamp; });
        $.each(list, function (_, m) {
            var $tr = $('<tr class="border-b border-slate-100">');
            $('<td class="py-2 pr-3">').text(st ? st.name : '-').appendTo($tr);
            $('<td class="py-2 pr-3 whitespace-nowrap">').text(fmtDate(m.timestamp)).appendTo($tr);
            $('<td class="py-2 pr-3 text-right">').text(fmtNum(m.waterLevel, 1)).appendTo($tr);
            $('<td class="py-2 pr-3 text-right">').text(fmtNum(m.flowRate, 1)).appendTo($tr);
            $('<td class="py-2 pr-3 text-right">').text(fmtNum(m.rainfall, 1)).appendTo($tr);
            $('<td class="py-2 pr-3 text-right">').text(fmtNum(m.batteryLevel, 1)).appendTo($tr);
            $('<td class="py-2 pr-3">').text(m.status).appendTo($tr);
            $('<td class="py-2">').append(badge(m.warningLevel)).appendTo($tr);
            $tr.appendTo($body);
        });
    }

    function renderFormat() {
        $('.format-btn').each(function () {
            var active = $(this).data('format') === state.format;
            $(this).toggleClass('bg-sky-600 text-white', active).toggleClass('bg-white hover:bg-slate-50', !active);
        });
    }

    function setLoading(on) {
        state.busy = on;
        $('#loading').toggleClass('hidden', !on).toggleClass('flex', on);
        $('#rows').toggleClass('opacity-50', on);
    }

    // ---------- loading data ----------

    function loadStations() {
        return request('/stations').then(function (text) {
            state.stations = parseList(text).map(normStation);
            var previous = $('#station').val();
            renderStations();
            if (previous && selectedStationExists(previous)) $('#station').val(previous);
        });
    }

    function selectedStationExists(id) {
        return state.stations.some(function (s) { return String(s.id) === String(id); });
    }

    function historyParams() {
        var p = {};
        if ($('#from').val()) p.from = new Date($('#from').val()).toISOString();
        if ($('#to').val()) p.to = new Date($('#to').val()).toISOString();
        if ($('#warningLevel').val()) p.warningLevel = $('#warningLevel').val();
        if ($('#limit').val() !== '') p.limit = $('#limit').val();
        return p;
    }

    function refresh() {
        var run = ++state.seq;
        var current = function () { return run === state.seq; }; // false if a newer refresh was started
        setLoading(true);
        clearError();

        var stationsReady = state.stations.length ? $.Deferred().resolve().promise() : loadStations();

        stationsReady.then(function () {
            if (!current()) return;
            var id = $('#station').val();
            if (id === null || id === undefined || id === '') {
                renderCurrent(null);
                renderRows([]);
                return;
            }
            var latest = request('/stations/' + id + '/measurements/latest').then(
                function (text) { if (current()) renderCurrent(normMeasurement(parseObject(text))); },
                function (xhr) {
                    if (xhr.status === 404) { if (current()) renderCurrent(null); return; } // no measurement yet
                    return $.Deferred().reject(xhr).promise();
                }
            );
            var history = request('/stations/' + id + '/measurements', historyParams()).then(function (text) {
                if (current()) renderRows(parseList(text).map(normMeasurement));
            });
            return $.when(latest, history);
        }).fail(function (xhr) {
            if (!current()) return;
            showError(xhr && xhr.status !== undefined ? errorMessage(xhr) : 'Antwort konnte nicht gelesen werden.');
        }).always(function () {
            if (!current()) return;
            setLoading(false);
            $('#updated').text('Zuletzt aktualisiert: ' + new Date().toLocaleTimeString('de-AT'));
            if (window.lucide) lucide.createIcons();
        });
    }

    function setupTimer() {
        if (state.timer) clearInterval(state.timer);
        var ms = Number($('#interval').val());
        if (ms > 0) state.timer = setInterval(function () { if (!state.busy) refresh(); }, ms);
    }

    // ---------- events ----------

    $('#apply').on('click', refresh);
    $('#station, #warningLevel, #from, #to, #limit').on('change', refresh);
    $('#interval').on('change', setupTimer);
    $('#reset').on('click', function () {
        $('#from, #to, #warningLevel').val('');
        $('#limit').val(50);
        refresh();
    });
    $('.format-btn').on('click', function () {
        state.format = $(this).data('format');
        renderFormat();
        state.stations = []; // reload the station list in the new format as well
        refresh();
    });

    renderFormat();
    setupTimer();
    refresh();
    if (window.lucide) lucide.createIcons();
});
