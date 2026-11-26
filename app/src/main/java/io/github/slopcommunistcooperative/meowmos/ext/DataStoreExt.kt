package io.github.slopcommunistcooperative.meowmos.ext

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.slopcommunistcooperative.meowmos.data.model.Settings
import io.github.slopcommunistcooperative.meowmos.data.model.UserSettings
import io.github.slopcommunistcooperative.meowmos.util.SettingsSerializer

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "settings_v3.json",
    serializer = SettingsSerializer
)

internal suspend fun Context.updateUserSettings(
    accountKey: String?,
    transform: (UserSettings) -> UserSettings,
) {
    settingsDataStore.updateData { settings ->
        val index = settings.usersList.indexOfFirst { it.accountKey == (accountKey ?: settings.currentUser) }
        if (index == -1) return@updateData settings
        val users = settings.usersList.toMutableList()
        val user = users[index]
        users[index] = user.copy(settings = transform(user.settings))
        settings.copy(usersList = users)
    }
}
