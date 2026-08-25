package com.resqlink.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache backing the future family-safety-tracking module. */
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val relation: String,
    val lastKnownLat: Double?,
    val lastKnownLng: Double?,
    val status: String,
    val lastUpdatedEpochMillis: Long
)
