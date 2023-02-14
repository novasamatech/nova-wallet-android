package io.novafoundation.nova.feature_staking_impl.domain.model

import jp.co.soramitsu.fearless_utils.runtime.AccountId
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

        override fun compareTo(other: Score): Int {
            return value.compareTo(other.value)
        }
    }
}
