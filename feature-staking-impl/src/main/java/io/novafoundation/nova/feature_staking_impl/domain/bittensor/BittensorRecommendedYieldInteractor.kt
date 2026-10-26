package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRecommendationsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class BittensorRecommendedYieldInteractor(
    private val validatorBackend: BittensorValidatorBackend,
    private val alphaYieldRepository: BittensorAlphaYieldRepository,
    private val chainRegistry: ChainRegistry,
    private val logger: Logger
) {

    private data class RecommendedValidator(val subnetId: BittensorNetuid.Subnet, val hotkey: AccountIdKey)

    suspend fun maximumYields(): Map<BittensorNetuid.Subnet, BigDecimal> {
        val recommendations = validatorBackend.recommendedValidators()
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val validatorsBySubnet = recommendations
            .mapNotNull { mapValidator(chain, it) }
            .groupBy { it.subnetId }

        val requests = Semaphore(MAX_CONCURRENT_REQUESTS)
        val results = validatorsBySubnet.entries.mapAsync { (subnetId, validators) ->
            requests.withPermit { loadMaximumYield(chain, subnetId, validators) }
        }
        val rates = results
            .mapNotNull { it.getOrNull() }
            .toMap()

        if (rates.isEmpty()) {
            results.firstNotNullOfOrNull { it.exceptionOrNull() }?.let { throw it }
        }
        return rates
    }

    private fun mapValidator(chain: Chain, remote: BittensorRecommendationsRemote.Item): RecommendedValidator? {
        return try {
            val subnetId = BittensorNetuid.Subnet(requireNotNull(remote.netuid))
            val hotkey = chain.accountIdOf(requireNotNull(remote.hotkey))
            RecommendedValidator(subnetId, AccountIdKey(hotkey))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.log("[Bittensor] Skipping an invalid recommended validator")
            null
        }
    }

    private suspend fun loadMaximumYield(
        chain: Chain,
        subnetId: BittensorNetuid.Subnet,
        validators: List<RecommendedValidator>
    ): Result<Pair<BittensorNetuid.Subnet, BigDecimal>?> {
        return try {
            val yields = alphaYieldRepository.alphaYields(chain, subnetId)
            val maximumYield = validators
                .mapNotNull { yields[it.hotkey] }
                .maxOrNull()
            Result.success(maximumYield?.let { subnetId to it })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logger.log("[Bittensor] Failed to load recommended yield for subnet ${subnetId.value}")
            logger.log(exception)
            Result.failure(exception)
        }
    }

    private companion object {

        const val MAX_CONCURRENT_REQUESTS = 4
    }
}
