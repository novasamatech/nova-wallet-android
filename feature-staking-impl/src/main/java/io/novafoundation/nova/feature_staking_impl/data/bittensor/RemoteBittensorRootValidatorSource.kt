package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

class RemoteBittensorRootValidatorSource(
    private val backend: BittensorValidatorBackend,
    private val validators: BittensorSubnetValidatorRepository,
    private val logger: io.novasama.substrate_sdk_android.wsrpc.logging.Logger
) : BittensorRootValidatorSource {

    override suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator? {
        val eligible = validators.rootValidators(chain).filter { it.selectable }
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
            val response = backend.rootValidator(chain.addressOf(validator.hotkey.value))
            val window = response.validator?.stakeYield?.h24 ?: return null
            if (window.coverage !in setOf("FULL", "PARTIAL") || window.valueQuality !in setOf("REPORTED", "DERIVED")) return null
            window.apy?.toBigDecimalOrNull()
                ?.takeIf { it > BigDecimal("-100") }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorRootYield] Validator metrics unavailable")
            logger.log(e)
            null
        }
    }
}
