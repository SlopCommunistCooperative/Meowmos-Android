package io.github.slopcommunistcooperative.meowmos.data.local.entity

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.slopcommunistcooperative.meowmos.data.model.MemoRepresentable
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import java.time.Instant

@Entity(
    tableName = "memos",
    indices = [
        Index(value = ["accountKey"]),
        Index(value = ["accountKey", "remoteId"])
    ]
)
data class MemoEntity(
    @PrimaryKey
    val identifier: String,
    override val remoteId: String? = null,
    val accountKey: String,
    override val content: String,
    override val date: Instant,
    override val visibility: MemoVisibility,
    override val pinned: Boolean,
    override val archived: Boolean = false,
    val needsSync: Boolean = true,
    val isDeleted: Boolean = false,
    val lastModified: Instant = Instant.now(),
    val lastSyncedAt: Instant? = null,
    @Embedded(prefix = "location_")
    override val location: MemoLocation? = null,
    val space: String? = null,
) : MemoRepresentable {
    @Ignore
    override var resources: List<ResourceEntity> = emptyList()
}
