package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorRootPrimaryHotkey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.existingPresetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.selectBittensorPreset
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RemoteBittensorRootValidatorSource(
    private val backend: BittensorValidatorBackend,
    private val validators: BittensorSubnetValidatorRepository,
    private val logger: io.novasama.substrate_sdk_android.wsrpc.logging.Logger
) : BittensorRootValidatorSource {

    override suspend fun validatorDetails(chain: Chain): Map<AccountIdKey, BittensorRootValidatorDetails> {
        return try {
            val response = backend.rootValidators()
            requireBittensorRootYieldWindows(response)
            mapRootValidatorDetails(chain, response.items.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorRootYield] Validator details unavailable")
            logger.log(e)
            throw e
        }
    }

    /**
     * The subnet preset applied to netuid 0, as on iOS: the held root validator first, otherwise the recommended root validators
     * ranked by their 30-day root APY.
     */
    override suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator? = coroutineScope {
        val rates = async { optionalYields(chain) }
        val existing = validators.existingPresetValidator(chain, BittensorNetuid.Root, bittensorRootPrimaryHotkey(positions), logger)
        val preset = selectBittensorPreset(existing, { validators.directory(chain, BittensorNetuid.Root) }, { rates.await() }, logger) {
            validators.validateRoot(chain, it)
        }
        rates.cancel()
        preset?.validator
    }

    override suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal? {
        return try {
            mapBittensorRootYields(chain, backend.rootValidators())[validator.hotkey]
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorRootYield] Validator metrics unavailable")
            logger.log(e)
            null
        }
    }

    private suspend fun optionalYields(chain: Chain): Map<AccountIdKey, BigDecimal> = try {
        mapBittensorRootYields(chain, backend.rootValidators())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.log("[BittensorPreset] Optional yields unavailable")
        logger.log(e)
        emptyMap()
    }
}

internal fun mapRootValidatorDetails(
    chain: Chain,
    rows: List<BittensorRootValidatorRemote.Validator>
): Map<AccountIdKey, BittensorRootValidatorDetails> = buildMap {
    for (row in rows) {
        val address = row.hotkey ?: continue
        val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: continue
        val rate = row.stakeYield?.d30?.apy?.toBigDecimalOrNull()
            ?.takeIf { it.isNonNegative && row.rootStakeTao?.toBigDecimalOrNull()?.isPositive == true }
        put(hotkey, BittensorRootValidatorDetails(BittensorValidator(hotkey, row.name.orEmpty()), rate))
    }
}

/**
 * The root validator list as iOS makeRootListing builds it: every root validators entry with a positive root stake, which is the
 * row's reported stake, named by its non-blank name or else its address. Entries without root stake cannot earn root yield.
 */
internal fun mapBittensorRootValidators(chain: Chain, response: BittensorRootValidatorRemote): List<BittensorValidator> =
    mapRootListing(chain, response).map { it.first }

/** Names of the root list entries; entries without a non-blank name are absent, callers fall back to the address. */
internal fun mapBittensorRootValidatorNames(chain: Chain, response: BittensorRootValidatorRemote): Map<AccountIdKey, String> =
    mapRootListing(chain, response).mapNotNull { (validator, name) -> name?.let { validator.hotkey to it } }.toMap()

private fun mapRootListing(chain: Chain, response: BittensorRootValidatorRemote): List<Pair<BittensorValidator, String?>> =
    response.items.orEmpty().mapNotNull { row ->
        val stake = row.rootStakeTao?.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: return@mapNotNull null
        val address = row.hotkey ?: return@mapNotNull null
        val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: return@mapNotNull null
        val name = row.name?.trim()?.takeIf(String::isNotBlank)
        BittensorValidator(hotkey, name ?: address, stake) to name
    }.distinctBy { it.first.hotkey }

/** Root yields need the optional stakeYieldWindows component, as iOS makeRootYields does. */
internal fun requireBittensorRootYieldWindows(response: BittensorRootValidatorRemote) {
    val component = response.meta?.components?.stakeYieldWindows
    requireAvailableBittensorComponent(component, "Root yield data")
    parseBittensorTimestamp(requireNotNull(component?.asOf))
}

/** 30-day root APY per hotkey, in reported percent, for entries with a positive root stake. */
internal fun mapBittensorRootYields(chain: Chain, response: BittensorRootValidatorRemote): Map<AccountIdKey, BigDecimal> {
    requireBittensorRootYieldWindows(response)
    return buildMap {
        for (row in response.items.orEmpty()) {
            if (row.rootStakeTao?.toBigDecimalOrNull()?.isPositive != true) continue
            val rate = row.stakeYield?.d30?.apy?.toBigDecimalOrNull()?.takeIf { it.isNonNegative } ?: continue
            val address = row.hotkey ?: continue
            val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: continue
            put(hotkey, rate)
        }
    }
}
