package com.example.foregroundclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foregroundclock.MyForegroundService.Companion.ONE_DAY_IN_MILLIS
import com.example.foregroundclock.ui.theme.ForegroundClockTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.res.stringResource

lateinit var context: Context
class MainActivity : ComponentActivity() {
    val vm: MainVM by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainVM(application) as T
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this.application

        enableEdgeToEdge()
        setContent {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            } else {
                startTimerService()
            }

            LaunchedEffect(Unit) {
                vm.createEvent.collectLatest {
                    // Check for notification permission on Android 13 and above

                    createAlarm(it)
                }

            }

            LaunchedEffect(Unit) {
                vm.deleteEvent.collectLatest {
                    // Check for notification permission on Android 13 and above

                    deleteAlarm(it)
                }

            }

            LaunchedEffect(Unit) {
                vm.repeatNowEvent.collectLatest {
                    // Check for notification permission on Android 13 and above

                    repeatNowAlarm(it)
                }

            }

            val events = vm.events.collectAsState()
            val showDialog = vm.showDialog.collectAsState()

            ForegroundClockTheme {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "⏰",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.channel_alarm_name),
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = vm::onFABClicked,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add"
                            )
                        }
                    }
                ) { innerPadding ->
                    MainPage(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        events = events.value,
                        onEventRepeat = vm::onRepeatClicked,
                        onEventDelete = { eventId ->
                            val event = events.value.find { it.id == eventId }
                            event?.let {
                                vm.onDelete(eventId, it.name)
                            }
                        },
                        viewModel = vm
                    )
                }

                if (showDialog.value) {
                    MyDialog(vm::onDialogClosed)
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTimerService()
        }
    }

    private fun startTimerService() {
        //Really need this, otherwise alarm will not come if App was closed (not just background , but swiped away)
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun createAlarm(event: EventCreateForAlarm) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        intent.putExtra("test", event.name)
        intent.putExtra("testid", event.id)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val repeatInMillis = event.repeatDays * ONE_DAY_IN_MILLIS

        // Schedule the alarm to repeat every 5 days
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + repeatInMillis,
            repeatInMillis,
            pendingIntent
        )
    }

    private fun deleteAlarm(id: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the alarm to repeat every 5 days
        alarmManager.cancel(
            pendingIntent
        )
    }

    private fun repeatNowAlarm(event: EventCreateForAlarm) {
        deleteAlarm(event.id)
        createAlarm(event)
    }

}

@Composable
fun MainPage(
    events: List<EventView>, 
    onEventRepeat: (Int) -> Unit, 
    onEventDelete: (Int) -> Unit, 
    viewModel: MainVM,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(events) {
                EventView(it, onEventRepeat, onEventDelete)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun EventView(event: EventView, onRepeat: (Int) -> Unit, onDelete: (Int) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = event.nextRepeat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { onRepeat(event.id) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.cd_repeat_event),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                FilledTonalIconButton(
                    onClick = { onDelete(event.id) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_event),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDialog(
    onClick: (EventCreate?) -> Unit
) {
    val text1 = remember { mutableStateOf("") }
    val text2 = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onClick(null) },
        title = { 
            Text(
                stringResource(R.string.dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = text1.value,
                    onValueChange = { text1.value = it },
                    label = { Text(stringResource(R.string.dialog_event_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = text2.value,
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            text2.value = it
                        }
                    },
                    label = { Text(stringResource(R.string.dialog_days_until)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (text1.value.isNotEmpty() && text2.value.isNotEmpty()) {
                        onClick(EventCreate(name = text1.value, repeatDays = text2.value.toInt()))
                    }
                },
                enabled = text1.value.isNotEmpty() && text2.value.isNotEmpty()
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onClick(null) }) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Preview
@Composable
fun ScreenPreview() {
    ForegroundClockTheme {
        MainPage(
            events = listOf(EventView(1, "name", "repeat")),
            onEventRepeat = {},
            onEventDelete = {},
            viewModel = MainVM(android.app.Application())
        )
    }
}