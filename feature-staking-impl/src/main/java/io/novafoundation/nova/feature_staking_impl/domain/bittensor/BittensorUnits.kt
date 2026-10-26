package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger

/** Fixed runtime alpha precision. Native-token precision comes from the chain asset. */
const val BITTENSOR_ALPHA_DECIMALS = 9

val BITTENSOR_ALPHA_SCALE: BigInteger = BigInteger.TEN.pow(BITTENSOR_ALPHA_DECIMALS)
