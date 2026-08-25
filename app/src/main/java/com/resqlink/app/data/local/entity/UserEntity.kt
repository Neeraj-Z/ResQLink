package com.resqlink.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Locally persisted profile -- also the payload source for the offline QR-identity module. */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val role: String,
    val bloodGroup: String?,
    val emergencyContact: String?,
    val medicalNotes: String?
)
