package io.github.slopcommunistcooperative.meowmos.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun oldJsonWithoutField_defaultsToFalse() {
        val s = json.decodeFromString<UserSettings>("""{"draft":"x","autosave":true}""")
        assertFalse(s.searchIncludesArchived)
        assertNull(s.lastMemoSpace)
        assertNull(s.lastCommentVisibility)
        assertTrue(s.autosave)
    }

    @Test
    fun fieldRoundTrips() {
        val s = UserSettings(
            searchIncludesArchived = true,
            lastMemoSpace = MemoSpace("spaces/family", "Family"),
            lastCommentVisibility = MemoVisibility.SPACE,
        )
        assertEquals(s, json.decodeFromString<UserSettings>(json.encodeToString(s)))
    }

    @Test
    fun firstCommentDefaultsToParentAudience() {
        val settings = UserSettings()
        for (parent in MemoVisibility.entries) {
            assertEquals(parent, settings.commentVisibilityFor(parent, inSpace = true))
            val safeParent = if (parent == MemoVisibility.SPACE) MemoVisibility.PRIVATE else parent
            assertEquals(safeParent, settings.commentVisibilityFor(parent, inSpace = false))
        }
    }

    @Test
    fun rememberedAudienceDoesNotWidenParentAudience() {
        val rememberedPrivate = UserSettings(lastCommentVisibility = MemoVisibility.PRIVATE)
        val rememberedProtected = UserSettings(lastCommentVisibility = MemoVisibility.PROTECTED)
        val rememberedPublic = UserSettings(lastCommentVisibility = MemoVisibility.PUBLIC)

        for (parent in MemoVisibility.entries) {
            assertEquals(MemoVisibility.PRIVATE, rememberedPrivate.commentVisibilityFor(parent, inSpace = true))
            assertEquals(parent, rememberedPublic.commentVisibilityFor(parent, inSpace = true))
            val expected = if (parent == MemoVisibility.PUBLIC) MemoVisibility.PROTECTED else parent
            assertEquals(expected, rememberedProtected.commentVisibilityFor(parent, inSpace = true))
        }
        assertEquals(MemoVisibility.PRIVATE, rememberedProtected.commentVisibilityFor(MemoVisibility.SPACE, inSpace = false))
        assertEquals(MemoVisibility.PRIVATE, rememberedPublic.commentVisibilityFor(MemoVisibility.SPACE, inSpace = false))
    }

    @Test
    fun rememberedSpaceAudienceRequiresSpaceAndNonPrivateParent() {
        val settings = UserSettings(lastCommentVisibility = MemoVisibility.SPACE)
        for (parent in MemoVisibility.entries) {
            assertEquals(MemoVisibility.PRIVATE, settings.commentVisibilityFor(parent, inSpace = false))
            val expected = if (parent == MemoVisibility.PRIVATE) MemoVisibility.PRIVATE else MemoVisibility.SPACE
            assertEquals(expected, settings.commentVisibilityFor(parent, inSpace = true))
        }
    }
}
