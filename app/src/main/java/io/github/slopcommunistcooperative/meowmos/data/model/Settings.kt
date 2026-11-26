package io.github.slopcommunistcooperative.meowmos.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val usersList: List<UserData> = emptyList(),
    val currentUser: String = "",
    val appLockEnabled: Boolean = false,
    val dynamicColorEnabled: Boolean = true,
)
