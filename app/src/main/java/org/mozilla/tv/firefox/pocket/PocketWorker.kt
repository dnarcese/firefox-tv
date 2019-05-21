/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.mozilla.tv.firefox.ext.serviceLocator



/**
 * todo: kdoc
 */
class PocketWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    private val pocketEndpointRaw = appContext.serviceLocator.pocketEndpointRaw
    private val pocketEndpoint = appContext.serviceLocator.pocketEndpoint
    private val sharedPrefs = applicationContext.getSharedPreferences(PocketFetchCalculator.SHARED_PREFS_FILE, 0)

    override fun doWork(): Result {
        // TODO: en-US only.

        // todo: does Sentry catch crashes in here?
        // todo: what happens to thrown exceptions?
        // todo: failure vs. retry? network errors, vs. JSON servers.

        // fetch raw json
        // Validate: convert raw json
        //     If converted values seem valid enough (10 successful conversions?)
        // If valid, save to disk, overwriting data.
        val rawJSONStr = runBlocking { pocketEndpointRaw.getGlobalVideoRecommendations() }
            ?: return Result.failure()
        val convertedVideos = pocketEndpoint.convertVideosJSON(rawJSONStr)
            ?: return Result.failure()
        if (convertedVideos.size >= 5) {
            sharedPrefs.edit().putString(PocketFetchCalculator.KEY_JSON_FULL, rawJSONStr)
                .apply()
        } else {
            return Result.failure()
        }

        return Result.success()
    }
}
