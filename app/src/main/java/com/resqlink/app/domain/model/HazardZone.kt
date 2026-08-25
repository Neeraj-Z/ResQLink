package com.resqlink.app.domain.model

enum class HazardType { FLOOD, EARTHQUAKE, FIRE, CYCLONE, LANDSLIDE, INDUSTRIAL, OTHER }

enum class HazardSeverity { LOW, MODERATE, HIGH, EXTREME }

/** A geographically bounded hazard, cached locally so it is visible even offline. */
data class HazardZone(
    val id: String,
    val type: HazardType,
    val severity: HazardSeverity,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val issuedAtEpochMillis: Long,
    val source: String
)
