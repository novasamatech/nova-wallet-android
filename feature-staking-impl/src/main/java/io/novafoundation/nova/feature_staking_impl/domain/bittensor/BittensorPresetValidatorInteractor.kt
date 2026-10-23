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

/** The subnet directory together with its automatic preset. [preset] is null when no validator qualifies. */
class BittensorValidatorPreset(
    val directory: List<BittensorSubnetValidator>,
    val preset: BittensorSubnetValidator?
)

/**
 * Automatic presets only. An explicitly selected hotkey must never be silently replaced.
 *
 * Mirrors iOS (NW-2303): an existing position keeps its primary validator unless the chain reports it inactive; otherwise the
 * preset is the recommended validator with the highest reliable APY that is active on the subnet, with a single fallback to the
 * runner-up.
 */
class BittensorPresetValidatorInteractor @Inject constructor(
    private val repository: BittensorSubnetValidatorRepository,
    private val yields: BittensorAlphaYieldRepository,
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
        val existing = repository.existingPresetValidator(chain, subnet, existingPrimaryHotkey(subnet), logger)
        val preset = selectBittensorPreset(existing, { directory.await().getOrThrow() }, { rates.await() }, logger) {
            repository.validate(chain, subnet, it)
        }
        rates.cancel()
        // A manual pick on the stake setup is looked up in this directory; the held validator is kept even without one.
        val rows = directory.await().getOrNull()?.validators.orEmpty()
        BittensorValidatorPreset(rows.withPreset(preset), preset)
    }

    /** Quick pick: the preset first, followed by the rest of the directory. Fails when nothing qualifies. */
    suspend fun validators(subnet: BittensorNetuid.Subnet): List<BittensorSubnetValidator> {
        val result = preset(subnet)
        val preset = checkNotNull(result.preset) { "No eligible recommended validator in this subnet" }
        return listOf(preset) + result.directory.filter { it.validator.hotkey != preset.validator.hotkey }
    }

    private suspend fun optionalDirectory(chain: Chain, subnet: BittensorNetuid.Subnet): Result<BittensorValidatorDirectory> = try {
        Result.success(repository.directory(chain, subnet))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
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

    /** Waits for positions to load or fail, as iOS requests the preset only then, so a held validator is never replaced. */
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
 * The automatic preset, shared by subnets and root as iOS shares SubtensorValidatorPresetOperationFactory: the [existing] position's
 * primary validator unless the chain reports it inactive, otherwise the first of the two best [bittensorPresetCandidates] that passes
 * [validate]. [directory] and [rates] are read only when no existing validator qualifies; a failed recommendations read fails the preset.
 */
internal suspend fun selectBittensorPreset(
    existing: BittensorSubnetValidator?,
    directory: suspend () -> BittensorValidatorDirectory,
    rates: suspend () -> Map<AccountIdKey, BigDecimal>,
    logger: Logger,
    validate: suspend (BittensorValidator) -> Unit
): BittensorSubnetValidator? {
    existing?.takeUnless { it.inactive }?.let { return it }
    val loaded = directory()
    loaded.recommendationFailure?.let { throw it }
    for (candidate in bittensorPresetCandidates(loaded.validators, rates()).take(MAX_PRESET_ATTEMPTS)) {
        try {
            validate(candidate.validator)
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

/**
 * Recommended, active and gate-passing rows, highest reliable APY first. Equal APYs and rows without a reliable APY follow the
 * ranking, as iOS highestYield takes the first ranked recommendation with the highest rate.
 */
internal fun bittensorPresetCandidates(
    directory: List<BittensorSubnetValidator>,
    rates: Map<AccountIdKey, BigDecimal>
): List<BittensorSubnetValidator> = directory.filter { it.canSelect && it.recommended }
    .sortedWith(
        compareByDescending<BittensorSubnetValidator> { rates[it.validator.hotkey] }
            .thenBy(nullsLast<Int>()) { it.recommendationRank }
    )

/** [hotkey] of an existing position as the chain reports it, or null when unavailable, as iOS createItemWrapper. */
internal suspend fun BittensorSubnetValidatorRepository.existingPresetValidator(
    chain: Chain,
    netuid: BittensorNetuid,
    hotkey: AccountIdKey?,
    logger: Logger
): BittensorSubnetValidator? {
    if (hotkey == null) return null
    return try {
        chainValidator(chain, netuid, hotkey)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        logger.log("[BittensorPreset] Existing validator unavailable")
        logger.log(failure)
        null
    }
}

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

/** The root validator holding the largest root stake of the account, with the same tie-break as [bittensorPrimaryHotkey]. */
internal fun bittensorRootPrimaryHotkey(positions: List<BittensorRootPosition>): AccountIdKey? = positions
    .filter { it.stakeInPlanks.signum() > 0 }
    .minWithOrNull(
        compareByDescending<BittensorRootPosition> { it.stakeInPlanks }
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
