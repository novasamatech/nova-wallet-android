package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class ParachainStakeSummaryComponentFactory(
    private val resourceManager: ResourceManager,
    private val delegatorStateUseCase: DelegatorStateUseCase,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext,
    ): StakeSummaryComponent = ParachainStakeSummaryComponent(
        resourceManager = resourceManager,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        delegatorStateUseCase = delegatorStateUseCase,
    )
}

private class ParachainStakeSummaryComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,

    assetWithChain: SingleAssetSharedState.AssetWithChain,
    private val hostContext: ComponentHostContext,
) : BaseStakeSummaryComponent(hostContext.scope) {

    override val state: Flow<StakeSummaryState?> = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = assetWithChain,
        stateProducer = ::delegatorSummaryStateFlow
    )
        .shareInBackground()

    private fun delegatorSummaryStateFlow(delegatorState: DelegatorState.Delegator): Flow<StakeSummaryModel> {
        return hostContext.assetFlow.mapLatest { asset ->
            StakeSummaryModel(
                totalStaked = mapAmountToAmountModel(delegatorState.total, asset),

                // TODO stake status
                status = StakeStatusModel.Active(
                    details = resourceManager.getString(R.string.staking_nominator_status_alert_active_title) to
                        resourceManager.getString(R.string.staking_parachain_delegator_status_active_message)
                )
            )
        }
    }
}
