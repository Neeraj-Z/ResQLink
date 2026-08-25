package com.resqlink.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FirstAidDto(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val steps: List<String>,
    val criticalWarning: String? = null,
    val imageUrl: String? = null
)
