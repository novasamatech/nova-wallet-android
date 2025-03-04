package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.copyIntoCurrent
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStore
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

class ManualMultiStakingSelectionType(
    private val selectedType: SingleStakingProperties,
    private val selectionStore: StartMultiStakingSelectionStore
) : MultiStakingSelectionType {

    override suspend fun validationSystem(selection: StartMultiStakingSelection): StartMultiStakingValidationSystem {
        return ValidationSystem {
            selectedType.validationSystem.copyIntoCurrent()

            enoughAvailableToStake()
        }
    }

    override suspend fun availableBalance(asset: Asset): Balance {
        return selectedType.availableBalance(asset)
    }

    override suspend fun maxAmountToStake(asset: Asset): Balance {
        return selectedType.maximumToStake(asset)
    }

    override suspend fun updateSelectionFor(stake: Balance) {
        selectionStore.updateStake(stake)
    }

    private fun StartMultiStakingValidationSystemBuilder.enoughAvailableToStake() {
        sufficientBalance(
            available = { it.amountOf(availableBalance(it.asset)) },
            amount = { it.amountOf(it.selection.stake) },
            error = { StartMultiStakingValidationFailure.NotEnoughAvailableToStake },
            fee = { it.fee }
        )
    }
}
