package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.form.ContraceptionSection
import me.neko.nzhelper.ui.component.form.PartnerGenderSection
import me.neko.nzhelper.ui.component.form.SettingsSection
import me.neko.nzhelper.ui.component.setting.SettingsCard

@Composable
internal fun PartnerPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    val pairMode = SessionMode.PAIR

    SettingsCard {
        item {
            SettingsSection {
                PartnerGenderSection(formState, onFormStateChange)
            }
        }
        item {
            SettingsSection {
                TagSelectCard(
                    title = "体位",
                    loadTags = {
                        TagSettings.getTags(context).filter {
                            it.groupId == TagSettings.GROUP_POSITION && it.appliesTo(pairMode)
                        }
                    },
                    selectedIds = formState.positions,
                    onSelectionChange = {
                        onFormStateChange(formState.copy(positions = it))
                    }
                )
            }
        }
        item {
            SettingsSection {
                TagSelectCard(
                    title = "情趣玩具",
                    loadTags = {
                        TagSettings.getTags(context).filter {
                            it.groupId == TagSettings.LEGACY_GROUP_TOOL && it.appliesTo(pairMode)
                        }
                    },
                    selectedIds = formState.toys,
                    onSelectionChange = {
                        onFormStateChange(formState.copy(toys = it))
                    }
                )
            }
        }
        item {
            SettingsSection {
                ContraceptionSection(formState, onFormStateChange)
            }
        }
        item {
            SettingsSection {
                TagSelectCard(
                    title = "射精方式",
                    loadTags = {
                        TagSettings.getTags(context).filter {
                            it.groupId == TagSettings.GROUP_EJACULATE && it.appliesTo(pairMode)
                        }
                    },
                    selectedIds = setOfNotNull(formState.ejaculation.takeIf { it.isNotBlank() }),
                    singleSelect = true,
                    onSelectionChange = {
                        onFormStateChange(
                            formState.copy(ejaculation = it.firstOrNull() ?: "")
                        )
                    }
                )
            }
        }
    }
}
