package io.novafoundation.nova.feature_staking_impl.domain.common

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface EraRewardCalculatorComparable {

    /**
     * Returns a number that can be used to compare different instances of [EraTimeCalculator]
     * to determine how much their calculations would deffer between each other
     * This wont correspond to real timestamp and shouldn't be used as such
     */
    fun derivedTimestamp(): Duration
}

fun <T : EraRewardCalculatorComparable> Flow<T>.ignoreInsignificantTimeChanges(): Flow<T> {
    return distinctUntilChanged { old, new -> new.canBeIgnoredAfter(old) }

}

private fun EraRewardCalculatorComparable.canBeIgnoredAfter(previous: EraRewardCalculatorComparable): Boolean {
    val previousTimestamp = previous.derivedTimestamp()
    val newTimestamp = derivedTimestamp()

    val difference = (newTimestamp - previousTimestamp).absoluteValue

    val canIgnore = difference < ERA_DURATION_DIFFERENCE_THRESHOLD

    Log.d("EraRewardCalculatorComparable", "New update for EraRewardCalculatorComparable, difference: $difference can ignore: $canIgnore")

    return canIgnore
}

private val ERA_DURATION_DIFFERENCE_THRESHOLD = 10.minutes
