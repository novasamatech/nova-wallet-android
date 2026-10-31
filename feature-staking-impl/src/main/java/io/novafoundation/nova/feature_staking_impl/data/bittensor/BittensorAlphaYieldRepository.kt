package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BittensorAlphaYieldRepository(
    private val remoteSource: BittensorEstimatesRemoteSource,
    private val infrastructureUrls: InfrastructureUrls
) {

    fun observeAlphaYields(chain: Chain, subnet: BittensorNetuid.Subnet): Flow<Map<AccountIdKey, BigDecimal>> =
        remoteSource.observeYields(chain, subnet).map { snapshot ->
            requireNotNull(snapshot.value.items).mapNotNull { mapBittensorValidatorYield(it, chain, subnet) }
                .distinctBy { it.hotkey }
                .filter { it.reliable }
                .associate { it.hotkey to it.annualPercentageYield }
        }

    /** The reported rate is an annual percent; validator commission is already accounted for. */
    suspend fun alphaYields(chain: Chain, subnet: BittensorNetuid.Subnet, includeUnreliable: Boolean = false): Map<AccountIdKey, BigDecimal> {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val pages = remoteSource.yields(baseUrl, chain, subnet)
        return pages.items.distinctBy { it.hotkey }
            .filter { includeUnreliable || it.reliable }
            .associate { it.hotkey to it.annualPercentageYield }
    }
}
