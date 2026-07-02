package me.neko.nzhelper.ui.util

import java.util.Locale

fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append(String.format(Locale.getDefault(), "%02d:", hours))
        append(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds))
    }
}