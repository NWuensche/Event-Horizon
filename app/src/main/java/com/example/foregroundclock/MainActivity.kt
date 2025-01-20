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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.foregroundclock.MyForegroundService.Companion.ONE_DAY_IN_MILLIS
import com.example.foregroundclock.ui.theme.ForegroundClockTheme
import kotlinx.coroutines.flow.collectLatest

lateinit var context: Context
class MainActivity : ComponentActivity() {
    val vm: MainVM by viewModels()




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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = vm::onFABClicked
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add"
                            )
                        }
                    }
                ) { innerPadding ->
                    MainPage (
                        modifier = Modifier.padding(innerPadding),
                        events = events.value,
                        onEventRepeat = vm::onRepeatClicked,
                        onEventDelete = vm::onDelete
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
fun MainPage(events: List<EventView>, onEventRepeat: (Int) -> Unit, onEventDelete: (Int) -> Unit, modifier: Modifier = Modifier) {

    LazyColumn(modifier = modifier) {
        items(events) {
            EventView(it, onEventRepeat, onEventDelete)
        }
    }

}

@Composable
fun EventView(event: EventView, onRepeat: (Int) -> Unit, onDelete: (Int) -> Unit) {
        Row {
            Column {
                Text(event.name)
                Row {
                    Text(event.nextRepeat)
                }
            }

            IconButton(
                onClick = { onRepeat(event.id) }
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null) //TODO Different Icon
            }

            IconButton(
                onClick = { onDelete(event.id) }
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null) //TODO Different Icon
            }
        }

}

@Composable
fun MyDialog(
    onClick: (EventCreate?) -> Unit
) {
    val text1 = remember { mutableStateOf("") }
    val text2 = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onClick(null) },
            title = { Text("Enter Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text1.value,
                        onValueChange = { text1.value= it },
                        label = { Text("Name des Events") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = text2.value,
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                text2.value = it

                            }
                            },
                        label = { Text("Tage bis zur Erinnerung") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onClick(EventCreate(name = text1.value, repeatDays = text2.value.toInt()))
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { onClick(null) }) {
                    Text("Cancel")
                }
            }
        )
}



@Preview(showBackground = true)
@Composable
fun ScreenPreview() {
    ForegroundClockTheme {
        //TODO Add second push channel for alarms
        MainPage(
            events = listOf(EventView(1, "name", "repeat")),
            onEventRepeat = {},
            onEventDelete = {}
        )
    }
}