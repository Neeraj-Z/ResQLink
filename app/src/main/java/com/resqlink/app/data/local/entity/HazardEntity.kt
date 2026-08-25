package com.resqlink.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached hazard/disaster zone, refreshed opportunistically by [com.resqlink.app.worker.HazardSyncWorker]. */
@Entity(tableName = "hazard_zones")
data class HazardEntity(
    @PrimaryKey val id: String,
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
