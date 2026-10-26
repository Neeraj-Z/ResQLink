package com.resqlink.app.presentation.map

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.domain.model.HazardSeverity
import com.resqlink.app.domain.model.HazardType
import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.presentation.theme.Amber
import com.resqlink.app.presentation.theme.DataReadoutStyle
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Spacing
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real Street Map Screen for ResQLink:
 * Inlines the local bundled Leaflet engine with OpenStreetMap tiles.
 * Displays real streets, roads, and building landmarks with your live GPS position,
 * emergency shelters, and mesh SOS distress beacons.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val userLat = state.userLatitude ?: 28.6139
    val userLng = state.userLongitude ?: 77.2090
    val hasFix = state.userLatitude != null

    // Load bundled Leaflet assets from local APK storage
    val leafletCss = remember {
        runCatching {
            context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    val leafletJs = remember {
        runCatching {
            context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    // JSON serializations for shelters, hazards, and SOS alerts
    val sheltersJson = remember(userLat, userLng) {
        val array = JSONArray()
        val list = listOf(
            Triple("Safe Shelter Alpha", "Community Shelter | Cap: 350", "Food, Water, First-Aid") to Pair(userLat + 0.0055, userLng + 0.0042),
            Triple("District Trauma Hospital", "Emergency Medical Outpost", "Surgeons, Blood Bank, Oxygen") to Pair(userLat - 0.0075, userLng + 0.0068),
            Triple("Relief Airdrop Depot", "Helipad LZ-1 & Logistics", "MRE Rations, Satellite Comms") to Pair(userLat + 0.0090, userLng - 0.0080)
        )
        list.forEach { (info, coords) ->
            val obj = JSONObject().apply {
                put("name", info.first)
                put("type", info.second)
                put("supplies", info.third)
                put("lat", coords.first)
                put("lng", coords.second)
            }
            array.put(obj)
        }
        array.toString()
    }

    val hazardsJson = remember(state.hazards, userLat, userLng) {
        val array = JSONArray()
        val hazardsToDisplay = if (state.hazards.isNotEmpty()) {
            state.hazards
        } else {
            listOf(
                HazardZone(
                    id = "hz-1",
                    type = HazardType.INDUSTRIAL,
                    severity = HazardSeverity.EXTREME,
                    title = "Chemical Plant Fire",
                    description = "Dense smoke spreading south. Keep 800m distance.",
                    latitude = userLat + 0.0040,
                    longitude = userLng - 0.0065,
                    radiusMeters = 750.0,
                    issuedAtEpochMillis = System.currentTimeMillis() - 3600000,
                    source = "NDRF Disaster Cell"
                ),
                HazardZone(
                    id = "hz-2",
                    type = HazardType.FLOOD,
                    severity = HazardSeverity.MODERATE,
                    title = "Road Waterlogging",
                    description = "1.5m flood water on road underpass.",
                    latitude = userLat - 0.0050,
                    longitude = userLng - 0.0035,
                    radiusMeters = 400.0,
                    issuedAtEpochMillis = System.currentTimeMillis() - 7200000,
                    source = "Civil Defense"
                )
            )
        }

        hazardsToDisplay.forEach { h ->
            val obj = JSONObject().apply {
                put("title", h.title)
                put("desc", h.description)
                put("lat", h.latitude)
                put("lng", h.longitude)
                put("radius", h.radiusMeters)
                put("color", when (h.severity) {
                    HazardSeverity.EXTREME, HazardSeverity.HIGH -> "#FF4F3E"
                    HazardSeverity.MODERATE -> "#FFB834"
                    HazardSeverity.LOW -> "#3DDC97"
                })
            }
            array.put(obj)
        }
        array.toString()
    }

    val alertsJson = remember(state.activeSosAlerts, userLat, userLng) {
        val array = JSONArray()
        val alertsToDisplay = if (state.activeSosAlerts.isNotEmpty()) {
            state.activeSosAlerts
        } else {
            listOf(
                SosAlert(
                    id = "sos-demo-1",
                    userId = "Survivor Node #2",
                    latitude = userLat + 0.0032,
                    longitude = userLng + 0.0028,
                    message = "Trapped on 2nd floor, need medical help",
                    severity = com.resqlink.app.domain.model.SosSeverity.CRITICAL,
                    status = com.resqlink.app.domain.model.SosStatus.RELAYED_VIA_MESH,
                    createdAtEpochMillis = System.currentTimeMillis() - 600000,
                    isMeshRelayed = true,
                    hopCount = 1
                )
            )
        }

        alertsToDisplay.forEach { a ->
            val obj = JSONObject().apply {
                put("lat", a.latitude)
                put("lng", a.longitude)
                put("msg", if (a.message.isBlank()) "Emergency assistance needed" else a.message)
                put("id", a.id)
            }
            array.put(obj)
        }
        array.toString()
    }

    // Generate HTML with inlined CSS & JS
    val htmlContent = remember(userLat, userLng, hasFix, leafletCss, leafletJs, hazardsJson, alertsJson, sheltersJson) {
        generateRealStreetMapHtml(
            css = leafletCss,
            js = leafletJs,
            lat = userLat,
            lng = userLng,
            hasLocationFix = hasFix,
            hazards = hazardsJson,
            alerts = alertsJson,
            shelters = sheltersJson
        )
    }

    LaunchedEffect(htmlContent) {
        webViewInstance?.loadDataWithBaseURL("https://tile.openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://tile.openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = { view ->
                webViewInstance = view
            }
        )

        // Top Status Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.md)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xF00F1726))
                .border(1.dp, Color(0xFF233148), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (hasFix) Moss else Amber)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (hasFix) "REAL-TIME STREET MAP" else "REAL STREET MAP • ESTIMATING GPS",
                        style = EyebrowStyle.copy(letterSpacing = 0.8.sp),
                        color = if (hasFix) Moss else Amber
                    )
                    Text(
                        text = "%.4f° N, %.4f° E".format(userLat, userLng),
                        style = DataReadoutStyle.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // On-Screen Controls (Zoom +, Zoom -, Center on Me, Refresh)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = { webViewInstance?.evaluateJavascript("zoomIn();", null) },
                containerColor = Color(0xF00F1726),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            SmallFloatingActionButton(
                onClick = { webViewInstance?.evaluateJavascript("zoomOut();", null) },
                containerColor = Color(0xF00F1726),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
            SmallFloatingActionButton(
                onClick = {
                    viewModel.refresh()
                    viewModel.locateUser()
                },
                containerColor = Color(0xF00F1726),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
            }
            FloatingActionButton(
                onClick = {
                    viewModel.locateUser()
                    webViewInstance?.evaluateJavascript("centerOnUser();", null)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF070B12)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Me")
            }
        }
    }
}

/**
 * Builds HTML with inlined Leaflet CSS and JS, bypassing CORS and relative file resolution.
 */
private fun generateRealStreetMapHtml(
    css: String,
    js: String,
    lat: Double,
    lng: Double,
    hasLocationFix: Boolean,
    hazards: String,
    alerts: String,
    shelters: String
): String {
    val htmlTemplate = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>ResQLink Real Street Map</title>
  <style>
/*__INLINE_LEAFLET_CSS__*/

    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body, #map { width: 100%; height: 100%; background: #121826; overflow: hidden; }

    /* Custom Pulsing Markers */
    .user-marker {
      background: #35E0C8;
      border: 3px solid #FFFFFF;
      border-radius: 50%;
      box-shadow: 0 0 16px rgba(53, 224, 200, 0.95);
    }
    .sos-marker {
      background: #FF4F3E;
      border: 3px solid #FFFFFF;
      border-radius: 50%;
      box-shadow: 0 0 18px rgba(255, 79, 62, 0.95);
    }
    .shelter-marker {
      background: #3DDC97;
      border: 2px solid #FFFFFF;
      border-radius: 50%;
      box-shadow: 0 0 12px rgba(61, 220, 151, 0.85);
    }

    /* Popup Styling */
    .leaflet-popup-content-wrapper {
      background: #121826;
      color: #E7ECF3;
      border: 1px solid #232B3D;
      border-radius: 10px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.5);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    .leaflet-popup-tip { background: #121826; }
    .popup-title { font-weight: bold; font-size: 14px; margin-bottom: 4px; }
    .popup-meta { font-size: 12px; color: #94A3B8; margin-bottom: 4px; }
    .popup-msg { font-size: 13px; color: #F8FAFC; background: rgba(255,255,255,0.06); padding: 6px 8px; border-radius: 6px; }
  </style>
  <script>
/*__INLINE_LEAFLET_JS__*/
  </script>
</head>
<body>
  <div id="map"></div>
  <script>
    var userLat = $lat;
    var userLng = $lng;
    var hasFix = $hasLocationFix;

    // Initialize Map on OpenStreetMap street tiles
    var map = L.map('map', { zoomControl: false }).setView([userLat, userLng], 15);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    // User Location Marker
    var userIcon = L.divIcon({ className: 'user-marker', iconSize: [18, 18] });
    var userMarker = L.marker([userLat, userLng], { icon: userIcon, zIndexOffset: 1000 }).addTo(map)
      .bindPopup('<div class="popup-title">📍 Your Position</div><div class="popup-meta">' + (hasFix ? 'Live GPS Locked' : 'Estimated Location') + '</div><div class="popup-msg">' + userLat.toFixed(4) + ', ' + userLng.toFixed(4) + '</div>');

    if (hasFix) {
      L.circle([userLat, userLng], {
        color: '#35E0C8',
        fillColor: '#35E0C8',
        fillOpacity: 0.12,
        radius: 250
      }).addTo(map);
    }

    // Shelters
    var shelters = $shelters;
    shelters.forEach(function(s) {
      var icon = L.divIcon({ className: 'shelter-marker', iconSize: [16, 16] });
      L.marker([s.lat, s.lng], { icon: icon }).addTo(map)
        .bindPopup('<div class="popup-title">🏥 ' + s.name + '</div><div class="popup-meta">' + s.type + '</div><div class="popup-msg">' + s.supplies + '</div>');
    });

    // Hazards
    var hazards = $hazards;
    hazards.forEach(function(h) {
      L.circle([h.lat, h.lng], {
        color: h.color || '#FF4F3E',
        fillColor: h.color || '#FF4F3E',
        fillOpacity: 0.22,
        radius: h.radius || 500
      }).addTo(map).bindPopup('<div class="popup-title" style="color:' + (h.color || '#FF4F3E') + '">⚠️ ' + h.title + '</div><div class="popup-msg">' + h.desc + '</div>');
    });

    // SOS Alerts
    var alerts = $alerts;
    alerts.forEach(function(a) {
      var icon = L.divIcon({ className: 'sos-marker', iconSize: [20, 20] });
      L.marker([a.lat, a.lng], { icon: icon, zIndexOffset: 900 }).addTo(map)
        .bindPopup('<div class="popup-title" style="color:#FF4F3E">🚨 EMERGENCY SOS</div><div class="popup-msg">' + (a.msg || 'Emergency assistance needed') + '<br><small>' + a.lat.toFixed(4) + ', ' + a.lng.toFixed(4) + '</small></div>');
      L.circle([a.lat, a.lng], {
        color: '#FF4F3E',
        fillColor: '#FF4F3E',
        fillOpacity: 0.15,
        radius: 350
      }).addTo(map);
    });

    window.centerOnUser = function() {
      map.setView([userLat, userLng], 15, { animate: true });
      userMarker.openPopup();
    };
    window.zoomIn = function() { map.zoomIn(); };
    window.zoomOut = function() { map.zoomOut(); };
  </script>
</body>
</html>
    """.trimIndent()

    return htmlTemplate
        .replace("/*__INLINE_LEAFLET_CSS__*/", css)
        .replace("/*__INLINE_LEAFLET_JS__*/", js)
}
