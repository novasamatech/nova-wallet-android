package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface SingleSelectStakingRewardEstimationComponentFactory {
    
    fun create(
        computationalScope: ComputationalScope,
        assetFlow: Flow<Asset>,
        selectedAmount: Flow<BigDecimal>,
        selectedTarget: Flow<AccountIdKey?>
    ): SingleSelectStakingRewardEstimationComponent
}
