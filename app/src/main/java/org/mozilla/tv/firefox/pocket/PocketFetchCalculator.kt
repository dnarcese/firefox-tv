package org.mozilla.tv.firefox.pocket

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// todo: improve explanation.
/**
 * Provides calculations for when Pocket needs to fetch new content.
 *
 * TODO
 */
class PocketFetchCalculator {

    fun Calendar.cloneCalendar(): Calendar = clone() as Calendar

    fun getWorkInitialDelayMillis(
        now: Calendar = Calendar.getInstance(),
        randInt: (Int) -> Int = { Random.nextInt(it) }
    ): Long {
        val nextFetchIntervalStartTime = now.cloneCalendar().apply {
            set(Calendar.HOUR_OF_DAY, FETCH_START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // For simplicity, always fetch the upcoming night. TODO: clean up
            add(Calendar.DATE, 1)
        }

        val fetchOffsetSeconds = randInt(FETCH_INTERVAL_DURATION_SECONDS)
        val userFetchTime = nextFetchIntervalStartTime.cloneCalendar().apply {
            add(Calendar.SECOND, fetchOffsetSeconds)
        }

        return userFetchTime.timeInMillis - now.timeInMillis
    }

    companion object {
        // TODO: add test verify start before end
        // todo: for simplicity, just use hours.
        @VisibleForTesting(otherwise = PRIVATE) val FETCH_START_HOUR = 3 // am
        @VisibleForTesting(otherwise = PRIVATE) val FETCH_END_HOUR = 5L
        @VisibleForTesting(otherwise = PRIVATE) val FETCH_INTERVAL_DURATION_SECONDS = TimeUnit.HOURS.toSeconds(FETCH_END_HOUR - FETCH_START_HOUR).toInt()

        const val SHARED_PREFS_FILE = "Pocket-Global-Video-Recs"
        const val KEY_JSON_FULL = "pocket-recs"
    }
}
