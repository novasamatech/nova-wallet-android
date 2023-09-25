package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.CurrentCollatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.DelegatedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.DelegatedCollatorGroup
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState.COLLATOR_NOT_ACTIVE
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState.TOO_LOW_STAKE
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Active
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Elected
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Inactive
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Waiting
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.formatStakeTargetRewardsOrNull
import io.novafoundation.nova.feature_staking_impl.presentation.openStartStaking
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.model.ManageCollatorsAction
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrentCollatorsViewModel(
    private val router: ParachainStakingRouter,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
    private val currentCollatorsInteractor: CurrentCollatorInteractor,
    private val selectedChainStale: AnySelectedAssetOptionSharedState,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val collatorsUseCase: CollatorsUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    tokenUseCase: TokenUseCase,
) : CurrentStakeTargetsViewModel() {

    private val groupedCurrentCollatorsFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .filterIsInstance<DelegatorState.Delegator>()
        .flatMapLatest(currentCollatorsInteractor::currentCollatorsFlow)
        .shareInBackground()

    private val flattenCurrentCollators = groupedCurrentCollatorsFlow
        .map { it.toValueList() }
        .inBackground()
        .share()

    override val currentStakeTargetsFlow = combine(
        groupedCurrentCollatorsFlow,
        tokenUseCase.currentTokenFlow()
    ) { gropedList, token ->
        val chain = selectedChainStale.chain()

        gropedList.mapKeys { (statusGroup, _) -> mapDelegatedCollatorStatusToUiModel(statusGroup) }
            .mapValues { (_, nominatedValidators) -> nominatedValidators.map { mapDelegatedCollatorToUiModel(chain, it, token) } }
            .toListWithHeaders()
    }
        .withLoading()
        .shareInBackground()

    override val warningFlow = flattenCurrentCollators.map { collators ->
        val hasNonRewardedDelegations = collators.any { it.delegationStatus == TOO_LOW_STAKE || it.delegationStatus == COLLATOR_NOT_ACTIVE }

        if (hasNonRewardedDelegations) {
            resourceManager.getString(R.string.staking_parachain_your_collaotrs_no_rewards)
        } else {
            null
        }
    }
        .inBackground()
        .share()

    override val titleFlow: Flow<String> = flowOf {
        resourceManager.getString(R.string.staking_parachain_your_collators)
    }

    val selectManageCollatorsAction = actionAwaitableMixinFactory.create<Unit, ManageCollatorsAction>()

    override fun stakeTargetInfoClicked(address: String) {
        launch {
            val payload = withContext(Dispatchers.Default) {
                val allCollators = flattenCurrentCollators.first()
                val selectedCollator = allCollators.first { it.collator.address == address }

                val stakeTarget = mapCollatorToDetailsParcelModel(selectedCollator.collator, selectedCollator.delegationStatus)

                StakeTargetDetailsPayload.parachain(stakeTarget, collatorsUseCase)
            }

            router.openCollatorDetails(payload)
        }
    }

    override fun backClicked() {
        router.back()
    }

    override fun changeClicked() {
        launch {
            when (selectManageCollatorsAction.awaitAction()) {
                ManageCollatorsAction.BOND_MORE -> router.openStartStaking(StartParachainStakingMode.BOND_MORE)
                ManageCollatorsAction.UNBOND -> router.openUnbond()
            }
        }
    }

    private suspend fun mapDelegatedCollatorToUiModel(
        chain: Chain,
        delegatedCollator: DelegatedCollator,
        token: Token
    ): SelectedStakeTargetModel {
        val collator = delegatedCollator.collator

        return SelectedStakeTargetModel(
            addressModel = iconGenerator.createAccountAddressModel(
                chain = chain,
                address = collator.address,
                name = collator.identity?.display
            ),
            nominated = mapAmountToAmountModel(delegatedCollator.delegation, token),
            isOversubscribed = delegatedCollator.delegationStatus == TOO_LOW_STAKE,
            isSlashed = false,
            apy = formatStakeTargetRewardsOrNull(collator.apr)
        )
    }

    private fun mapDelegatedCollatorStatusToUiModel(statusGroup: DelegatedCollatorGroup) = when (statusGroup) {
        is DelegatedCollatorGroup.Active -> SelectedStakeTargetStatusModel.Active(
            resourceManager = resourceManager,
            groupSize = statusGroup.numberOfCollators,
            description = R.string.staking_parachain_your_collators_active
        )
        is DelegatedCollatorGroup.Elected -> SelectedStakeTargetStatusModel.Elected(
            resourceManager = resourceManager,
            description = R.string.staking_parachain_your_collators_elected
        )

        is DelegatedCollatorGroup.Inactive -> SelectedStakeTargetStatusModel.Inactive(
            resourceManager = resourceManager,
            groupSize = statusGroup.numberOfCollators,
            description = R.string.staking_parachain_your_collators_inactive,
        )

        is DelegatedCollatorGroup.WaitingForNextEra -> SelectedStakeTargetStatusModel.Waiting(
            resourceManager = resourceManager,
            title = resourceManager.getString(R.string.staking_parachain_your_collators_waiting_title, statusGroup.numberOfCollators),
            description = R.string.staking_parachain_your_collators_waiting
        )
    }
}
