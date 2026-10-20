package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

class RemoteBittensorRootValidatorSource(
    private val backend: BittensorValidatorBackend,
    private val validators: BittensorSubnetValidatorRepository,
    private val logger: io.novasama.substrate_sdk_android.wsrpc.logging.Logger
) : BittensorRootValidatorSource {

    override suspend fun validatorDetails(chain: Chain): Map<AccountIdKey, BittensorRootValidatorDetails> {
        return try {
            val response = backend.rootValidators()
            mapRootValidatorDetails(chain, response.items.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorRootYield] Validator details unavailable")
            logger.log(e)
            throw e
        }
    }

    override suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator? {
        val eligible = validators.rootValidators(chain).filter { it.canSelect }
        val existing = positions.filter { it.stakeInPlanks.isPositive() }
            .maxByOrNull { it.stakeInPlanks }
            ?.validatorHotkey
        eligible.firstOrNull { it.validator.hotkey == existing }
            ?.let { return it.validator }

        val recommendations = backend.recommendedHotkeys(0)
        return recommendations.firstNotNullOfOrNull { address ->
            val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }
                .getOrNull()
            eligible.firstOrNull { it.validator.hotkey == hotkey }
                ?.validator
        }
    }

    override suspend fun annualPercentageYield(chain: Chain, validator: BittensorValidator): BigDecimal? {
        return try {
            val response = backend.rootValidators()
            mapRootValidatorYield(chain, validator, response.items.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorRootYield] Validator metrics unavailable")
            logger.log(e)
            null
        }
    }

    private fun mapRootValidatorYield(
        chain: Chain,
        validator: BittensorValidator,
        rows: List<BittensorRootValidatorRemote.Validator>
    ): BigDecimal? {
        val row = rows.lastOrNull { matchesRootValidatorYield(chain, validator, it) } ?: return null
        return row.stakeYield?.d30?.apy?.toBigDecimalOrNull()
            ?.takeIf { it.isNonNegative }
    }

    private fun matchesRootValidatorYield(
        chain: Chain,
        validator: BittensorValidator,
        row: BittensorRootValidatorRemote.Validator
    ): Boolean {
        if (!hasThirtyDayYieldAndPositiveRootStake(row)) return false
        val address = row.hotkey ?: return false
        val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull()
        return hotkey == validator.hotkey
    }

    private fun hasThirtyDayYieldAndPositiveRootStake(row: BittensorRootValidatorRemote.Validator): Boolean {
        val rootStake = row.rootStakeTao?.toBigDecimalOrNull() ?: return false
        return row.stakeYield?.d30 != null && rootStake.isPositive
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
