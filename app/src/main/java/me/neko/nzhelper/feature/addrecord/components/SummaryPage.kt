package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.Contraception
import me.neko.nzhelper.core.model.PartnerGender
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.core.util.formatTime
import me.neko.nzhelper.feature.addrecord.AddRecordFlow
import me.neko.nzhelper.ui.component.form.SettingsSection
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.wizard.SummaryRow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun SummaryPage(
    flow: AddRecordFlow,
    formState: SessionFormState,
    elapsedSeconds: Int,
    editSession: Session?
) {
    val context = LocalContext.current
    val isPair = SessionMode.fromKey(formState.mode).isPair

    val timestamp = when (flow) {
        AddRecordFlow.EDIT -> try {
            formState.toLocalDateTime()
        } catch (_: Exception) {
            editSession?.timestamp
        }

        AddRecordFlow.MANUAL -> try {
            formState.toLocalDateTime()
        } catch (_: Exception) {
            null
        }

        AddRecordFlow.TIMER -> LocalDateTime.now()
    }
    val duration = when (flow) {
        AddRecordFlow.EDIT -> formState.manualDurationSeconds
        AddRecordFlow.MANUAL -> formState.manualDurationSeconds
        AddRecordFlow.TIMER -> elapsedSeconds
    }

    val categories = remember { TagSettings.getCategories(context) }
    val categoryName = categories.firstOrNull { it.id == formState.categoryId }?.name
        ?: formState.categoryId
    val tags = remember { TagSettings.getTags(context) }
    fun tagName(id: String): String = tags.firstOrNull { it.id == id }?.name ?: id
    val tagText = formState.tagIds
        .mapNotNull { id -> tags.firstOrNull { it.id == id }?.name }
        .joinToString("、")
        .ifBlank { "无" }

    SettingsCard {
        item {
            SettingsSection {
                SummaryRow("模式", SessionMode.fromKey(formState.mode).label)
            }
        }
        if (timestamp != null) {
            item {
                SettingsSection {
                    SummaryRow(
                        "日期时间",
                        timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    )
                }
            }
        }
        item {
            SettingsSection {
                SummaryRow("时长", formatTime(duration))
            }
        }
        item {
            SettingsSection {
                SummaryRow("分类", categoryName)
            }
        }
        item {
            SettingsSection {
                SummaryRow("标签", tagText)
            }
        }
        item {
            SettingsSection {
                SummaryRow("评分", "%.1f".format(formState.rating))
            }
        }
        item {
            SettingsSection {
                SummaryRow(
                    "高潮",
                    if (isPair) {
                        "我 ${formState.climaxCount} 次 · 对方 ${formState.partnerClimaxCount} 次"
                    } else {
                        "${formState.climaxCount} 次"
                    }
                )
            }
        }
        if (formState.locations.isNotEmpty()) {
            item {
                SettingsSection {
                    SummaryRow("地点", formState.locations.joinToString("、") { tagName(it) })
                }
            }
        }
        if (formState.moods.isNotEmpty()) {
            item {
                SettingsSection {
                    SummaryRow("情绪", formState.moods.joinToString("、") { tagName(it) })
                }
            }
        }
        if (isPair) {
            if (formState.partners.isNotEmpty()) {
                item {
                    SettingsSection {
                        SummaryRow("伴侣", formState.partners.joinToString("、") { tagName(it) })
                    }
                }
            }
            if (formState.initiator.isNotBlank()) {
                item {
                    SettingsSection {
                        SummaryRow("发起者", formState.initiator)
                    }
                }
            }
            item {
                SettingsSection {
                    SummaryRow(
                        "对方性别",
                        PartnerGender.fromKey(formState.partnerGender)?.label ?: "未设置"
                    )
                }
            }
            if (formState.partnerName.isNotBlank()) {
                item {
                    SettingsSection {
                        SummaryRow("对方昵称", formState.partnerName)
                    }
                }
            }
            if (formState.positions.isNotEmpty()) {
                item {
                    SettingsSection {
                        SummaryRow("体位", formState.positions.joinToString("、") { tagName(it) })
                    }
                }
            }
            if (formState.toys.isNotEmpty()) {
                item {
                    SettingsSection {
                        SummaryRow("情趣玩具", formState.toys.joinToString("、") { tagName(it) })
                    }
                }
            }
            item {
                SettingsSection {
                    SummaryRow("避孕措施", Contraception.fromKey(formState.contraception).label)
                }
            }
            if (formState.ejaculation.isNotBlank()) {
                item {
                    SettingsSection {
                        SummaryRow("射精方式", tagName(formState.ejaculation))
                    }
                }
            }
        }
        if (formState.remark.isNotBlank()) {
            item {
                SettingsSection {
                    SummaryRow("备注", formState.remark)
                }
            }
        }
    }
}
