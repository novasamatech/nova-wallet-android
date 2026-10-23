package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** The subnet directory together with its automatic preset. [preset] is null when no validator qualifies. */
class BittensorValidatorPreset(
    val directory: List<BittensorSubnetValidator>,
    val preset: BittensorSubnetValidator?
)

/**
 * Automatic presets only. An explicitly selected hotkey must never be silently replaced.
 *
 * Mirrors iOS (NW-2303): an existing position keeps its primary validator while it is active; otherwise the preset is the
 * recommended validator with the highest reliable APY that is active on the subnet, with a single fallback to the runner-up.
 */
class BittensorPresetValidatorInteractor @Inject constructor(
    private val repository: BittensorSubnetValidatorRepository,
    private val yields: BittensorAlphaYieldRepository,
    private val chains: ChainRegistry,
    private val portfolioRepository: BittensorPortfolioRepository,
    private val logger: Logger
) {

    /** Fails only when the directory or recommendations cannot be loaded, so callers can offer a retry. */
    suspend fun preset(subnet: BittensorNetuid.Subnet): BittensorValidatorPreset = coroutineScope {
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        // Positions and yields are independent of the directory; load them alongside it instead of after it.
        val existingHotkey = async { existingPrimaryHotkey(subnet) }
        val rates = async { optionalYields(chain, subnet) }
        // A directory failure cancels both loads with the scope.
        val directory = repository.validators(chain, subnet)
        val existing = existingHotkey.await()?.let { hotkey -> directory.firstOrNull { it.validator.hotkey == hotkey && it.canSelect } }
        val preset = existing ?: recommendedPreset(chain, subnet, directory, rates.await())
        BittensorValidatorPreset(directory, preset)
    }

    private suspend fun recommendedPreset(
        chain: Chain,
        subnet: BittensorNetuid.Subnet,
        directory: List<BittensorSubnetValidator>,
        rates: Map<AccountIdKey, BigDecimal>
    ): BittensorSubnetValidator? {
        for (candidate in bittensorPresetCandidates(directory, rates).take(MAX_ATTEMPTS)) {
            try {
                repository.validate(chain, subnet, candidate.validator)
                return candidate
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                logger.log("[BittensorPreset] Automatic candidate unavailable")
                logger.log(failure)
            }
        }
        return null
    }

    private suspend fun optionalYields(chain: Chain, subnet: BittensorNetuid.Subnet): Map<AccountIdKey, BigDecimal> = try {
        yields.alphaYields(chain, subnet)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        logger.log("[BittensorPreset] Optional yields unavailable")
        logger.log(failure)
        emptyMap()
    }

    private suspend fun existingPrimaryHotkey(subnet: BittensorNetuid.Subnet): AccountIdKey? = try {
        withTimeoutOrNull(PORTFOLIO_TIMEOUT_MILLIS) {
            portfolioRepository.observePortfolio().first { it.dataStatus !is BittensorDataStatus.Loading }
        }
            ?.positions
            ?.subnets
            ?.let { bittensorPrimaryHotkey(it, subnet) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // Like iOS after a failed positions sync: fall back to the recommendation instead of blocking the preset.
        logger.log("[BittensorPreset] Positions unavailable")
        logger.log(failure)
        null
    }

    private companion object {
        const val MAX_ATTEMPTS = 2

        const val PORTFOLIO_TIMEOUT_MILLIS = 10_000L
    }
}

/** Recommended, active and gate-passing rows, highest reliable APY first; rows without a reliable APY keep directory order last. */
internal fun bittensorPresetCandidates(
    directory: List<BittensorSubnetValidator>,
    rates: Map<AccountIdKey, BigDecimal>
): List<BittensorSubnetValidator> = directory.filter { it.canSelect && it.recommended }
    .sortedWith(compareByDescending { rates[it.validator.hotkey] })

/**
 * The validator holding the largest stake of the account on [subnet], as iOS picks a position's primary hotkey;
 * equal stakes resolve to the lexicographically smaller hotkey bytes.
 */
internal fun bittensorPrimaryHotkey(positions: List<BittensorSubnetPosition>, subnet: BittensorNetuid.Subnet): AccountIdKey? = positions
    .filter { it.subnetId == subnet && it.alphaInPlanks.signum() > 0 }
    .minWithOrNull(
        compareByDescending<BittensorSubnetPosition> { it.alphaInPlanks }
            .thenComparator { left, right -> compareUnsignedBytes(left.validatorHotkey.value, right.validatorHotkey.value) }
    )
    ?.validatorHotkey

private fun compareUnsignedBytes(left: ByteArray, right: ByteArray): Int {
    for (index in 0 until minOf(left.size, right.size)) {
        val difference = left[index].toUByte().compareTo(right[index].toUByte())
        if (difference != 0) return difference
    }
    return left.size.compareTo(right.size)
}
