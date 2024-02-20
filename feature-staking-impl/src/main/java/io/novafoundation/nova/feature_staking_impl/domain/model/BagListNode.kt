package io.novafoundation.nova.feature_staking_impl.domain.model

import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

class BagListNode(
    val id: AccountId,
    val previous: AccountId?,
    val next: AccountId?,
    val bagUpper: Score,
    val score: Score,
) {

    @JvmInline
    value class Score(val value: BigInteger) : Comparable<Score> {

        companion object {

            fun zero() = Score(BigInteger.ZERO)
        }

        override fun compareTo(other: Score): Int {
            return value.compareTo(other.value)
        }
    }
}
