package io.novafoundation.nova.feature_governance_api.data.repository.common

import java.math.BigInteger

sealed interface RecentVotesDateThreshold {
    class BlockNumber(val number: BigInteger) : RecentVotesDateThreshold
    class Timestamp(val timestampMs: Long) : RecentVotesDateThreshold
}

