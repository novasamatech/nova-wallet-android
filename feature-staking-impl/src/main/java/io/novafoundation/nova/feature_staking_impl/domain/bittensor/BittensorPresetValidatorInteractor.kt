package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException

/** Automatic presets only. An explicitly selected hotkey must never be silently replaced. */
class BittensorPresetValidatorInteractor(
    private val repository: BittensorSubnetValidatorRepository,
    private val yields: BittensorAlphaYieldRepository,
    private val chains: ChainRegistry,
    private val logger: Logger
) {

    suspend fun validators(subnet: BittensorNetuid.Subnet): List<BittensorSubnetValidator> {
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        val directory = repository.validators(chain, subnet)
        val rates = try {
            yields.alphaYields(chain, subnet)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.log("[BittensorPreset] Optional yields unavailable")
            logger.log(failure)
            emptyMap()
        }
        val candidates = directory.filter { it.canSelect && it.recommended }
            .sortedWith(compareByDescending<BittensorSubnetValidator> { rates[it.validator.hotkey] })
            .take(MAX_ATTEMPTS)
        var lastFailure: Exception? = null
        for (candidate in candidates) {
            try {
                repository.validate(chain, subnet, candidate.validator)
                return listOf(candidate) + directory.filter { it.validator.hotkey != candidate.validator.hotkey }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                lastFailure = failure
                logger.log("[BittensorPreset] Automatic candidate unavailable")
                logger.log(failure)
            }
        }
        throw IllegalStateException("No eligible recommended validator in this subnet", lastFailure)
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
