package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** How many of the listed subnets the validator holds a permit on ("Subnets validated", as on iOS). */
data class BittensorValidatorCoverage(val permitted: Int, val total: Int)

internal fun bittensorValidatorCoverage(
    permits: Set<BittensorNetuid.Subnet>?,
    listed: Set<BittensorNetuid.Subnet>
): BittensorValidatorCoverage? = permits?.let { BittensorValidatorCoverage(it.intersect(listed).size, listed.size) }

class BittensorValidatorCoverageInteractor(
    private val delegates: BittensorDelegateSource,
    private val subnets: BittensorSubnetRepository
) {

    suspend fun coverage(hotkey: AccountIdKey): BittensorValidatorCoverage? = coroutineScope {
        val permits = async { delegates.validatorPermits(ChainGeneses.BITTENSOR, hotkey) }
        val listed = async { subnets.subnets().map { it.id }.toSet() }
        bittensorValidatorCoverage(permits.await(), listed.await())
    }
}
