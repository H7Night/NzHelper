package me.neko.nzhelper.feature.addrecord.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import me.neko.nzhelper.core.datastore.TagSettings
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.model.SessionMode
import me.neko.nzhelper.ui.component.form.SettingsSection
import me.neko.nzhelper.ui.component.setting.SettingsCard

@Composable
internal fun SoloDetailPage(
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val mode = SessionMode.fromKey(formState.mode)

    SettingsCard {
        item {
            SettingsSection {
                SoloGroupTagCard("身体", TagSettings.GROUP_BODY, mode, formState, onFormStateChange)
            }
        }
        item {
            SettingsSection {
                SoloGroupTagCard(
                    "行为",
                    TagSettings.LEGACY_GROUP_ACT,
                    mode,
                    formState,
                    onFormStateChange
                )
            }
        }
        item {
            SettingsSection {
                SoloGroupTagCard(
                    "道具",
                    TagSettings.LEGACY_GROUP_TOOL,
                    mode,
                    formState,
                    onFormStateChange
                )
            }
        }
    }
}

@Composable
private fun SoloGroupTagCard(
    title: String,
    groupId: String,
    mode: SessionMode,
    formState: SessionFormState,
    onFormStateChange: (SessionFormState) -> Unit
) {
    val context = LocalContext.current
    val tags = TagSettings.getTags(context).filter {
        it.groupId == groupId && it.appliesTo(mode)
    }
    val ids = tags.map { it.id }.toSet()

    TagSelectCard(
        title = title,
        loadTags = { tags },
        selectedIds = formState.tagIds.intersect(ids),
        onSelectionChange = { selected ->
            onFormStateChange(
                formState.copy(
                    tagIds = formState.tagIds - ids + selected
                )
            )
        }
    )
}
