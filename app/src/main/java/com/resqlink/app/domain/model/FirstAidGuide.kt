package com.resqlink.app.domain.model

/** A single offline-cached first-aid procedure (e.g. CPR, bleeding control, burns). */
data class FirstAidGuide(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val steps: List<String>,
    val criticalWarning: String? = null,
    val imageUrl: String? = null
)
