package com.resqlink.app.domain.model

enum class SafetyStatus { SAFE, NEEDS_HELP, UNKNOWN }

/** Future-ready model for the family/group safety-tracking module. */
data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String,
    val lastKnownLat: Double?,
    val lastKnownLng: Double?,
    val status: SafetyStatus,
    val lastUpdatedEpochMillis: Long
)
