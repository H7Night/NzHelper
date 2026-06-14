package me.neko.nzhelper.ui.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.data.Session
import me.neko.nzhelper.data.SessionFormState
import me.neko.nzhelper.data.SessionRepository
import me.neko.nzhelper.ui.dialog.DetailsDialog
import me.neko.nzhelper.ui.dialog.formatTime
import me.neko.nzhelper.ui.service.TimerService
import java.time.LocalDateTime

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 绑定 Service
    val serviceIntent = remember { Intent(context, TimerService::class.java) }
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                timerService = (binder as TimerService.LocalBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    // 启动并绑定服务
    LaunchedEffect(Unit) {
        ContextCompat.startForegroundService(
            context,
            serviceIntent.apply { action = TimerService.ACTION_START }
        )
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    // 订阅 Service 状态
    val elapsedSeconds by timerService
        ?.elapsedSec
        ?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }

    val isServiceRunning by timerService
        ?.isRunning
        ?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    var formState by remember { mutableStateOf(SessionFormState()) }

    val sessions = remember { mutableStateListOf<Session>() }

    // 加载历史
    LaunchedEffect(Unit) {
        val loaded = SessionRepository.loadSessions(context)
        sessions.clear()
        sessions.addAll(loaded)
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = "牛子小助手") },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(
            androidx.compose.foundation.layout.WindowInsetsSides.Top + androidx.compose.foundation.layout.WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // 计时器主区域
                item {
                    TimerCard(
                        elapsedSeconds = elapsedSeconds,
                        isRunning = isServiceRunning,
                        onToggleRun = {
                            val action =
                                if (isServiceRunning) TimerService.ACTION_PAUSE else TimerService.ACTION_START
                            context.startService(serviceIntent.apply { this.action = action })
                        },
                        onStop = {
                            if (elapsedSeconds > 0) showConfirmDialog = true
                            else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 历史记录列表
                if (sessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "近期记录",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(sessions.reversed()) { session ->
                        SessionItem(session = session)
                    }
                }
            }

            if (showConfirmDialog) {
                ConfirmStopDialog(
                    onDismiss = { showConfirmDialog = false },
                    onConfirm = {
                        showConfirmDialog = false
                        showDetailsDialog = true
                        context.startService(serviceIntent.apply {
                            action = TimerService.ACTION_PAUSE
                        })
                    }
                )
            }

            DetailsDialog(
                show = showDetailsDialog,
                formState = formState,
                onFormStateChange = { formState = it },
                onConfirm = {
                    val now = LocalDateTime.now()
                    val session = Session(
                        timestamp = now,
                        duration = elapsedSeconds,
                        remark = formState.remark,
                        location = formState.location,
                        watchedMovie = formState.watchedMovie,
                        climax = formState.climax,
                        rating = formState.rating,
                        mood = formState.mood,
                        props = formState.props
                    )
                    sessions.add(session)
                    scope.launch {
                        SessionRepository.saveSessions(context, sessions)
                    }

                    formState = SessionFormState()
                    showDetailsDialog = false
                    context.startService(serviceIntent.apply { action = TimerService.ACTION_STOP })
                },
                onDismiss = {
                    showDetailsDialog = false
                    context.startService(serviceIntent.apply { action = TimerService.ACTION_START })
                }
            )
        }
    }
}

@Composable
private fun ConfirmStopDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.PauseCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("结束了吗？") },
        text = { Text("要结束本次记录并填写详情吗？", textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("结束记录") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("继续") }
        }
    )
}

/**
 * 计时器卡片 UI 组件
 */
@Composable
private fun TimerCard(
    elapsedSeconds: Int,
    isRunning: Boolean,
    onToggleRun: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Text(
                    text = "记录新的手艺活",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatTime(elapsedSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放/暂停 FAB
                    FloatingActionButton(
                        onClick = onToggleRun,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isRunning) "暂停" else "开始",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 停止 FAB
                    FloatingActionButton(
                        onClick = onStop,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "结束",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 历史记录条目
 */
@SuppressLint("DefaultLocale")
@Composable
private fun SessionItem(session: Session) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${session.timestamp.monthValue}月${session.timestamp.dayOfMonth}日 ${session.timestamp.hour}:${
                        String.format(
                            "%02d",
                            session.timestamp.minute
                        )
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatTime(session.duration),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (session.remark.isNotEmpty()) {
                    Text(
                        text = session.remark,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = session.rating.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}