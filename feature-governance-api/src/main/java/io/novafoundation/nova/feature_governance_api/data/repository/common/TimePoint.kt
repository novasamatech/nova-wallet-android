package io.novafoundation.nova.feature_governance_api.data.repository.common

import java.math.BigInteger

sealed interface TimePoint {
    class BlockNumber(val number: BigInteger) : TimePoint
    class Timestamp(val timestampMs: Long) : TimePoint
}

