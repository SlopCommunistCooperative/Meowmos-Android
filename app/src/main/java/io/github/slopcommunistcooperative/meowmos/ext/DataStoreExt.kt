package io.github.slopcommunistcooperative.meowmos.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.slopcommunistcooperative.meowmos.data.model.Settings
import io.github.slopcommunistcooperative.meowmos.util.SettingsSerializer

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings_v3.json",
    serializer = SettingsSerializer
)
