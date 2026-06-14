package me.neko.nzhelper.data

import java.time.LocalDateTime

data class Session(
    val timestamp: LocalDateTime,
    val duration: Int,
    val remark: String,
    val location: String,
    val watchedMovie: Boolean,
    val climax: Boolean,
    val rating: Float,
    val mood: String,
    val props: String
)