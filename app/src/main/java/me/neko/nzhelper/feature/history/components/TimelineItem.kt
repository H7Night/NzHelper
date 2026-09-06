package me.neko.nzhelper.feature.history.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.TagDef
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.model.allTagIds
import me.neko.nzhelper.core.model.sessionMode
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.ui.component.setting.SettingsCornerRadius
import me.neko.nzhelper.ui.component.tag.TagChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    session: Session,
    tagDefs: Map<String, TagDef>,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val today = remember { LocalDate.now() }
    val sessionDate = remember(session.timestamp) { session.timestamp.toLocalDate() }
    val dayDiff = remember(sessionDate) { ChronoUnit.DAYS.between(sessionDate, today).toInt() }
    val isToday = dayDiff == 0

    val relativeDate = remember(dayDiff, sessionDate) {
        when (dayDiff) {
            0 -> "今天"
            1 -> "昨天"
            2 -> "前天"
            else -> sessionDate.format(
                DateTimeFormatter.ofPattern(
                    "yyyy年M月d日 EEE",
                    Locale.CHINA
                )
            )
        }
    }
    val timeText = remember(session.timestamp) {
        session.timestamp.format(DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA))
    }

    val resolvedTags = remember(
        session.tagIds, session.locations, session.moods, session.positions, session.toys,
        session.ejaculation, session.tagSnapshots, tagDefs
    ) {
        session.allTagIds().mapNotNull { id ->
            tagDefs[id] ?: session.tagSnapshots.orEmpty().firstOrNull { it.id == id }
        }.take(5)
    }

    val showActions = onDelete != null
    val largeRadius = 16.dp
    val smallRadius = 4.dp

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius
        else if (isFirst) largeRadius else smallRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "timelineItemTopRadius"
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (pressed) SettingsCornerRadius
        else if (isLast) largeRadius else smallRadius,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "timelineItemBottomRadius"
    )
    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onClick()
                        }
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = relativeDate,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isToday) primary else onSurface,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleSmall,
                        color = onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (showActions) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            val durationColor = if (isToday) primary else onSurfaceVariant
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = durationColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatTime(session.duration),
                        style = MaterialTheme.typography.labelLarge,
                        color = durationColor
                    )
                }
                if (session.rating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                session.rating < 3.0f -> Icons.Outlined.SentimentDissatisfied
                                session.rating == 3.0f -> Icons.Outlined.SentimentNeutral
                                else -> Icons.Outlined.SentimentVerySatisfied
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "%.1f".format(session.rating),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (session.climaxCount > 0) {
                    TagChip(
                        name = "高潮×${session.climaxCount}",
                        color = "rose",
                        icon = null,
                        small = true
                    )
                }
                if (session.sessionMode() == SessionMode.PAIR) {
                    TagChip(
                        name = "双人",
                        color = "pink",
                        icon = null,
                        small = true
                    )
                    if (session.partnerClimaxCount > 0) {
                        TagChip(
                            name = "对方×${session.partnerClimaxCount}",
                            color = "violet",
                            icon = null,
                            small = true
                        )
                    }
                }
            }

            if (resolvedTags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    resolvedTags.forEach { tag ->
                        TagChip(
                            name = tag.name,
                            color = tag.color,
                            icon = tag.icon,
                            small = true
                        )
                    }
                }
            }

            if (session.remark.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Text(
                        text = session.remark,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}