package io.github.slopcommunistcooperative.meowmos.data.model

import kotlinx.serialization.Serializable
import java.time.Instant

enum class MemoVisibility {
    PRIVATE,
    PROTECTED,
    PUBLIC,
    SPACE
}

@Serializable
data class MemoSpace(val name: String, val title: String)

data class Memo(
    override val remoteId: String,
    override val content: String,
    override val date: Instant,
    override val pinned: Boolean,
    override val visibility: MemoVisibility,
    override val resources: List<Resource>,
    val tags: List<String>,
    val creator: User? = null,
    override val archived: Boolean = false,
    val updatedAt: Instant? = null,
    override val location: MemoLocation? = null,
    val space: String? = null,
    val accountKey: String? = null,
) : MemoRepresentable
