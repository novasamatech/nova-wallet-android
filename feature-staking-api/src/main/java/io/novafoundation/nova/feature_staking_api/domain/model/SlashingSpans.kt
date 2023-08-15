package io.novafoundation.nova.feature_staking_api.domain.model

import java.math.BigInteger

class SlashingSpans(
    val lastNonZeroSlash: EraIndex,
    val prior: List<EraIndex>
)

fun SlashingSpans?.numberOfSlashingSpans(): BigInteger {
    if (this == null) return BigInteger.ZERO

    // all from prior + one for lastNonZeroSlash
    val numberOfSlashingSpans = prior.size + 1

    return numberOfSlashingSpans.toBigInteger()
}
