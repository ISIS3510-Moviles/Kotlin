package com.example.campusbites.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore(name = "home_data_cache")