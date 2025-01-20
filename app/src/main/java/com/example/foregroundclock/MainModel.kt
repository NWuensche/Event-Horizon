package com.example.foregroundclock

import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainModel {

    companion object {
        val db = PreferenceManager.getDefaultSharedPreferences(context)
        val eventsKey = "EVENTS"
    }

    fun getEvents(): List<EventDB> {
        return db.getString(eventsKey, "[]")!!.run {
            Json.decodeFromString(this)
        }
    }

    fun saveEvents(allEvents: List<EventDB>) {
        db.edit().run {
            putString(eventsKey, Json.encodeToString(allEvents))
            commit()
        }
    }
}