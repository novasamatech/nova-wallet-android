package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

class ManualMultiStakingSelectionType(
    private val selectedType: SingleStakingProperties,
) : MultiStakingSelectionType {

    override suspend fun validationSystem(): StartMultiStakingValidationSystem {
        return selectedType.validationSystem
    }

    override suspend fun availableBalance(asset: Asset): Balance {
        return selectedType.availableBalance(asset)
    }

    override suspend fun updateSelectionFor(stake: Balance) {
        // When selection is selected manually, we do not update it based on entered amount
    }
}
