package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

interface SingleStakingProperties {

    fun availableBalance(asset: Asset): Balance

    val recommendation: SingleStakingRecommendation

    val validationSystem: StartMultiStakingValidationSystem

    suspend fun minStake(): Balance
}

interface SingleStakingRecommendation {

    suspend fun recommendedSelection(): StartMultiStakingSelection
}

