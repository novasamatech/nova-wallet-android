package io.novafoundation.nova.feature_governance_api.data.repository.common

import java.math.BigInteger

sealed interface RecentVotesDateThreshold {

    companion object;

    class BlockNumber(val number: BigInteger) : RecentVotesDateThreshold
    class Timestamp(val timestampMs: Long) : RecentVotesDateThreshold
}

fun RecentVotesDateThreshold.Companion.zeroPoint() = RecentVotesDateThreshold.BlockNumber(BigInteger.ZERO)
