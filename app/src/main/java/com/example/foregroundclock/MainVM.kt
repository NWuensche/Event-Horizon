package com.example.foregroundclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

class MainVM: ViewModel() {

    private val mainModel = MainModel()

    val events = MutableStateFlow(emptyList<EventView>())
    var eventsDB = emptyList<EventDB>()
        set(value) {
            mainModel.saveEvents(value)
            events.value = value.map { it.toEventView() }
            field = value
        }
    val showDialog = MutableStateFlow(false)

    val createEvent = MutableSharedFlow<EventCreateForAlarm>()
    val deleteEvent = MutableSharedFlow<Int>()
    val repeatNowEvent = MutableSharedFlow<EventCreateForAlarm>()

    fun onDelete(eventId: Int) {
        //todo snackbar mit name gelöscht
        eventsDB = eventsDB.filterNot { it.id == eventId }
        viewModelScope.launch {
            deleteEvent.emit(eventId)
        }
        //TODO Delete alarm
    }

    fun onFABClicked() {
        showDialog.value = true
    }

    fun onRepeatClicked(eventId: Int) {
        eventsDB = eventsDB.map {
            if (it.id != eventId) return@map it



            val new = it.copy(nextRepeatISO = LocalDateTime.now().plusDays(it.repeatDays.toLong()).toIso()) //Start in now + repeatdays days

            viewModelScope.launch {
                repeatNowEvent.emit(new.toEventsCreateForAlarm())
            }

            new
        }
    }

    init {
        eventsDB = mainModel.getEvents()
    }

    /**
     * @param event null iff dialog dismissed
     */
    fun onDialogClosed(newEvent: EventCreate?) {
        showDialog.value = false
        if (newEvent == null) return

        val newEventDB = newEvent.toEventDB()
        eventsDB = eventsDB + listOf(newEventDB)

        viewModelScope.launch {
            createEvent.emit(newEventDB.toEventsCreateForAlarm())
        }
    }
}

data class EventCreate(val name: String, val repeatDays: Int)
data class EventCreateForAlarm(val name: String, val repeatDays: Int, val id: Int)

data class EventView(val id: Int, val name: String, val nextRepeat: String)

@Serializable
data class EventDB(val id: Int, val name: String, val startsAtISO: String, val nextRepeatISO: String, val repeatDays: Int)

fun EventCreate.toEventDB(): EventDB {
    val start = LocalDateTime.now()
    val nextRepeat = start.plusDays(repeatDays.toLong())

    return EventDB (
        id = Random.nextInt(),
        name = name,
        startsAtISO = start.toIso(),
        nextRepeatISO = nextRepeat.toIso(),
        repeatDays = repeatDays
    )

}

fun EventDB.toEventsCreateForAlarm(): EventCreateForAlarm {
    return EventCreateForAlarm (
        id = id,
        name = name,
        repeatDays = repeatDays
    )

}

fun LocalDateTime.toIso(): String = this.format(DateTimeFormatter.ISO_DATE_TIME)

fun EventDB.toEventView(): EventView {
    return EventView(
        id = id,
        name = name,
        nextRepeat = nextRepeatISO
    )

}
