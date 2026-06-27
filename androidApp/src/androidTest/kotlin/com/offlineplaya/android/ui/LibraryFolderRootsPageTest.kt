package com.offlineplaya.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.offlineplaya.shared.domain.model.Folder
import com.offlineplaya.shared.presentation.ui.TestTags
import com.offlineplaya.shared.presentation.ui.pages.LibraryFolderRootsPage
import com.offlineplaya.shared.presentation.ui.theme.PreviewTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Isolated-screen test for [LibraryFolderRootsPage]: managed roots render and a
 * row tap routes to [onFolderClick] with that folder's id.
 */
@OptIn(ExperimentalTestApi::class)
class LibraryFolderRootsPageTest {

    @Test
    fun renders_roots_and_tap_routes_folder_id() = runComposeUiTest {
        var clickedFolderId = -1L
        setContent {
            PreviewTheme {
                LibraryFolderRootsPage(
                    roots = persistentListOf(
                        Folder(1, "content://A", "", "Main Collection", null, 247),
                        Folder(2, "content://B", "", "Rare Bootlegs", null, 412),
                    ),
                    onFolderClick = { clickedFolderId = it },
                    onTabSelected = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.FolderRoots.ROOT),
                )
            }
        }

        onNodeWithTag(TestTags.FolderRoots.ROOT).assertIsDisplayed()
        onNodeWithText("Main Collection").assertIsDisplayed()
        onNodeWithText("Rare Bootlegs").assertIsDisplayed().performClick()
        assertEquals("folder tap should route its id", 2L, clickedFolderId)
    }

    @Test
    fun renders_no_roots() = runComposeUiTest {
        setContent {
            PreviewTheme {
                LibraryFolderRootsPage(
                    roots = persistentListOf(),
                    onFolderClick = {},
                    onTabSelected = {},
                    onBack = {},
                    modifier = Modifier.testTag(TestTags.FolderRoots.ROOT),
                )
            }
        }
        onNodeWithTag(TestTags.FolderRoots.ROOT).assertIsDisplayed()
    }
}
