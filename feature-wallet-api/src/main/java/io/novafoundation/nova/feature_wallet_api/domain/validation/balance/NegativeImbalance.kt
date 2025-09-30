package io.novafoundation.nova.feature_wallet_api.domain.validation.balance

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive

@JvmInline
value class NegativeImbalance private constructor(val value: Balance) : Comparable<NegativeImbalance> {

    init {
        require(value.isPositive()) {
            "Imbalance cannot be negative"
        }
    }

    companion object {

        /**
         * @return How much should be added to [have] to match [want]. null in case there is no imbalance
         */
        fun from(have: Balance, want: Balance): NegativeImbalance? {
            return if (have >= want) {
                null
            } else {
                NegativeImbalance((want - have))
            }
        }
    }

    operator fun plus(other: NegativeImbalance): NegativeImbalance {
        return NegativeImbalance(value + other.value)
    }

    override fun compareTo(other: NegativeImbalance): Int {
        return value.compareTo(other.value)
    }
}

fun NegativeImbalance?.max(other: NegativeImbalance?): NegativeImbalance? {
    return when {
        this != null && other != null -> maxOf(this, other)
        this != null -> this
        other != null -> other
        else -> null
    }
}
