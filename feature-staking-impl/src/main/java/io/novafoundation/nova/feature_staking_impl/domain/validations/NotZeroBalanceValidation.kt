package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.controller.SetControllerValidationPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.state.chain
import java.math.BigDecimal

class NotZeroBalanceValidation(
    private val stakingSharedState: StakingSharedState,
    private val assetSourceRegistry: AssetSourceRegistry,
) : Validation<SetControllerValidationPayload, SetControllerValidationFailure> {

    override suspend fun validate(value: SetControllerValidationPayload): ValidationStatus<SetControllerValidationFailure> {
        val chain = stakingSharedState.chain()
        val feeAsset = chain.utilityAsset
        val controllerId = chain.accountIdOf(value.controllerAddress)

        val controllerBalance = assetSourceRegistry.sourceFor(feeAsset).balance.queryAccountBalance(chain, feeAsset, controllerId).free.toBigDecimal()

        return if (controllerBalance > BigDecimal.ZERO) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.WARNING, SetControllerValidationFailure.ZERO_CONTROLLER_BALANCE)
        }
    }
}
