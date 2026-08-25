package com.resqlink.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SosRequestDto(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val severity: String,
    val createdAtEpochMillis: Long
)

@Serializable
data class SosResponseDto(
    val id: String,
    val status: String
)
