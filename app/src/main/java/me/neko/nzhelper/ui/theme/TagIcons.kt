package me.neko.nzhelper.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.Bathroom
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

object TagIcons {

    private val map = mapOf(
        // 分类 / 通用
        "tag" to Icons.Outlined.Tag,
        "folder" to Icons.Outlined.Folder,
        "hash" to Icons.Outlined.Sell,
        "hand" to Icons.Outlined.BackHand,
        "category" to Icons.Outlined.Category,
        "mood" to Icons.Outlined.Mood,
        "place" to Icons.Outlined.Place,
        // 环境
        "map-pin" to Icons.Outlined.Place,
        "bed" to Icons.Outlined.Bed,
        "bed-double" to Icons.Outlined.Bed,
        "sofa" to Icons.Outlined.Weekend,
        "shower-head" to Icons.Outlined.Bathroom,
        "door-closed" to Icons.Outlined.MeetingRoom,
        "briefcase" to Icons.Outlined.Work,
        "building-2" to Icons.Outlined.Apartment,
        // 时间
        "clock" to Icons.Outlined.AccessTime,
        "sunrise" to Icons.Outlined.LightMode,
        "sun" to Icons.Outlined.WbSunny,
        "sunset" to Icons.Outlined.NightsStay,
        "moon" to Icons.Outlined.Bedtime,
        "moon-star" to Icons.Outlined.BedtimeOff,
        "calendar" to Icons.Outlined.CalendarToday,
        "calendar-days" to Icons.Outlined.CalendarMonth,
        // 状态
        "heart-pulse" to Icons.Outlined.Favorite,
        "battery-low" to Icons.Outlined.BatteryAlert,
        "battery-alert" to Icons.Outlined.BatteryAlert,
        "brain" to Icons.Outlined.Psychology,
        "smile" to Icons.Outlined.SentimentSatisfied,
        "meh" to Icons.Outlined.SentimentDissatisfied,
        "flame" to Icons.Outlined.LocalFireDepartment,
        "eye-off" to Icons.Outlined.VisibilityOff,
        "thermometer" to Icons.Outlined.Sick,
        "cloud-fog" to Icons.Outlined.Cloud,
        "leaf" to Icons.Outlined.Eco,
        "party-popper" to Icons.Outlined.Celebration,
        // 行为
        "sparkles" to Icons.Outlined.Celebration,
        "monitor-play" to Icons.Outlined.Movie,
        "droplets" to Icons.Outlined.WaterDrop,
        "dumbbell" to Icons.Outlined.FitnessCenter,
        "wine" to Icons.Outlined.LocalBar,
        // 道具
        "wrench" to Icons.Outlined.Handyman,
        "cup-soda" to Icons.Outlined.Spa,
        "baby" to Icons.Outlined.Face
    )

    /** 提供给「标签管理」选图标用的候选列表。 */
    val candidates: List<String> = listOf(
        "tag", "hash", "folder", "hand", "place", "mood", "category",
        "bed", "sofa", "shower-head", "door-closed", "briefcase", "building-2",
        "clock", "sunrise", "sun", "sunset", "moon", "moon-star",
        "calendar", "calendar-days",
        "heart-pulse", "battery-alert", "brain", "smile", "meh", "flame",
        "eye-off", "thermometer", "cloud-fog", "leaf", "party-popper",
        "monitor-play", "droplets", "dumbbell", "wine", "wrench", "cup-soda", "baby"
    )

    fun iconFor(name: String): ImageVector = map[name.lowercase()] ?: Icons.Outlined.Tag
}
