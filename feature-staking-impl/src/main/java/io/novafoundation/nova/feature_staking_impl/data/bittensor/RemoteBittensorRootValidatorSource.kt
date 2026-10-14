package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.ext.accountIdOf
import java.math.BigDecimal

class RemoteBittensorRootValidatorSource(
    private val backend: BittensorValidatorBackend,
    private val validators: BittensorSubnetValidatorRepository
) : BittensorRootValidatorSource {

    override suspend fun preferredValidator(chain: Chain, positions: List<BittensorRootPosition>): BittensorValidator? {
        val eligible = validators.rootValidators(chain).filter { it.selectable }
        val existing = positions.filter { it.stakeInPlanks.signum() > 0 }.maxByOrNull { it.stakeInPlanks }?.validatorHotkey
        eligible.firstOrNull { it.validator.hotkey == existing }?.let { return it.validator }

        val recommendations = backend.recommendedHotkeys(0)
        return recommendations.firstNotNullOfOrNull { address ->
            val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull()
            eligible.firstOrNull { it.validator.hotkey == hotkey }?.validator
        }
    }

    override suspend fun annualPercentageYield(): BigDecimal? = null
}
