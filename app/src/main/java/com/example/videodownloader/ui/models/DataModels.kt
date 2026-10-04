package com.example.videodownloader.ui.models

import androidx.compose.runtime.Immutable

@Immutable
data class CleanQualityOption(
    val id: String,
    val tag: String,
    val title: String,
    val subtitle: String
)

@Immutable
data class ThreadOption(
    val threads: Int,
    val title: String,
    val subtitle: String,
    val badge: String
)

@Immutable
data class AutoDismissOption(
    val millis: Long,
    val title: String,
    val subtitle: String,
    val badge: String
)
