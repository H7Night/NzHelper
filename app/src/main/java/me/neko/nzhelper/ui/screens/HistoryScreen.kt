package me.neko.nzhelper.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.JsonParser.parseString
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.neko.nzhelper.NzApplication
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionFormState
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.DetailsDialog
import me.neko.nzhelper.ui.dialog.formatTime
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 数据状态
    val sessions = remember { mutableStateListOf<Session>() }
    val gson = NzApplication.gson
    val listType = object : TypeToken<List<Session>>() {}.type

    // UI 交互状态
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 详情/编辑状态
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    var isViewingDetails by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // 编辑表单状态
    var editFormState by remember { mutableStateOf(SessionFormState()) }

    // 加载数据
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    // 导入 Launcher
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                scope.launch {
                    val imported = parseImportFile(context, it, gson, listType)
                    if (imported.isNotEmpty()) {
                        sessions.clear()
                        sessions.addAll(imported)
                        SessionRepository.saveSessions(context, sessions)
                        Toast.makeText(
                            context,
                            "成功导入 ${imported.size} 条记录",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "导入失败：文件格式不正确或为空", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

    // 导出 Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { writer -> writer.write(gson.toJson(sessions)) }
                    }
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("历史记录") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出数据") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("NzHelper_Export_${System.currentTimeMillis()}.json")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入数据") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "清除全部记录",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(
            androidx.compose.foundation.layout.WindowInsetsSides.Top +
                    androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (sessions.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(
                        items = sessions,
                        key = { it.timestamp }
                    ) { session ->
                        SessionHistoryCard(
                            session = session,
                            onClick = {
                                selectedSession = session
                                isViewingDetails = true
                            },
                            onEdit = {
                                selectedSession = session
                                isEditing = true
                                editFormState = SessionFormState(
                                    remark = session.remark,
                                    location = session.location,
                                    watchedMovie = session.watchedMovie,
                                    climax = session.climax,
                                    rating = session.rating,
                                    mood = session.mood,
                                    props = session.props
                                )
                            },
                            onDelete = {
                                sessions.remove(session)
                                scope.launch { SessionRepository.saveSessions(context, sessions) }
                            }
                        )
                    }
                }
            }
        }
    }

    // 删除全部确认
    if (showClearDialog) {
        ConfirmDialog(
            icon = Icons.Default.Warning,
            title = "清除全部记录",
            message = "此操作不可撤销，确定要删除所有记录吗？",
            confirmText = "全部删除",
            onConfirm = {
                sessions.clear()
                scope.launch { SessionRepository.saveSessions(context, sessions) }
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    // 查看详情弹窗
    if (isViewingDetails && selectedSession != null) {
        SessionDetailDialog(
            session = selectedSession!!,
            onDismiss = {
                isViewingDetails = false
                selectedSession = null
            },
            onEditClick = {
                isViewingDetails = false
                isEditing = true
                editFormState = SessionFormState(
                    remark = selectedSession!!.remark,
                    location = selectedSession!!.location,
                    watchedMovie = selectedSession!!.watchedMovie,
                    climax = selectedSession!!.climax,
                    rating = selectedSession!!.rating,
                    mood = selectedSession!!.mood,
                    props = selectedSession!!.props
                )
            }
        )
    }

    // 编辑弹窗 (复用 DetailsDialog)
    DetailsDialog(
        show = isEditing,
        formState = editFormState,
        onFormStateChange = { editFormState = it },
        onConfirm = {
            val original = selectedSession ?: return@DetailsDialog
            val index = sessions.indexOf(original)
            if (index != -1) {
                val updated = original.copy(
                    remark = editFormState.remark,
                    location = editFormState.location,
                    watchedMovie = editFormState.watchedMovie,
                    climax = editFormState.climax,
                    rating = editFormState.rating,
                    mood = editFormState.mood,
                    props = editFormState.props
                )
                sessions[index] = updated
                scope.launch { SessionRepository.saveSessions(context, sessions) }
            }
            isEditing = false
            selectedSession = null
        },
        onDismiss = {
            isEditing = false
            selectedSession = null
        }
    )
}

// --- 辅助组件 ---

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "(。・ω・。)",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "暂无历史记录",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        formatTime(session.duration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (session.remark.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· ${session.remark}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: Session,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "记录详情",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider()

                DetailItem(
                    "时间",
                    session.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                DetailItem("时长", formatTime(session.duration))
                DetailItem("地点", session.location.ifEmpty { "未记录" })
                DetailItem("备注", session.remark.ifEmpty { "无" })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TagItem("看片", session.watchedMovie)
                    TagItem("发射", session.climax)
                }

                DetailItem("道具", session.props)
                DetailItem("心情", session.mood)
                DetailItem("评分", "%.1f".format(session.rating))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("关闭")
                    }
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("编辑")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // 使用 Modifier 来设置最小宽度，而不是直接传参数
            modifier = Modifier.defaultMinSize(minWidth = 60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TagItem(label: String, isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isActive) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 导入逻辑封装
private suspend fun parseImportFile(
    context: Context,
    uri: Uri,
    gson: Gson,
    listType: java.lang.reflect.Type
): List<Session> = withContext(Dispatchers.IO) {
    val result = mutableListOf<Session>()
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonStr = inputStream.bufferedReader().readText()

            // 尝试解析新格式
            try {
                val list: List<Session> = gson.fromJson(jsonStr, listType)
                result.addAll(list)
                return@withContext result
            } catch (_: Exception) {
                // 新格式失败，尝试旧格式解析 (保留原有逻辑)
                try {
                    val root = parseString(jsonStr).asJsonArray
                    for (elem in root) {
                        if (elem.isJsonArray) {
                            val arr = elem.asJsonArray
                            val timeStr = arr[0].asString
                            val timestamp =
                                LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            val duration = if (arr.size() > 1) arr[1].asInt else 0
                            val remark =
                                if (arr.size() > 2 && !arr[2].isJsonNull) arr[2].asString else ""
                            val location =
                                if (arr.size() > 3 && !arr[3].isJsonNull) arr[3].asString else ""
                            val watchedMovie = if (arr.size() > 4) arr[4].asBoolean else false
                            val climax = if (arr.size() > 5) arr[5].asBoolean else false
                            val rating =
                                if (arr.size() > 6 && !arr[6].isJsonNull) arr[6].asFloat.coerceIn(
                                    0f,
                                    5f
                                ) else 3f
                            val mood =
                                if (arr.size() > 7 && !arr[7].isJsonNull) arr[7].asString else "平静"
                            val props =
                                if (arr.size() > 8 && !arr[8].isJsonNull) arr[8].asString else "手"

                            result.add(
                                Session(
                                    timestamp = timestamp,
                                    duration = duration,
                                    remark = remark,
                                    location = location,
                                    watchedMovie = watchedMovie,
                                    climax = climax,
                                    rating = rating,
                                    mood = mood,
                                    props = props
                                )
                            )
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    result
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    HistoryScreen()
}
