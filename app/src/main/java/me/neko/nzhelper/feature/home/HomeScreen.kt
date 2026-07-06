package me.neko.nzhelper.feature.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import me.neko.nzhelper.core.model.Session
import me.neko.nzhelper.core.model.SessionFormState
import me.neko.nzhelper.core.database.SessionRepository
import me.neko.nzhelper.ui.component.dialog.DetailsDialog
import me.neko.nzhelper.feature.home.components.ConfirmResetDialog
import me.neko.nzhelper.feature.home.components.ConfirmStopDialog
import me.neko.nzhelper.feature.home.components.TimelineItem
import me.neko.nzhelper.feature.home.components.TimerCard
import me.neko.nzhelper.core.datastore.CategorySettings
import me.neko.nzhelper.core.service.TimerService
import java.time.LocalDateTime

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(isActive: Boolean = false) {
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
        val autoStart = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            .getBoolean("auto_start_timer", false)

        if (autoStart) {
            ContextCompat.startForegroundService(
                context,
                serviceIntent.apply { action = TimerService.ACTION_START }
            )
        }
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }
    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    val elapsedSeconds by timerService?.elapsedSec?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }
    val isServiceRunning by timerService?.isRunning?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var formState by remember { mutableStateOf(SessionFormState()) }
    val sessions = remember { mutableStateListOf<Session>() }

    LaunchedEffect(isActive) {
        if (isActive) {
            val loaded = SessionRepository.loadSessions(context)
                .sortedByDescending { it.timestamp }
            sessions.clear()
            sessions.addAll(loaded)
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(text = "牛子小助手") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 计时器主区域
                item {
                    TimerCard(
                        elapsedSeconds = elapsedSeconds,
                        isRunning = isServiceRunning,
                        onToggleRun = {
                            if (isServiceRunning) {
                                context.startService(serviceIntent.apply {
                                    action = TimerService.ACTION_PAUSE
                                })
                            } else {
                                ContextCompat.startForegroundService(
                                    context,
                                    serviceIntent.apply { action = TimerService.ACTION_START }
                                )
                            }
                        },
                        onStop = {
                            if (elapsedSeconds > 0) showConfirmDialog = true
                            else Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT).show()
                        },
                        onReset = {
                            if (elapsedSeconds > 0) {
                                showResetConfirmDialog = true
                            } else {
                                Toast.makeText(context, "计时尚未开始", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val now = LocalDateTime.now()
                            formState = SessionFormState(
                                manualYear = now.year,
                                manualMonth = now.monthValue,
                                manualDay = now.dayOfMonth,
                                manualHour = now.hour,
                                manualMinute = now.minute
                            )
                            showManualAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("手动添加记录")
                    }
                }

                if (sessions.isNotEmpty()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val recentSessions = sessions.take(8)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Timeline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "活动时间轴",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "最近 ${recentSessions.size} 次记录",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                recentSessions.forEachIndexed { index, session ->
                                    TimelineItem(
                                        session = session,
                                        isLast = index == recentSessions.lastIndex
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                    alpha = 0.5f
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "(。・ω・。)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "暂无记录，开始第一次记录吧",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
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

    if (showResetConfirmDialog) {
        ConfirmResetDialog(
            onDismiss = { showResetConfirmDialog = false },
            onConfirm = {
                showResetConfirmDialog = false
                context.startService(serviceIntent.apply {
                    action = TimerService.ACTION_RESET
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
            sessions.add(0, session)
            scope.launch { SessionRepository.saveSessions(context, sessions) }

            formState = SessionFormState()
            showDetailsDialog = false
            context.startService(serviceIntent.apply { action = TimerService.ACTION_STOP })
        },
        onDismiss = {
            showDetailsDialog = false
            context.startService(serviceIntent.apply { action = TimerService.ACTION_START })
        },
        locationList = CategorySettings.getLocations(context),
        propsList = CategorySettings.getProps(context),
        moodList = CategorySettings.getMoods(context)
    )

    DetailsDialog(
        show = showManualAddDialog,
        formState = formState,
        onFormStateChange = { formState = it },
        showDurationField = true,
        title = "手动添加记录",
        onConfirm = {
            val duration = formState.manualDurationSeconds
            if (duration <= 0) {
                Toast.makeText(context, "请输入时长", Toast.LENGTH_SHORT).show()
                return@DetailsDialog
            }
            val timestamp = try {
                formState.toLocalDateTime()
            } catch (_: Exception) {
                Toast.makeText(context, "日期时间无效，请重新选择", Toast.LENGTH_SHORT)
                    .show()
                return@DetailsDialog
            }
            val session = Session(
                timestamp = timestamp,
                duration = duration,
                remark = formState.remark,
                location = formState.location,
                watchedMovie = formState.watchedMovie,
                climax = formState.climax,
                rating = formState.rating,
                mood = formState.mood,
                props = formState.props
            )
            sessions.add(0, session)
            scope.launch { SessionRepository.saveSessions(context, sessions) }

            formState = SessionFormState()
            showManualAddDialog = false
        },
        onDismiss = { showManualAddDialog = false },
        locationList = CategorySettings.getLocations(context),
        propsList = CategorySettings.getProps(context),
        moodList = CategorySettings.getMoods(context)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}