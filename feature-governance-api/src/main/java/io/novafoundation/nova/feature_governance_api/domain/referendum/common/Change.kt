package io.novafoundation.nova.feature_governance_api.domain.referendum.common

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

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
