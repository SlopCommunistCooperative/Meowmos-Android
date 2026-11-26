package io.github.slopcommunistcooperative.meowmos.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.skydoves.sandwich.ApiResponse
import kotlinx.coroutines.runBlocking
import io.github.slopcommunistcooperative.meowmos.data.api.*
import io.github.slopcommunistcooperative.meowmos.data.local.FileStorage
import io.github.slopcommunistcooperative.meowmos.data.local.MeowmosDatabase
import io.github.slopcommunistcooperative.meowmos.data.model.Account
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocation
import io.github.slopcommunistcooperative.meowmos.data.model.MemoLocationChange
import io.github.slopcommunistcooperative.meowmos.data.model.MemoVisibility
import io.github.slopcommunistcooperative.meowmos.data.model.MemosAccount
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant

class LocationSyncTest {
    private lateinit var database: MeowmosDatabase
    private lateinit var repository: SyncingRepository
    private val account = Account.MemosV1(MemosAccount(remoteIdentifier = "users/a"))
    private val remote = linkedMapOf<String, MemosV1Memo>()
    // Memos stores memo timestamps as Unix seconds; Room stores them as milliseconds.
    private var tick = Instant.ofEpochSecond(Instant.now().epochSecond)
    private fun nextTime(): Instant = tick.plusSeconds(1).also { tick = it }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MeowmosDatabase::class.java).build()
        val api = Proxy.newProxyInstance(MemosV1Api::class.java.classLoader, arrayOf(MemosV1Api::class.java)) { _, method, args ->
            when (method.name) {
                "getCurrentUser" -> ApiResponse.Success(GetCurrentUserResponse(MemosV1User("users/a", username = "a")))
                "getUserSetting" -> ApiResponse.Success(MemosV1UserSetting(MemosV1UserSettingGeneralSetting(memoVisibility = MemosVisibility.PRIVATE)))
                "listMemos" -> ApiResponse.Success(ListMemosResponse(remote.values.filter { it.state == args[2] }, null))
                "createMemo" -> {
                    val request = args[0] as MemosV1CreateMemoRequest
                    val memo = MemosV1Memo(
                        name = "memos/${remote.size + 1}", state = MemosV1State.NORMAL,
                        content = request.content, visibility = request.visibility,
                        createTime = request.createTime, updateTime = nextTime(), location = request.location,
                        space = request.space,
                    )
                    remote[memo.name] = memo
                    ApiResponse.Success(memo)
                }
                "updateMemo" -> {
                    val name = "memos/${args[0]}"
                    val request = args[1] as UpdateMemoRequest
                    val mask = (args[2] as String?).orEmpty().split(',')
                    val previous = remote.getValue(name)
                    val memo = previous.copy(
                        content = request.content ?: previous.content,
                        visibility = request.visibility ?: previous.visibility,
                        pinned = request.pinned ?: previous.pinned,
                        state = request.state ?: previous.state,
                        updateTime = nextTime(),
                        location = if ("location" in mask) request.location else previous.location,
                        space = if ("space" in mask) request.space else previous.space,
                    )
                    remote[name] = memo
                    ApiResponse.Success(memo)
                }
                "listResources" -> ApiResponse.Success(ListResourceResponse(emptyList()))
                else -> error("Unexpected API call: ${method.name}")
            }
        } as MemosV1Api
        repository = SyncingRepository(database.memoDao(), FileStorage(context), MemosV1Repository(api, account), account,
            deferredPushDelayMillis = 600_000)
    }

    @After fun tearDown() {
        repository.close()
        database.close()
    }

    private fun <T> success(response: ApiResponse<T>): T {
        assertTrue("Expected success, got $response", response is ApiResponse.Success)
        return (response as ApiResponse.Success).data
    }

    @Test fun locationCreatesPullsEditsAndRemovesAcrossSync() = runBlocking {
        val home = MemoLocation("Home", -1.234567, 2.345678)
        val memo = success(repository.createMemo("note", MemoVisibility.PRIVATE, emptyList(), deferPush = true, location = home))
        success(repository.sync())
        assertEquals(home, remote.values.single().location)
        assertEquals(home, repository.getMemo(memo.identifier)?.location)
        assertFalse(requireNotNull(repository.getMemo(memo.identifier)).needsSync)

        val office = MemoLocation("Office", 0.0, 0.0)
        val serverMemo = remote.values.single()
        remote[serverMemo.name] = serverMemo.copy(location = office, updateTime = nextTime())
        success(repository.sync())
        assertEquals(office, repository.getMemo(memo.identifier)?.location)

        success(repository.updateMemo(memo.identifier, pinned = true, deferPush = true))
        success(repository.sync())
        assertEquals(office, remote.values.single().location)

        success(repository.updateMemo(memo.identifier, location = MemoLocationChange(home), deferPush = true))
        success(repository.sync())
        assertEquals(home, remote.values.single().location)

        success(repository.updateMemo(memo.identifier, location = MemoLocationChange(null), deferPush = true))
        success(repository.sync())
        assertNull(remote.values.single().location)
        assertNull(repository.getMemo(memo.identifier)?.location)
        assertEquals(1, remote.size)
    }

    @Test fun spacePlacementSurvivesOfflineEditsAndCanBeRemoved() = runBlocking {
        val family = "spaces/family"
        val project = "spaces/project"
        val memo = success(repository.createMemo("shared note", MemoVisibility.SPACE, emptyList(), deferPush = true, space = family))
        assertEquals(family, repository.getMemo(memo.identifier)?.space)

        success(repository.sync())
        val serverMemo = remote.values.single()
        assertEquals(family, serverMemo.space)
        assertEquals(MemosVisibility.SPACE, serverMemo.visibility)
        remote[serverMemo.name] = serverMemo.copy(space = project, updateTime = nextTime())
        success(repository.sync())
        assertEquals(project, repository.getMemo(memo.identifier)?.space)

        success(repository.updateMemo(memo.identifier, visibility = MemoVisibility.PRIVATE, deferPush = true, space = ""))
        success(repository.sync())
        assertNull(remote.values.single().space)
        assertEquals(MemosVisibility.PRIVATE, remote.values.single().visibility)
        assertNull(repository.getMemo(memo.identifier)?.space)
    }

    @Test fun conflictingLocationEditsPreserveBothVersions() = runBlocking {
        val memo = success(repository.createMemo("note", MemoVisibility.PRIVATE, emptyList(), deferPush = true, location = MemoLocation("Initial", 1.0, 2.0)))
        success(repository.sync())
        val localLocation = MemoLocation("Phone", 3.0, 4.0)
        val serverLocation = MemoLocation("Web", 5.0, 6.0)
        success(repository.updateMemo(memo.identifier, location = MemoLocationChange(localLocation), deferPush = true))
        val serverMemo = remote.values.single()
        remote[serverMemo.name] = serverMemo.copy(location = serverLocation, updateTime = nextTime())
        success(repository.sync())
        assertEquals(setOf(localLocation, serverLocation), remote.values.map { it.location }.toSet())
        assertEquals(setOf(localLocation, serverLocation), success(repository.listMemos()).map { it.location }.toSet())
    }
}
