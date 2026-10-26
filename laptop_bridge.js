/**
 * ResQLink Laptop Rescue Station & Mesh Bridge
 * 
 * Provides:
 * 1. HTTP Server on port 8988 (API for NearbyMeshManager USB bridge & Web Console)
 * 2. UDP Mesh Radio on port 8988 (Direct Wi-Fi Broadcast with phones on the same network)
 * 3. Live Browser Dashboard at http://localhost:8988
 */

const http = require('http');
const dgram = require('dgram');
const os = require('os');
const { exec } = require('child_process');

const PORT = 8988;
const laptopName = `Laptop Station (${os.hostname()})`;
const clients = new Set();
let connectedPhones = new Map();
let receivedAlerts = [];

// 1. Setup UDP Wi-Fi Mesh Socket
const udpSocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

udpSocket.on('error', (err) => {
  console.log(`[UDP Radio Warning] ${err.message}`);
});

udpSocket.on('message', (msg, rinfo) => {
  const text = msg.toString();
  try {
    if (text.startsWith('{"beacon":')) {
      const beacon = JSON.parse(text);
      if (beacon.nodeId === 'laptop_station') return; // Ignore own beacon
      const ip = rinfo.address;
      const deviceModel = beacon.beacon || 'Android Phone';
      const existing = connectedPhones.get(ip);
      connectedPhones.set(ip, {
        id: ip,
        name: deviceModel,
        ip: ip,
        transport: 'Wi-Fi Direct / Local Mesh',
        lastSeen: Date.now()
      });
      if (!existing) {
        console.log(`\x1b[32m[+ Wi-Fi Mesh Peer Connected]\x1b[0m ${deviceModel} at ${ip}`);
        notifyClients({ type: 'PEERS_UPDATED', peers: Array.from(connectedPhones.values()) });
      }
    } else if (text.includes('"id":') && text.includes('"latitude":')) {
      const alert = JSON.parse(text);
      handleIncomingAlert(alert, `Wi-Fi Mesh from ${rinfo.address}`);
    }
  } catch (e) {}
});

udpSocket.bind(PORT, () => {
  try {
    udpSocket.setBroadcast(true);
    console.log(`\x1b[36m[UDP Mesh Radio]\x1b[0m Listening and broadcasting on port ${PORT}`);
  } catch (e) {}
});

// Periodic UDP Beacon (Heartbeat every 2.5s)
setInterval(() => {
  const beaconMsg = Buffer.from(JSON.stringify({
    beacon: laptopName,
    nodeId: 'laptop_station'
  }));
  udpSocket.send(beaconMsg, 0, beaconMsg.length, PORT, '255.255.255.255', () => {});

  // Clean stale peers
  const now = Date.now();
  let changed = false;
  for (const [ip, peer] of connectedPhones.entries()) {
    if (now - peer.lastSeen > 7000 && peer.transport.includes('Wi-Fi')) {
      connectedPhones.delete(ip);
      changed = true;
      console.log(`\x1b[33m[- Wi-Fi Peer Timed Out]\x1b[0m ${peer.name} (${ip})`);
    }
  }
  if (changed) {
    notifyClients({ type: 'PEERS_UPDATED', peers: Array.from(connectedPhones.values()) });
  }
}, 2500);

function handleIncomingAlert(alert, source) {
  console.log('\n\x1b[41m\x1b[37m ================== EMERGENCY SOS RECEIVED ================== \x1b[0m');
  console.log(`\x1b[1m\x1b[33mSource:\x1b[0m ${source}`);
  console.log(`\x1b[1m\x1b[33mAlert ID:\x1b[0m ${alert.id}`);
  console.log(`\x1b[1m\x1b[33mUser / Node:\x1b[0m ${alert.userId || 'Citizen'}`);
  console.log(`\x1b[1m\x1b[32mCoordinates:\x1b[0m Lat ${alert.latitude}, Lng ${alert.longitude}`);
  console.log(`\x1b[1m\x1b[36mEmergency Message:\x1b[0m "${alert.message}"`);
  console.log(`\x1b[1m\x1b[35mRelay Hop Count:\x1b[0m ${alert.hopCount || 0}`);
  console.log('\x1b[41m\x1b[37m ============================================================ \x1b[0m\n');

  receivedAlerts.unshift({ ...alert, receivedAt: new Date().toLocaleTimeString(), source });
  notifyClients({ type: 'NEW_ALERT', alert: receivedAlerts[0] });
}

function notifyClients(data) {
  const json = `data: ${JSON.stringify(data)}\n\n`;
  for (const client of clients) {
    client.write(json);
  }
}

// 2. Setup HTTP Server
const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // API: Ping from phone
  if (url.pathname === '/api/ping') {
    const deviceName = url.searchParams.get('device') || 'Android Phone';
    const remoteIp = req.socket.remoteAddress;
    connectedPhones.set('usb_bridge', {
      id: 'usb_bridge',
      name: deviceName,
      ip: remoteIp,
      transport: 'USB Tunnel / Local HTTP',
      lastSeen: Date.now()
    });
    notifyClients({ type: 'PEERS_UPDATED', peers: Array.from(connectedPhones.values()) });
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'ok', station: laptopName }));
  }

  // API: Incoming SOS from phone
  if (url.pathname === '/api/sos' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        handleIncomingAlert(payload, 'USB Cable / Local HTTP Link');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'delivered_to_laptop_station' }));
      } catch (err) {
        res.writeHead(400);
        res.end('Invalid JSON');
      }
    });
    return;
  }

  // API: Trigger test alert from laptop to phone
  if (url.pathname === '/api/send_test' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      const data = JSON.parse(body || '{}');
      const testPayload = {
        id: `sos-laptop-${Date.now().toString(36)}`,
        userId: 'Laptop Commander',
        latitude: 28.6139,
        longitude: 77.2090,
        message: data.message || 'TEST ALERT: Laptop Rescue Station broadcast drill',
        severity: 'HIGH',
        createdAtEpochMillis: Date.now(),
        hopCount: 1
      };
      const bytes = Buffer.from(JSON.stringify(testPayload));
      udpSocket.send(bytes, 0, bytes.length, PORT, '255.255.255.255', () => {
        console.log(`\x1b[32m[Sent Test Alert via Wi-Fi Mesh]\x1b[0m "${testPayload.message}"`);
      });
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, payload: testPayload }));
    });
    return;
  }

  // SSE Stream for Live Web Updates
  if (url.pathname === '/events') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
    });
    res.write(`data: ${JSON.stringify({ type: 'INIT', peers: Array.from(connectedPhones.values()), alerts: receivedAlerts })}\n\n`);
    clients.add(res);
    req.on('close', () => clients.delete(res));
    return;
  }

  // Web Dashboard UI
  if (url.pathname === '/') {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>ResQLink • Laptop Rescue Command Station</title>
  <style>
    :root {
      --bg: #07090E;
      --card: #0E131F;
      --card-border: #1E293B;
      --text: #F8FAFC;
      --text-muted: #94A3B8;
      --ember: #FF4F3E;
      --signal: #35E0C8;
      --moss: #3DDC97;
      --font: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: var(--bg); color: var(--text); font-family: var(--font); padding: 24px; min-height: 100vh; }
    .header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid var(--card-border); margin-bottom: 24px; }
    .title-group h1 { font-size: 22px; font-weight: 800; color: #fff; display: flex; align-items: center; gap: 10px; }
    .badge-live { background: rgba(61, 220, 151, 0.15); color: var(--moss); border: 1px solid var(--moss); padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
    .sub { color: var(--text-muted); font-size: 13px; margin-top: 4px; }
    .grid { display: grid; grid-template-columns: 340px 1fr; gap: 24px; }
    @media (max-width: 860px) { .grid { grid-template-columns: 1fr; } }
    .panel { background: var(--card); border: 1px solid var(--card-border); border-radius: 12px; padding: 20px; }
    .panel-title { font-size: 12px; font-weight: 800; color: var(--signal); text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
    .peer-card { background: rgba(2, 6, 23, 0.5); border: 1px solid var(--card-border); border-radius: 8px; padding: 14px; margin-bottom: 10px; transition: border 0.2s; }
    .peer-card:hover { border-color: var(--signal); }
    .peer-title { font-size: 14px; font-weight: 700; color: #fff; display: flex; justify-content: space-between; }
    .peer-sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
    .peer-tag { font-size: 11px; color: var(--moss); background: rgba(61, 220, 151, 0.1); padding: 2px 6px; border-radius: 4px; }
    .empty-state { text-align: center; padding: 32px 16px; color: var(--text-muted); font-size: 13px; }
    .alert-card { background: rgba(255, 79, 62, 0.08); border: 1.5px solid var(--ember); border-radius: 10px; padding: 18px; margin-bottom: 14px; animation: pulseIn 0.4s ease-out; }
    @keyframes pulseIn { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
    .alert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
    .alert-badge { background: var(--ember); color: #fff; font-size: 11px; font-weight: 800; padding: 3px 8px; border-radius: 4px; letter-spacing: 0.5px; }
    .alert-time { font-size: 12px; color: var(--text-muted); }
    .alert-msg { font-size: 16px; font-weight: 700; color: #fff; margin-bottom: 12px; line-height: 1.4; }
    .alert-meta { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 8px; background: rgba(0, 0, 0, 0.3); padding: 10px; border-radius: 6px; font-size: 12px; }
    .meta-item { color: var(--text-muted); }
    .meta-item strong { color: #fff; display: block; font-size: 13px; margin-top: 2px; }
    .btn { background: var(--signal); color: #0B0F17; border: none; padding: 10px 16px; border-radius: 6px; font-weight: 700; font-size: 13px; cursor: pointer; transition: opacity 0.2s; width: 100%; margin-top: 12px; }
    .btn:hover { opacity: 0.9; }
    .instructions { background: rgba(30, 41, 59, 0.4); border-radius: 8px; padding: 12px; font-size: 12px; color: var(--text-muted); line-height: 1.5; margin-top: 16px; }
    code { background: #020617; padding: 2px 6px; border-radius: 4px; color: var(--signal); font-family: monospace; }
  </style>
</head>
<body>
  <div class="header">
    <div class="title-group">
      <h1>ResQLink Command Station <span class="badge-live">Live Link</span></h1>
      <div class="sub">${laptopName} • Listening on port ${PORT} (UDP & HTTP)</div>
    </div>
    <div>
      <button class="btn" style="width: auto; margin: 0; background: var(--card-border); color: #fff;" onclick="testBeep()">🔊 Test Audio</button>
    </div>
  </div>

  <div class="grid">
    <!-- Left Column: Connected Devices -->
    <div>
      <div class="panel">
        <div class="panel-title">
          <span>Connected Phones (<span id="peerCount">0</span>)</span>
        </div>
        <div id="peerList">
          <div class="empty-state">
            Searching for Android devices on Wi-Fi or USB...<br><br>
            Make sure your phone and laptop are on the same Wi-Fi, or connect USB cable.
          </div>
        </div>

        <div class="instructions">
          <strong>Testing Tips:</strong><br>
          • <strong>Wi-Fi Direct:</strong> Connect both laptop & phone to same Wi-Fi.<br>
          • <strong>USB Cable:</strong> Run <code>adb reverse tcp:8988 tcp:8988</code> in terminal.
        </div>
      </div>

      <div class="panel" style="margin-top: 20px;">
        <div class="panel-title">Broadcast Alert to Phone</div>
        <input id="testMsg" type="text" placeholder="Custom emergency broadcast message..." style="width: 100%; padding: 10px; border-radius: 6px; border: 1px solid var(--card-border); background: #020617; color: #fff; font-size: 13px;" value="Command Station: Flood barrier breached, evacuate to high ground." />
        <button class="btn" onclick="sendTestAlert()">Broadcast to Connected Phones</button>
      </div>
    </div>

    <!-- Right Column: Live SOS Feed -->
    <div class="panel">
      <div class="panel-title">
        <span>Real-Time Relayed SOS Alerts Feed (<span id="alertCount">0</span>)</span>
        <span style="color: var(--text-muted); font-size: 11px;">Automatic Live Refresh</span>
      </div>
      <div id="alertList">
        <div class="empty-state">
          No emergency SOS alerts received yet.<br><br>
          Press the red <strong>SOS</strong> button on your phone to transmit a live emergency alert across the mesh!
        </div>
      </div>
    </div>
  </div>

  <script>
    const peerList = document.getElementById('peerList');
    const peerCount = document.getElementById('peerCount');
    const alertList = document.getElementById('alertList');
    const alertCount = document.getElementById('alertCount');

    function testBeep() {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = 880;
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.4);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    }

    function renderPeers(peers) {
      peerCount.textContent = peers.length;
      if (peers.length === 0) {
        peerList.innerHTML = '<div class="empty-state">Searching for Android devices on Wi-Fi or USB...<br><br>Open ResQLink on your phone.</div>';
        return;
      }
      peerList.innerHTML = peers.map(p => \`
        <div class="peer-card">
          <div class="peer-title">
            <span>\${p.name}</span>
            <span class="peer-tag">\${p.transport}</span>
          </div>
          <div class="peer-sub">IP: \${p.ip} · Status: Active</div>
        </div>
      \`).join('');
    }

    function renderAlerts(alerts) {
      alertCount.textContent = alerts.length;
      if (alerts.length === 0) {
        alertList.innerHTML = '<div class="empty-state">No emergency SOS alerts received yet.<br><br>Press the <strong>SOS</strong> button on your phone.</div>';
        return;
      }
      alertList.innerHTML = alerts.map(a => \`
        <div class="alert-card">
          <div class="alert-header">
            <span class="alert-badge">EMERGENCY SOS RECEIVED</span>
            <span class="alert-time">\${a.receivedAt || 'Just now'}</span>
          </div>
          <div class="alert-msg">"\${a.message}"</div>
          <div class="alert-meta">
            <div class="meta-item">Source<strong>\${a.source || 'Mesh Link'}</strong></div>
            <div class="meta-item">GPS Fix<strong>\${Number(a.latitude).toFixed(5)}, \${Number(a.longitude).toFixed(5)}</strong></div>
            <div class="meta-item">Sender ID<strong>\${a.userId || 'Citizen'}</strong></div>
            <div class="meta-item">Hops<strong>\${a.hopCount || 0}</strong></div>
          </div>
        </div>
      \`).join('');
    }

    // Connect SSE
    const es = new EventSource('/events');
    es.onmessage = (e) => {
      const data = JSON.parse(e.data);
      if (data.type === 'INIT') {
        renderPeers(data.peers || []);
        renderAlerts(data.alerts || []);
      } else if (data.type === 'PEERS_UPDATED') {
        renderPeers(data.peers || []);
      } else if (data.type === 'NEW_ALERT') {
        testBeep();
        renderAlerts([data.alert, ...(window._alerts || [])]);
      }
    };

    function sendTestAlert() {
      const msg = document.getElementById('testMsg').value;
      fetch('/api/send_test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: msg })
      }).then(() => alert('Broadcast packet sent to all connected phones via UDP!'));
    }
  </script>
</body>
</html>`);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('\n\x1b[32m==============================================================\x1b[0m');
  console.log('\x1b[1m\x1b[37m  RESQLINK LAPTOP RESCUE STATION IS RUNNING!\x1b[0m');
  console.log('\x1b[32m==============================================================\x1b[0m');
  console.log(`\n\x1b[36m  1. Open the Live Rescue Dashboard in your browser:\x1b[0m`);
  console.log(`     \x1b[1m\x1b[33mhttp://localhost:${PORT}\x1b[0m`);
  console.log(`\n\x1b[36m  2. How to connect your Android Phone:\x1b[0m`);
  console.log(`     \x1b[1m• Method A (Wi-Fi):\x1b[0m Keep your phone on the same Wi-Fi.`);
  console.log(`       (Both exchange UDP beacon packets on port ${PORT} automatically)`);
  console.log(`     \x1b[1m• Method B (USB Cable):\x1b[0m Run this in your terminal:`);
  console.log(`       \x1b[1m\x1b[32madb reverse tcp:${PORT} tcp:${PORT}\x1b[0m\n`);
  console.log('\x1b[32m==============================================================\x1b[0m\n');
});

// Try auto adb reverse if adb is installed
exec(`adb reverse tcp:${PORT} tcp:${PORT}`, (err) => {
  if (!err) {
    console.log(`\x1b[32m[ADB Auto-Forward]\x1b[0m Port ${PORT} automatically forwarded over USB cable!`);
  }
});
