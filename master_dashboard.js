/**
 * ResQLink Master Opportunistic Sync & Rescue Operations Dashboard
 * 
 * Central Master Command Server for remote rescue teams far away from the disaster.
 * Receives opportunistic uploads when any mesh device gains internet connectivity,
 * rendering full analytics, multi-hop packet traces, live GPS maps, and dispatch controls.
 */

const http = require('http');
const dgram = require('dgram');
const os = require('os');
const { exec } = require('child_process');

const PORT = 9000;
const clients = new Set();

// Master In-Memory Data Store (Starts clean with 0 alerts and 0 logs)
let syncedAlertsMap = new Map();
let syncLogsList = [];
let dispatchStatusMap = new Map(); // alertId -> { status, notes, assignedTeam, updatedAt }

function ingestAlert(alert, sourceDevice) {
  if (!alert || !alert.id) return;
  const isNew = !syncedAlertsMap.has(alert.id);
  syncedAlertsMap.set(alert.id, {
    ...alert,
    syncedByDevice: alert.syncedByDevice || sourceDevice,
    syncTimestamp: Date.now()
  });

  const syncLogEntry = {
    id: `log-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
    timestamp: new Date().toLocaleTimeString(),
    deviceNode: sourceDevice,
    alertCount: 1,
    details: `Opportunistic Sync Upload: Ingested emergency alert "${alert.message || 'Emergency'}" from ${sourceDevice}.`
  };
  syncLogsList.unshift(syncLogEntry);

  console.log(`\n\x1b[42m\x1b[30m [MASTER DASHBOARD SOS INGESTED] \x1b[0m`);
  console.log(`\x1b[36mSource:\x1b[0m ${sourceDevice}`);
  console.log(`\x1b[32mVictim Coords:\x1b[0m ${alert.latitude}, ${alert.longitude}`);
  console.log(`\x1b[33mMessage:\x1b[0m "${alert.message}"`);

  notifyClients({
    type: 'SYNC_RECEIVED',
    log: syncLogEntry,
    alerts: Array.from(syncedAlertsMap.values())
  });
}

// Setup UDP Mesh Listener (Ports 8988 and 9000)
function setupUdpListener(port) {
  try {
    const udp = dgram.createSocket({ type: 'udp4', reuseAddr: true });
    udp.on('error', () => {});
    udp.on('message', (msg, rinfo) => {
      try {
        const text = msg.toString();
        if (text.includes('"id":') && text.includes('"latitude":')) {
          const payload = JSON.parse(text);
          ingestAlert(payload, `Wi-Fi Radio Mesh (${rinfo.address})`);
        }
      } catch (e) {}
    });
    udp.bind(port, () => {
      try { udp.setBroadcast(true); } catch (e) {}
    });
  } catch (e) {}
}
setupUdpListener(8988);
setupUdpListener(9000);

const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Device-Node');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // 1. Opportunistic Sync Ingestion Endpoint (POST /api/opportunistic_sync)
  if (url.pathname === '/api/opportunistic_sync' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        const deviceNode = payload.deviceNodeId || req.headers['x-device-node'] || req.socket.remoteAddress;
        const incomingAlerts = payload.alerts || (payload.id ? [payload] : []);
        let newCount = 0;

        incomingAlerts.forEach(alert => {
          if (!syncedAlertsMap.has(alert.id)) {
            newCount++;
          }
          syncedAlertsMap.set(alert.id, {
            ...alert,
            syncedByDevice: deviceNode,
            syncTimestamp: Date.now()
          });
        });

        const syncLogEntry = {
          id: `log-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
          timestamp: new Date().toLocaleTimeString(),
          deviceNode: deviceNode,
          alertCount: incomingAlerts.length,
          details: `Opportunistic Sync Complete: Uploaded ${incomingAlerts.length} alert(s) via restored internet link.`
        };
        syncLogsList.unshift(syncLogEntry);

        console.log(`\n\x1b[42m\x1b[30m [OPPORTUNISTIC SYNC UPLOAD RECEIVED] \x1b[0m`);
        console.log(`\x1b[36mSynced By Node:\x1b[0m ${deviceNode}`);
        console.log(`\x1b[32mAlerts Processed:\x1b[0m ${incomingAlerts.length} (${newCount} new)`);

        notifyClients({
          type: 'SYNC_RECEIVED',
          log: syncLogEntry,
          alerts: Array.from(syncedAlertsMap.values())
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          status: 'success',
          message: 'Opportunistic sync logs & analytics ingested successfully',
          alertsIngested: incomingAlerts.length
        }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'error', message: 'Invalid payload JSON' }));
      }
    });
    return;
  }

  // 2. Dispatch Action Endpoint (POST /api/dispatch)
  if (url.pathname === '/api/dispatch' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const { alertId, status, assignedTeam, notes } = JSON.parse(body);
        dispatchStatusMap.set(alertId, {
          status: status || 'RESCUE_DISPATCHED',
          assignedTeam: assignedTeam || 'NDRF Air-Water Team 01',
          notes: notes || 'Rescue unit dispatched to GPS coordinates',
          updatedAt: new Date().toLocaleTimeString()
        });

        notifyClients({
          type: 'DISPATCH_UPDATED',
          alertId,
          dispatchInfo: dispatchStatusMap.get(alertId)
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'updated', alertId }));
      } catch (e) {
        res.writeHead(400);
        res.end('Invalid request');
      }
    });
    return;
  }

  // 3. SSE Stream for Live Dashboard Updates (GET /events)
  if (url.pathname === '/events') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive'
    });
    const initData = {
      type: 'INIT',
      alerts: Array.from(syncedAlertsMap.values()),
      logs: syncLogsList,
      dispatches: Object.fromEntries(dispatchStatusMap)
    };
    res.write(`data: ${JSON.stringify(initData)}\n\n`);
    clients.add(res);
    req.on('close', () => clients.delete(res));
    return;
  }

  // 4. Serve Master Remote Rescue Operations Dashboard UI (GET /)
  if (url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(getDashboardHTML());
    return;
  }

  res.writeHead(404);
  res.end('Not Found');
});

function notifyClients(data) {
  const message = `data: ${JSON.stringify(data)}\n\n`;
  for (const client of clients) {
    client.write(message);
  }
}

function getLocalIPs() {
  const interfaces = os.networkInterfaces();
  const ips = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push(iface.address);
      }
    }
  }
  return ips;
}

server.listen(PORT, '0.0.0.0', () => {
  const ips = getLocalIPs();
  const primaryIp = ips[0] || 'localhost';

  console.log('\n\x1b[35m════════════════════════════════════════════════════════════════════════\x1b[0m');
  console.log('\x1b[1m\x1b[37m  RESQLINK MASTER OPPORTUNISTIC SYNC & RESCUE COMMAND DASHBOARD\x1b[0m');
  console.log('\x1b[35m════════════════════════════════════════════════════════════════════════\x1b[0m');

  console.log(`\n\x1b[36m  ① Open the Rescue Dashboard in your browser:\x1b[0m`);
  console.log(`     \x1b[1m\x1b[33mhttp://localhost:${PORT}\x1b[0m`);

  console.log(`\n\x1b[36m  ② To connect the Android app (phone on same Wi-Fi):\x1b[0m`);
  console.log(`     Open \x1b[1mlocal.properties\x1b[0m in the project root and set:`);
  ips.forEach(ip => {
    console.log(`     \x1b[1m\x1b[32mMASTER_DASHBOARD_URL=http://${ip}:${PORT}\x1b[0m`);
  });
  if (ips.length === 0) {
    console.log(`     \x1b[1m\x1b[32mMASTER_DASHBOARD_URL=http://<YOUR_PC_IP>:${PORT}\x1b[0m`);
  }
  console.log(`     Then rebuild the app in Android Studio.`);

  console.log(`\n\x1b[36m  ③ For internet/cloud access (ngrok):\x1b[0m`);
  console.log(`     Run: \x1b[1mngrok http ${PORT}\x1b[0m`);
  console.log(`     Set MASTER_DASHBOARD_URL to the https://xxx.ngrok.io URL shown.`);

  console.log('\n\x1b[35m════════════════════════════════════════════════════════════════════════\x1b[0m');
  console.log('\x1b[32m  Dashboard is LIVE. Waiting for opportunistic sync uploads...\x1b[0m');
  console.log('\x1b[35m════════════════════════════════════════════════════════════════════════\x1b[0m\n');

  // Try auto adb reverse if adb is available
  exec(`adb reverse tcp:${PORT} tcp:${PORT}`, (err) => {
    if (!err) {
      console.log(`\x1b[32m[ADB Auto-Forward]\x1b[0m Port ${PORT} forwarded over USB. Android Studio emulator will work automatically.`);
    }
  });
  exec(`adb reverse tcp:8988 tcp:8988`);
});

function getDashboardHTML() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ResQLink Master Operations • Remote Rescue Command Dashboard</title>
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    :root {
      --bg: #070A10;
      --card: #0F1623;
      --card-raised: #172033;
      --border: #1E2D4A;
      --border-bright: #2E436D;
      --text: #F1F5F9;
      --muted: #94A3B8;
      --ember: #FF4F3E;
      --signal: #35E0C8;
      --moss: #3DDC97;
      --amber: #F59E0B;
      --font: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: var(--bg); color: var(--text); font-family: var(--font); height: 100vh; overflow: hidden; display: flex; flex-direction: column; }
    
    /* Top Master Navigation Bar */
    header { height: 60px; background: var(--card); border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; padding: 0 24px; flex-shrink: 0; }
    .logo-group { display: flex; align-items: center; gap: 12px; }
    .logo-badge { background: var(--ember); color: #fff; font-size: 12px; font-weight: 900; padding: 4px 8px; border-radius: 6px; letter-spacing: 1px; }
    .logo-title { font-size: 18px; font-weight: 800; color: #fff; letter-spacing: 0.5px; }
    .logo-sub { font-size: 12px; color: var(--signal); background: rgba(53, 224, 200, 0.1); border: 1px solid var(--signal); padding: 3px 8px; border-radius: 999px; font-weight: 600; }
    .header-stats { display: flex; gap: 20px; font-size: 12px; }
    .stat-pill { background: var(--bg); border: 1px solid var(--border); padding: 6px 14px; border-radius: 8px; display: flex; align-items: center; gap: 8px; }
    .stat-val { font-weight: 800; font-size: 14px; color: #fff; }

    /* Main Grid Layout */
    .main-container { flex: 1; display: grid; grid-template-columns: 380px 1fr 420px; gap: 0; overflow: hidden; }
    
    /* Left Panel: Opportunistic Sync Log & Feed */
    .panel { background: var(--card); border-right: 1px solid var(--border); display: flex; flex-direction: column; overflow: hidden; }
    .panel-right { border-right: none; border-left: 1px solid var(--border); }
    .panel-header { padding: 16px 20px; border-bottom: 1px solid var(--border); font-size: 12px; font-weight: 800; color: var(--signal); text-transform: uppercase; letter-spacing: 1px; display: flex; justify-content: space-between; align-items: center; }
    .panel-scroll { flex: 1; overflow-y: auto; padding: 16px; }

    /* Sync Event Log Items */
    .sync-log-card { background: var(--card-raised); border: 1px solid var(--border); border-radius: 10px; padding: 14px; margin-bottom: 12px; font-size: 12px; line-height: 1.5; }
    .sync-log-time { color: var(--muted); font-size: 11px; margin-bottom: 4px; display: flex; justify-content: space-between; }
    .sync-log-node { color: var(--moss); font-weight: 700; }
    .sync-log-desc { color: var(--text); margin-top: 4px; }

    /* Center Panel: Map & Visual Command Center */
    .center-panel { display: flex; flex-direction: column; overflow: hidden; position: relative; }
    #map { flex: 1; width: 100%; height: 100%; background: #000; }
    
    /* Emergency Alert List Card */
    .alert-card { background: var(--card-raised); border: 1px solid var(--border); border-radius: 12px; padding: 16px; margin-bottom: 14px; cursor: pointer; transition: all 0.2s; position: relative; }
    .alert-card:hover { border-color: var(--signal); transform: translateY(-1px); }
    .alert-card.selected { border-color: var(--signal); background: rgba(53, 224, 200, 0.05); }
    .alert-card.extreme { border-left: 4px solid var(--ember); }
    .alert-card.high { border-left: 4px solid var(--amber); }

    .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .severity-badge { font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 4px; text-transform: uppercase; letter-spacing: 0.5px; }
    .severity-extreme { background: rgba(255, 79, 62, 0.2); color: var(--ember); border: 1px solid var(--ember); }
    .severity-high { background: rgba(245, 158, 11, 0.2); color: var(--amber); border: 1px solid var(--amber); }

    .victim-name { font-size: 15px; font-weight: 800; color: #fff; margin-bottom: 4px; }
    .alert-msg { font-size: 13px; color: var(--text); line-height: 1.4; margin-bottom: 12px; font-style: italic; background: rgba(0,0,0,0.3); padding: 8px 10px; border-radius: 6px; }

    /* Hop Route Trace Chips */
    .hop-trace { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-top: 8px; font-size: 11px; }
    .hop-chip { background: #0A0F1D; border: 1px solid var(--border-bright); padding: 2px 8px; border-radius: 4px; color: var(--signal); font-family: monospace; }

    /* Dispatch Controls */
    .dispatch-box { margin-top: 12px; padding-top: 12px; border-top: 1px dashed var(--border); display: flex; justify-content: space-between; align-items: center; }
    .dispatch-status { font-size: 11px; font-weight: 700; color: var(--moss); }
    .btn-dispatch { background: var(--signal); color: #070A10; border: none; padding: 6px 12px; border-radius: 6px; font-size: 11px; font-weight: 800; cursor: pointer; }
    .btn-dispatch:hover { opacity: 0.9; }

    /* Search & Filter Bar */
    .filter-bar { padding: 12px 16px; background: var(--card); border-bottom: 1px solid var(--border); display: flex; gap: 8px; }
    .filter-input { flex: 1; background: var(--bg); border: 1px solid var(--border); color: #fff; padding: 8px 12px; border-radius: 6px; font-size: 12px; }
  </style>
</head>
<body>

  <!-- Top Header -->
  <header>
    <div class="logo-group">
      <span class="logo-badge">MASTER COMMAND</span>
      <span class="logo-title">ResQLink Emergency Operations Hub</span>
      <span class="logo-sub">Opportunistic Sync Ingestion Active</span>
    </div>

    <div class="header-stats">
      <div class="stat-pill">
        <span>Synced Alerts:</span>
        <span class="stat-val" id="statAlerts">0</span>
      </div>
      <div class="stat-pill">
        <span>Active Disaster Nodes:</span>
        <span class="stat-val" id="statNodes" style="color: var(--signal);">0</span>
      </div>
      <div class="stat-pill">
        <span>Master Port:</span>
        <span class="stat-val" style="color: var(--moss);">${PORT}</span>
      </div>
    </div>
  </header>

  <!-- Main 3-Column Interface -->
  <div class="main-container">
    
    <!-- LEFT: Opportunistic Sync Ingestion Audit Logs -->
    <div class="panel">
      <div class="panel-header">
        <span>Opportunistic Sync Upload Audit Logs</span>
        <span style="font-size: 10px; color: var(--muted);" id="logCount">0 Logs</span>
      </div>
      <div class="panel-scroll" id="syncLogsContainer">
        <!-- Logs dynamically injected -->
      </div>
    </div>

    <!-- CENTER: Geographic Emergency Map -->
    <div class="center-panel">
      <div id="map"></div>
    </div>

    <!-- RIGHT: Master Synced Alerts & Dispatch Console -->
    <div class="panel panel-right">
      <div class="panel-header">
        <span>Remote Rescue Dispatch Queue</span>
        <span style="font-size: 10px; color: var(--ember);" id="pendingCount">1 Pending</span>
      </div>

      <div class="filter-bar">
        <input type="text" id="searchInput" class="filter-input" placeholder="Search by victim name, message, or GPS..." onkeyup="filterAlerts()" />
      </div>

      <div class="panel-scroll" id="alertsContainer">
        <!-- Alert Cards Injected -->
      </div>
    </div>

  </div>

  <script>
    let alertsData = [];
    let logsData = [];
    let dispatchesData = {};
    let selectedAlertId = null;
    let map, markersGroup;

    // Initialize Leaflet Map
    function initMap() {
      map = L.map('map', { zoomControl: true }).setView([28.6139, 77.2090], 13);
      L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        maxZoom: 19
      }).addTo(map);

      markersGroup = L.layerGroup().addTo(map);
    }

    function renderMapMarkers() {
      markersGroup.clearLayers();
      alertsData.forEach(alert => {
        const isExtreme = alert.severity === 'EXTREME';
        const color = isExtreme ? '#FF4F3E' : '#F59E0B';
        const marker = L.circleMarker([alert.latitude, alert.longitude], {
          radius: 10,
          fillColor: color,
          color: '#FFFFFF',
          weight: 2,
          opacity: 1,
          fillOpacity: 0.8
        });

        marker.bindPopup(\`
          <div style="font-family: sans-serif; padding: 4px;">
            <strong style="color: \${color};">\${alert.severity} PRIORITY</strong><br/>
            <strong>\${alert.userId || 'Citizen'}</strong><br/>
            \${alert.message}<br/><br/>
            <small>Synced via: \${alert.syncedByDevice || 'Mesh Link'}</small>
          </div>
        \`);

        markersGroup.addLayer(marker);
      });
    }

    function renderLogs() {
      const container = document.getElementById('syncLogsContainer');
      document.getElementById('logCount').textContent = \`\${logsData.length} Logs\`;
      
      if (logsData.length === 0) {
        container.innerHTML = '<div style="color: var(--muted); text-align: center; padding: 30px;">No sync events recorded yet.</div>';
        return;
      }

      container.innerHTML = logsData.map(log => \`
        <div class="sync-log-card">
          <div class="sync-log-time">
            <span>\${log.timestamp}</span>
            <span class="sync-log-node">\${log.deviceNode}</span>
          </div>
          <div class="sync-log-desc">\${log.details}</div>
        </div>
      \`).join('');
    }

    function renderAlerts() {
      const container = document.getElementById('alertsContainer');
      const query = document.getElementById('searchInput').value.toLowerCase();
      
      const filtered = alertsData.filter(a => 
        (a.userId || '').toLowerCase().includes(query) ||
        (a.message || '').toLowerCase().includes(query) ||
        (a.syncedByDevice || '').toLowerCase().includes(query)
      );

      document.getElementById('statAlerts').textContent = alertsData.length;
      document.getElementById('pendingCount').textContent = \`\${filtered.length} Displayed\`;

      if (filtered.length === 0) {
        container.innerHTML = '<div style="color: var(--muted); text-align: center; padding: 40px;">No synced alerts match search filter.</div>';
        return;
      }

      container.innerHTML = filtered.map(a => {
        const isSelected = a.id === selectedAlertId;
        const sevClass = (a.severity || 'HIGH').toLowerCase();
        const dispatch = dispatchesData[a.id] || { status: 'PENDING', assignedTeam: 'Unassigned' };
        const route = a.relayPath ? a.relayPath.join(' ➔ ') : 'Direct Uplink';

        return \`
          <div class="alert-card \${sevClass} \${isSelected ? 'selected' : ''}" onclick="selectAlert('\${a.id}', \${a.latitude}, \${a.longitude})">
            <div class="card-top">
              <span class="severity-badge severity-\${sevClass}">\${a.severity || 'HIGH'} PRIORITY</span>
              <span style="font-size: 11px; color: var(--muted);">Hop \${a.hopCount || 1}</span>
            </div>

            <div class="victim-name">\${a.userId || 'Victim Alert'}</div>
            <div class="alert-msg">"\${a.message}"</div>

            <div style="font-size: 11px; color: var(--muted); margin-bottom: 6px;">
              <strong>GPS:</strong> \${Number(a.latitude).toFixed(5)}, \${Number(a.longitude).toFixed(5)}<br/>
              <strong>Synced By:</strong> \${a.syncedByDevice || 'Restored Uplink Node'}
            </div>

            <!-- Hop Trace -->
            <div style="font-size: 10px; color: var(--signal); font-weight: 700; margin-top: 6px;">RELAY HOP TRACE:</div>
            <div class="hop-trace">
              <span class="hop-chip">\${route}</span>
            </div>

            <!-- Dispatch Action -->
            <div class="dispatch-box">
              <span class="dispatch-status">\${dispatch.status === 'RESCUE_DISPATCHED' ? '✔ DISPATCHED (' + dispatch.assignedTeam + ')' : '⏳ PENDING DISPATCH'}</span>
              <button class="btn-dispatch" onclick="event.stopPropagation(); triggerDispatch('\${a.id}')">
                \${dispatch.status === 'RESCUE_DISPATCHED' ? 'Update Status' : 'Dispatch Team'}
              </button>
            </div>
          </div>
        \`;
      }).join('');
    }

    function selectAlert(id, lat, lng) {
      selectedAlertId = id;
      renderAlerts();
      map.flyTo([lat, lng], 15, { duration: 1.2 });
    }

    function triggerDispatch(alertId) {
      const team = prompt('Enter Rescue Unit / Helicopter ID:', 'NDRF Helicopter Team 04');
      if (!team) return;
      fetch('/api/dispatch', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ alertId, status: 'RESCUE_DISPATCHED', assignedTeam: team })
      });
    }

    function filterAlerts() {
      renderAlerts();
    }

    // Connect Server-Sent Events (SSE) Stream
    const es = new EventSource('/events');
    es.onmessage = (e) => {
      const data = JSON.parse(e.data);
      if (data.type === 'INIT') {
        alertsData = data.alerts || [];
        logsData = data.logs || [];
        dispatchesData = data.dispatches || {};
        renderLogs();
        renderAlerts();
        renderMapMarkers();
      } else if (data.type === 'SYNC_RECEIVED') {
        alertsData = data.alerts;
        logsData.unshift(data.log);
        renderLogs();
        renderAlerts();
        renderMapMarkers();
      } else if (data.type === 'DISPATCH_UPDATED') {
        dispatchesData[data.alertId] = data.dispatchInfo;
        renderAlerts();
      }
    };

    window.onload = () => {
      initMap();
      renderLogs();
      renderAlerts();
      renderMapMarkers();
    };
  </script>
</body>
</html>`;
}
