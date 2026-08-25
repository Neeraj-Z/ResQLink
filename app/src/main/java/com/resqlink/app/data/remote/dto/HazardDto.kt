package com.resqlink.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HazardDto(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val issuedAtEpochMillis: Long,
    val source: String
)
