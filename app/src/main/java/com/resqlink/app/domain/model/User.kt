package com.resqlink.app.domain.model

enum class UserRole { CITIZEN, VOLUNTEER, RESCUE_TEAM, ADMIN }

/** Authenticated user profile, doubling as the payload for the offline QR-identity module. */
data class User(
    val id: String,
    val name: String,
    val phone: String,
    val role: UserRole,
    val bloodGroup: String?,
    val emergencyContact: String?,
    val medicalNotes: String?
)
