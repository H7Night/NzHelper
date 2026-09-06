package me.neko.nzhelper.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.core.datastore.ChartVisibilitySettings
import me.neko.nzhelper.ui.component.ReorderableColumn
import me.neko.nzhelper.ui.component.setting.SettingsCard
import me.neko.nzhelper.ui.component.setting.SettingsItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChartManageScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var orderedCharts by remember {
        mutableStateOf(ChartVisibilitySettings.getOrderedCharts(context))
    }

    val visibilityStates = remember {
        mutableStateMapOf<ChartVisibilitySettings.Chart, Boolean>().apply {
            ChartVisibilitySettings.Chart.entries.forEach { chart ->
                put(chart, ChartVisibilitySettings.isVisible(context, chart))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("统计卡片管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "拖动左侧图标可调整卡片顺序，开关控制是否在统计页展示",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = {
                        orderedCharts = ChartVisibilitySettings.Chart.entries.toList()
                        ChartVisibilitySettings.saveOrder(context, orderedCharts)
                        ChartVisibilitySettings.Chart.entries.forEach { chart ->
                            visibilityStates[chart] = chart.defaultVisible
                            ChartVisibilitySettings.setVisible(context, chart, chart.defaultVisible)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("恢复默认")
                    }
                }
            }
            ReorderableColumn(
                items = orderedCharts,
                keyOf = { it.key },
                onReorder = { newOrder -> orderedCharts = newOrder },
                onCommit = { ChartVisibilitySettings.saveOrder(context, orderedCharts) },
                gap = 4.dp
            ) { chart, dragHandle, _ ->
                SettingsCard {
                    item {
                        SettingsItem(
                            icon = Icons.Filled.DragHandle,
                            leadingModifier = dragHandle,
                            title = chart.label,
                            subtitle = chart.description,
                            onClick = {
                                val checked = !(visibilityStates[chart] ?: chart.defaultVisible)
                                visibilityStates[chart] = checked
                                ChartVisibilitySettings.setVisible(context, chart, checked)
                            },
                            trailingContent = {
                                Switch(
                                    checked = visibilityStates[chart] ?: chart.defaultVisible,
                                    onCheckedChange = { checked ->
                                        visibilityStates[chart] = checked
                                        ChartVisibilitySettings.setVisible(context, chart, checked)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChartManageScreenPreview() {
    ChartManageScreen(onBack = {})
}
