/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import androidx.annotation.UiThread
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.telemetry.SentryIntegration
import org.mozilla.tv.firefox.utils.PeriodicRequester
import org.mozilla.tv.firefox.utils.Response
import java.util.concurrent.TimeUnit

private const val LOGTAG = "PocketVideoRepo"
private val CACHE_UPDATE_FREQUENCY_MILLIS = TimeUnit.MINUTES.toMillis(45)
private const val BASE_RETRY_TIME = 1_000L

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 */
open class PocketVideoRepo(
    private val pocketEndpoint: PocketEndpoint,
    private val pocketFeedStateMachine: PocketFeedStateMachine,
    initialState: FeedState,
    private val sharedPreferences: SharedPreferences,
    private val resources: Resources
) {

    sealed class FeedState {
        data class LoadComplete(val videos: List<PocketViewModel.FeedItem>) : FeedState()
        object Loading : FeedState()
        object NoAPIKey : FeedState()
        object FetchFailed : FeedState()
        object Inactive : FeedState()
    }

    private val _feedState = BehaviorSubject.createDefault(initialState)
    open val feedState = _feedState.hide()
        .observeOn(AndroidSchedulers.mainThread())

    private val periodicRequester = PeriodicRequester(pocketEndpoint)
    private val compositeDisposable = CompositeDisposable()

    fun update() {
        pocketEndpoint.request()
            .subscribeOn(Schedulers.io())
            .subscribe(this::postUpdate)
            .addTo(compositeDisposable)
    }

    @UiThread // update backgroundUpdates.
    fun startBackgroundUpdates() {
        // called from onStart; maybe somewhere else but we can write it here for now
        // Fetch from shared preferences
        // Push results into Rx: _feedState.onNext
        // Later: only update if it's new: _feedState.distinctUntilChanged() // ... // .subscribe()
        val videos = sharedPreferences.getString(PocketFetchCalculator.KEY_JSON_FULL, null)
            ?: resources.getString(R.raw.bundled_pocket_json)
        val convertedVideos = pocketEndpoint.convertVideosJSON(videos)

        if (convertedVideos == null) {
            Log.e(LOGTAG, "Error in converting JSON to Pocket video")
            SentryIntegration.capture(Exception("Error in converting JSON to Pocket video"))
            return
        }

        //convert videos
        _feedState.onNext(FeedState.LoadComplete(convertedVideos))

        compositeDisposable.clear()
        periodicRequester.start()
            .subscribe(this::postUpdate)
            .addTo(compositeDisposable)
    }

    // When we the app is not in use, we don't want to hit the network for no reason, so we cancel updates
    @UiThread // stop updating backgroundUpdates.
    fun stopBackgroundUpdates() {
        compositeDisposable.clear()
    }

    private fun postUpdate(response: Response<PocketData>) {
        fun Response<PocketData>.toRepoState(): FeedState =
            if (this is Response.Success) FeedState.LoadComplete(this.data)
            else FeedState.FetchFailed

        fun postState(newState: PocketVideoRepo.FeedState) {
            val computed = pocketFeedStateMachine.computeNewState(newState, _feedState.value)
            if (_feedState.value !== computed) {
                _feedState.onNext(computed)
            }
        }

        postState(response.toRepoState())
    }
}
