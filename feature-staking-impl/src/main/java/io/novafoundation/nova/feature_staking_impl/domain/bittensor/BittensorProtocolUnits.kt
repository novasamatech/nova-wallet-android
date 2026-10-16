package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger

internal object BittensorProtocolUnits {

    const val ALPHA_PRECISION = 9

    const val FIXED_POINT_FRACTION_BITS = 32

    val RAO_PER_TAO: BigInteger = BigInteger.TEN.pow(9)

    val DEFAULT_ROOT_CLAIM_THRESHOLD_RAO: BigInteger = BigInteger.valueOf(500_000)

    val NOMINATOR_FACTOR_DENOMINATOR: BigInteger = BigInteger.valueOf(1_000_000)
}
