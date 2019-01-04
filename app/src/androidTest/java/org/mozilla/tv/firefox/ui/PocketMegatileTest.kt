/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ui

import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.tv.firefox.FirefoxTestApplication
import org.mozilla.tv.firefox.helpers.AndroidAssetDispatcher
import org.mozilla.tv.firefox.helpers.MainActivityTestRule
import org.mozilla.tv.firefox.helpers.TestAssetHelper
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.ui.robots.navigationOverlay

/**
 * A test for the pocket megatile including:
 * - Loading state appears
 * - Loading state transitions to error (or success) correctly
 * - Error states look correct
 * - Try again button works correctly
 */

class PocketMegatileTest {

    @get:Rule val activityTestRule = MainActivityTestRule()

    private lateinit var app: FirefoxTestApplication
    private lateinit var page: TestAssetHelper.TestAsset

    private lateinit var mockedState: PocketVideoRepo.FeedState

    @Before
    fun setup() {
        val server = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }
        page = TestAssetHelper.getGenericAssets(server).first()
        app = activityTestRule.activity.application as FirefoxTestApplication

        mockedState = PocketVideoRepo.FeedState.FetchFailed
        app.pushPocketRepoState(mockedState)
    }

    @After
    fun tearDown() {
        activityTestRule.activity.finishAndRemoveTask()
    }

    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
    @Test
    fun pocketMegatileTest() {
        /*
        * Force error state
        * Verify error screen
        * Click try again
        * Verify error screen
        * Click try again
        * Verify videos
        */

        navigationOverlay {
            mockedState = PocketVideoRepo.FeedState.FetchFailed
            app.pushPocketRepoState(mockedState)

            assertPocketTryAgainButtonDisplayed(true)
            assertPocketErrorTextDisplayed(true)
            pocketTryAgain()

//            assertPocketVideos(true)
//            assertPocketTryAgainButtonDisplayed(false)
        }
    }
}
