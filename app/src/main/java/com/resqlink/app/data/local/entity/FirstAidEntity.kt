package com.resqlink.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.resqlink.app.data.local.Converters

/**
 * First-aid content bundled/cached fully offline -- this must never depend on
 * network availability, since it is often needed exactly when connectivity is down.
 */
@Entity(tableName = "first_aid_guides")
@TypeConverters(Converters::class)
data class FirstAidEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val steps: List<String>,
    val criticalWarning: String?,
    val imageUrl: String?
)
