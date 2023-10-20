package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus.Inactive.Reason
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.model.StashNoneStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.ValidatorStatus
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

class RelaychainStakeSummaryComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StakeSummaryComponent = RelaychainStakeSummaryComponent(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        stakingSharedComputation = stakingSharedComputation,
    )
}

private class RelaychainStakeSummaryComponent(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val resourceManager: ResourceManager,
) : BaseStakeSummaryComponent(hostContext.scope) {

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    override val state: Flow<StakeSummaryState?> = selectedAccountStakingStateFlow.transformLatest { stakingState ->
        when (stakingState) {
            is StakingState.NonStash -> emit(null)
            is StakingState.Stash.Nominator -> emitAll(nominatorState(stakingState))
            is StakingState.Stash.Validator -> emitAll(validatorState(stakingState))
            is StakingState.Stash.None -> emitAll(neitherState(stakingState))
        }
    }
        .onStart { emit(null) }
        .shareInBackground()

    private suspend fun nominatorState(
        stakingState: StakingState.Stash.Nominator,
    ): Flow<StakeSummaryState> = stakeSummaryState(stakingInteractor.observeNominatorSummary(stakingState, hostContext.scope)) { status ->
        when (status) {
            NominatorStatus.Active -> StakeStatusModel.Active(
                details = string(R.string.staking_nominator_status_alert_active_title) to
                    string(R.string.staking_nominator_status_alert_active_message)
            )

            is NominatorStatus.Inactive -> StakeStatusModel.Inactive(
                details = when (status.reason) {
                    Reason.MIN_STAKE -> string(R.string.staking_nominator_status_alert_inactive_title) to
                        string(R.string.staking_nominator_status_alert_low_stake)

                    Reason.NO_ACTIVE_VALIDATOR -> string(R.string.staking_nominator_status_alert_inactive_title) to
                        string(R.string.staking_nominator_status_alert_no_validators)
                }
            )

            is NominatorStatus.Waiting -> StakeStatusModel.Waiting(
                timeLeft = status.timeLeft,
                messageFormat = R.string.staking_nominator_status_waiting_format,
                details = string(R.string.staking_nominator_status_waiting) to
                    string(R.string.staking_nominator_status_alert_waiting_message)
            )
        }
    }

    private suspend fun validatorState(
        stakingState: StakingState.Stash.Validator
    ): Flow<StakeSummaryState> = stakeSummaryState(stakingInteractor.observeValidatorSummary(stakingState, hostContext.scope)) { status ->
        when (status) {
            ValidatorStatus.ACTIVE -> StakeStatusModel.Active(
                details = string(R.string.staking_nominator_status_alert_active_title) to
                    string(R.string.staking_nominator_status_alert_active_message)
            )

            ValidatorStatus.INACTIVE -> StakeStatusModel.Inactive(
                details = string(R.string.staking_nominator_status_alert_inactive_title) to
                    string(R.string.staking_nominator_status_alert_no_validators)
            )
        }
    }

    private suspend fun neitherState(
        stakingState: StakingState.Stash.None
    ): Flow<StakeSummaryState> = stakeSummaryState(stakingInteractor.observeStashSummary(stakingState, hostContext.scope)) { status ->
        when (status) {
            StashNoneStatus.INACTIVE -> StakeStatusModel.Inactive(
                details = string(R.string.staking_nominator_status_alert_inactive_title) to
                    string(R.string.staking_bonded_inactive)
            )
        }
    }

    private fun <STATUS> stakeSummaryState(
        domainFlow: Flow<StakeSummary<STATUS>>,
        statusMapper: (STATUS) -> StakeStatusModel
    ): Flow<StakeSummaryState> = combine(
        hostContext.assetFlow,
        domainFlow
    ) { asset, summary ->
        StakeSummaryModel(
            totalStaked = mapAmountToAmountModel(summary.activeStake, asset),
            status = statusMapper(summary.status),
        )
    }.withLoading()

    private fun string(@StringRes stringRes: Int) = resourceManager.getString(stringRes)
}
