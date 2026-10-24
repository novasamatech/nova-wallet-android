package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BITTENSOR_REQUEST_TIMEOUT
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorSubnetCatalogue
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** How many of the listed subnets the validator holds a permit on ("Subnets validated", as on iOS). */
data class BittensorValidatorCoverage(val permitted: Int, val total: Int)

internal fun bittensorValidatorCoverage(
    permits: Set<BittensorNetuid.Subnet>?,
    listed: Set<BittensorNetuid.Subnet>
): BittensorValidatorCoverage? = permits?.let { BittensorValidatorCoverage(it.intersect(listed).size, listed.size) }

class BittensorValidatorCoverageInteractor(
    private val delegates: BittensorDelegateSource,
    private val catalogue: BittensorValidatorSubnetCatalogue,
    private val timeoutMillis: Long = BITTENSOR_REQUEST_TIMEOUT
) {

    /** The denominator is every subnet the backend catalogue lists, as on iOS; null when the hotkey is not a delegate. */
    suspend fun coverage(hotkey: AccountIdKey): BittensorValidatorCoverage? {
        val result = withTimeoutOrNull(timeoutMillis) {
            coroutineScope {
                val permits = async { delegates.validatorPermits(ChainGeneses.BITTENSOR, hotkey) }
                val listed = async { catalogue.subnets() }
                CoverageResult(bittensorValidatorCoverage(permits.await(), listed.await()))
            }
        } ?: throw BittensorTimeoutException("Validator coverage")
        return result.coverage
    }

    private class CoverageResult(val coverage: BittensorValidatorCoverage?)
}
