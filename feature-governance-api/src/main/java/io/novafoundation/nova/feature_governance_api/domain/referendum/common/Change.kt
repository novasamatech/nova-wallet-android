package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger
import kotlin.time.Duration

sealed class Change<T>(open val previousValue: T, open val newValue: T) {

    data class Changed<T>(
        override val previousValue: T,
        override val newValue: T,
        val absoluteDifference: T,
        val positive: Boolean
    ) : Change<T>(previousValue, newValue)

    data class Same<T>(val value: T) : Change<T>(previousValue = value, newValue = value)
}

fun Change<Balance>.absoluteDifference(): Balance {
    return when (this) {
        is Change.Changed -> absoluteDifference
        is Change.Same -> Balance.ZERO
    }
}

fun Change<Duration>.absoluteDifference(): Duration {
    return when (this) {
        is Change.Changed -> absoluteDifference
        is Change.Same -> Duration.ZERO
    }
}

fun <T : Comparable<T>> Change(
    previousValue: T,
    newValue: T,
    absoluteDifference: T
): Change<T> {
    return if (previousValue == newValue) {
        Change.Same(newValue)
    } else {
        Change.Changed(
            previousValue = previousValue,
            newValue = newValue,
            absoluteDifference = absoluteDifference,
            positive = newValue > previousValue
        )
    }
}

fun Change(
    previousValue: BigInteger,
    newValue: BigInteger,
): Change<BigInteger> {
    val absoluteDifference = (newValue - previousValue).abs()

    return Change(previousValue, newValue, absoluteDifference)
}

fun Change(
    previousValue: Duration,
    newValue: Duration,
): Change<Duration> {
    val absoluteDifference = (newValue - previousValue).absoluteValue

    return Change(previousValue, newValue, absoluteDifference)
}
