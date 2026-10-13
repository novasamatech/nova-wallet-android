package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Upper bound for one Bittensor read (chain RPC, Nova service, CoinGecko) before the screen reports it as failed. */
internal val BITTENSOR_REQUEST_TIMEOUT: Duration = 20.seconds
