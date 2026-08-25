package com.resqlink.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table for SOS alerts. This is the source of truth on-device; the app
 * must remain fully usable (create + view SOS history) with zero connectivity.
 */
@Entity(tableName = "sos_alerts")
data class SosEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val severity: String,
    val status: String,
    val createdAtEpochMillis: Long,
    val isMeshRelayed: Boolean = false,
    val isSyncedToServer: Boolean = false,
    val hopCount: Int = 0
)
