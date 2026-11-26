package io.github.slopcommunistcooperative.meowmos.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MemoEditGesture {
    NONE,
    SINGLE,
    DOUBLE,
    LONG,
}

@Serializable
data class UserSettings(
    val draft: String = "",
    val acceptedUnsupportedSyncVersions: List<String> = emptyList(),
    val editGesture: MemoEditGesture = MemoEditGesture.NONE,
    val autosave: Boolean = false,
    val draftLocation: MemoLocation? = null,
    val searchIncludesArchived: Boolean = false,
    val lastMemoSpace: MemoSpace? = null,
    val lastCommentVisibility: MemoVisibility? = null,
)

internal fun UserSettings.commentVisibilityFor(parent: MemoVisibility, inSpace: Boolean): MemoVisibility {
    val parentAudience = if (parent == MemoVisibility.SPACE && !inSpace) MemoVisibility.PRIVATE else parent
    return when (lastCommentVisibility) {
        null -> parentAudience
        MemoVisibility.PRIVATE -> MemoVisibility.PRIVATE
        MemoVisibility.PROTECTED -> if (parent == MemoVisibility.PUBLIC) MemoVisibility.PROTECTED else parentAudience
        MemoVisibility.PUBLIC -> parentAudience
        MemoVisibility.SPACE -> if (inSpace && parent != MemoVisibility.PRIVATE) MemoVisibility.SPACE else MemoVisibility.PRIVATE
    }
}
