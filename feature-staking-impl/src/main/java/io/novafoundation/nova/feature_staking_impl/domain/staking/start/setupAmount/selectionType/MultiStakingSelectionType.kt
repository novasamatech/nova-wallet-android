package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface MultiStakingSelectionType {

    suspend fun validationSystem(selection: StartMultiStakingSelection): StartMultiStakingValidationSystem

    suspend fun availableBalance(asset: Asset): Balance

    suspend fun maxAmountToStake(asset: Asset): Balance

    suspend fun updateSelectionFor(stake: Balance)
}
