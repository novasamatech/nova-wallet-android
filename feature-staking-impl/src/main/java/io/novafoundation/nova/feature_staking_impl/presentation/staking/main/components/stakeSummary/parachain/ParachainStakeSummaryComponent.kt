package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain

import android.util.Log
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex

class ParachainStakeSummaryComponentFactory(
    private val resourceManager: ResourceManager,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext,
    ): StakeSummaryComponent = ParachainStakeSummaryComponent(
        resourceManager = resourceManager,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        delegatorStateUseCase = delegatorStateUseCase,
        selectedAccountUseCase = selectedAccountUseCase
    )
}

private class ParachainStakeSummaryComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,

    assetWithChain: SingleAssetSharedState.AssetWithChain,
    hostContext: ComponentHostContext,
) : BaseStakeSummaryComponent(hostContext.scope) {

    override val state: Flow<StakeSummaryState?> = selectedAccountUseCase.selectedMetaAccountFlow()
        .transformLatest { account ->
            emit(null) // hide UI until state of delegator is determined

            val stateFlow = combine(
                delegatorStateUseCase.delegatorStateFlow(account, assetWithChain.chain, assetWithChain.asset),
                hostContext.assetFlow,
                ::Pair
            )
                .withIndex()
                .transformLatest { (index, delegatorWithAsset) ->
                    val (delegator, asset) = delegatorWithAsset

                    if (delegator is DelegatorState.Delegator) {
                        // first loading of summary might take a while - show loading.
                        // We do not show loading for subsequent updates since there is already some info on the screen from the first load
                        if (index == 0) {
                            emit(LoadingState.Loading())
                        }

                        val summaryFlow = delegatorSummaryStateFlow(delegator, asset).map { LoadingState.Loaded(it) }

                        emitAll(summaryFlow)
                    }
                }

            emitAll(stateFlow)
        }
        .onStart { emit(null) }
        .catch { Log.e(this@ParachainStakeSummaryComponent.LOG_TAG, "Failed to construct state", it) }
        .shareInBackground()

    private fun delegatorSummaryStateFlow(delegatorState: DelegatorState.Delegator, asset: Asset): Flow<StakeSummaryModel> {
        return flowOf {
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
