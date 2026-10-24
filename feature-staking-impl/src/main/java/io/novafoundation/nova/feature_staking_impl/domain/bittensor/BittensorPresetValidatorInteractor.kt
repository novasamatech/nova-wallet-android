package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mapBittensorRootYields
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/** The subnet directory together with its automatic preset. [preset] is null when no validator qualifies. */
class BittensorValidatorPreset(
    val directory: List<BittensorSubnetValidator>,
    val preset: BittensorSubnetValidator?
)

/**
 * Automatic presets only. An explicitly selected hotkey must never be silently replaced.
 *
 * An existing position keeps its primary validator unless the chain reports it inactive; otherwise the
 * preset is the recommended validator with the highest reliable APY that is active on the subnet, with a single fallback to the
 * runner-up.
 */
class BittensorPresetValidatorInteractor @Inject constructor(
    private val repository: BittensorSubnetValidatorRepository,
    private val yields: BittensorAlphaYieldRepository,
    private val backend: BittensorValidatorBackend,
    private val chains: ChainRegistry,
    private val portfolioRepository: BittensorPortfolioRepository,
    private val logger: Logger
) {

    /**
     * Fails only when no existing validator qualifies and the directory or recommendations cannot be loaded, so callers can
     * offer a retry.
     */
    suspend fun preset(subnet: BittensorNetuid.Subnet): BittensorValidatorPreset = coroutineScope {
        val chain = chains.getChain(ChainGeneses.BITTENSOR)
        // The directory and yields are independent of the positions; load them alongside instead of after them.
        val directory = async { optionalDirectory(chain, subnet) }
        val rates = async { optionalYields(chain, subnet) }
        val existing = activeExistingValidator(chain, subnet, existingPrimaryHotkey(subnet))
        val preset = existing ?: recommendedPreset(chain, subnet, directory.await().getOrThrow(), rates)
        rates.cancel()
        // A manual pick on the stake setup is looked up in this directory; the held validator is kept even without one.
        val rows = directory.await().getOrNull()?.validators.orEmpty()
        BittensorValidatorPreset(rows.withPreset(preset), preset)
    }

    /**
     * The subnet preset applied to netuid 0: the held root validator first, otherwise the recommended root validators
     * ranked by their 30-day root APY. The directory is read only when no held validator qualifies.
     */
    suspend fun rootPreset(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator? = coroutineScope {
        val rates = async { optionalYields(chain, BittensorNetuid.Root) }
        val existing = activeExistingValidator(chain, BittensorNetuid.Root, bittensorRootPrimaryHotkey(positions))
        val preset = existing ?: recommendedPreset(chain, BittensorNetuid.Root, repository.directory(chain, BittensorNetuid.Root), rates)
        rates.cancel()
        preset?.validator
    }

    /**
     * The first of the two best [bittensorPresetCandidates] that passes the chain check, shared by subnets and root.
     * A failed recommendations read fails the preset.
     */
    private suspend fun recommendedPreset(
        chain: Chain,
        netuid: BittensorNetuid,
        directory: BittensorValidatorDirectory,
        rates: Deferred<Map<AccountIdKey, BigDecimal>>
    ): BittensorSubnetValidator? {
        directory.recommendationFailure?.let { throw it }
        for (candidate in bittensorPresetCandidates(directory.validators, rates.await()).take(MAX_PRESET_ATTEMPTS)) {
            try {
                repository.validate(chain, netuid, candidate.validator)
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

    private suspend fun optionalDirectory(chain: Chain, subnet: BittensorNetuid.Subnet): Result<BittensorValidatorDirectory> = try {
        Result.success(repository.directory(chain, subnet))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private suspend fun optionalYields(chain: Chain, netuid: BittensorNetuid): Map<AccountIdKey, BigDecimal> = try {
        when (netuid) {
            BittensorNetuid.Root -> mapBittensorRootYields(chain, backend.rootValidators())
            is BittensorNetuid.Subnet -> yields.alphaYields(chain, netuid)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        logger.log("[BittensorPreset] Optional yields unavailable")
        logger.log(failure)
        emptyMap()
    }

    /** [hotkey] of an existing position as the chain reports it, or null when unreadable or inactive. */
    private suspend fun activeExistingValidator(chain: Chain, netuid: BittensorNetuid, hotkey: AccountIdKey?): BittensorSubnetValidator? {
        if (hotkey == null) return null
        return try {
            repository.chainValidator(chain, netuid, hotkey).takeUnless { it.inactive }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.log("[BittensorPreset] Existing validator unavailable")
            logger.log(failure)
            null
        }
    }

    /** Waits for positions to load or fail, so a held validator is never replaced. */
    private suspend fun existingPrimaryHotkey(subnet: BittensorNetuid.Subnet): AccountIdKey? = try {
        portfolioRepository.observePortfolio().first { it.dataStatus !is BittensorDataStatus.Loading }
            .positions
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

    private fun List<BittensorSubnetValidator>.withPreset(preset: BittensorSubnetValidator?): List<BittensorSubnetValidator> {
        val hotkey = preset?.validator?.hotkey ?: return this
        if (none { it.validator.hotkey == hotkey }) return listOf(preset) + this
        return map { if (it.validator.hotkey == hotkey) preset else it }
    }
}

private const val MAX_PRESET_ATTEMPTS = 2

/**
 * Recommended, active and gate-passing rows, highest reliable APY first. Equal APYs and rows without a reliable APY follow the
 * ranking, so the first ranked recommendation with the highest rate wins.
 */
internal fun bittensorPresetCandidates(
    directory: List<BittensorSubnetValidator>,
    rates: Map<AccountIdKey, BigDecimal>
): List<BittensorSubnetValidator> = directory.filter { it.canSelect && it.recommended }
    .sortedWith(
        compareByDescending<BittensorSubnetValidator> { rates[it.validator.hotkey] }
            .thenBy(nullsLast<Int>()) { it.recommendationRank }
    )

/**
 * The validator holding the largest stake of the account on [subnet], as iOS picks a position's primary hotkey;
 * equal stakes resolve to the lexicographically smaller hotkey bytes.
 */
internal fun bittensorPrimaryHotkey(positions: List<BittensorSubnetPosition>, subnet: BittensorNetuid.Subnet): AccountIdKey? = positions
    .filter { it.subnetId == subnet }
    .primaryHotkey(BittensorSubnetPosition::validatorHotkey, BittensorSubnetPosition::alphaInPlanks)

/** The root validator holding the largest root stake of the account, with the same tie-break as [bittensorPrimaryHotkey]. */
internal fun bittensorRootPrimaryHotkey(positions: List<BittensorRootPosition>): AccountIdKey? =
    positions.primaryHotkey(BittensorRootPosition::validatorHotkey, BittensorRootPosition::stakeInPlanks)

private fun <T> List<T>.primaryHotkey(hotkey: (T) -> AccountIdKey, stake: (T) -> BigInteger): AccountIdKey? = filter { stake(it).isPositive() }
    .minWithOrNull(
        compareByDescending(stake)
            .thenComparator { left, right -> compareUnsignedBytes(hotkey(left).value, hotkey(right).value) }
    )
    ?.let(hotkey)

private fun compareUnsignedBytes(left: ByteArray, right: ByteArray): Int {
    for (index in 0 until minOf(left.size, right.size)) {
        val difference = left[index].toUByte().compareTo(right[index].toUByte())
        if (difference != 0) return difference
    }
    return left.size.compareTo(right.size)
}
