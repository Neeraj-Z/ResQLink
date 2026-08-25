package com.resqlink.app.presentation.sos

import android.Manifest
import android.os.Build

/**
 * Runtime permissions required to run the Nearby Connections mesh relay.
 * The exact set differs by API level (Android 12 introduced the granular
 * BLUETOOTH_ADVERTISE/CONNECT/SCAN permissions; Android 13 added
 * NEARBY_WIFI_DEVICES) -- see the corresponding maxSdkVersion-gated legacy
 * entries in AndroidManifest.xml.
 */
object MeshPermissions {
    val required: List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        // Pre-Android 13 BLE/Wi-Fi Direct discovery still requires location
        // permission at the OS level, independent of ResQLink's own use of
        // location for the SOS coordinates themselves.
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
