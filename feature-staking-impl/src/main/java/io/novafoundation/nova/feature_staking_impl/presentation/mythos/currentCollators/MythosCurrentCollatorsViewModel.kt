package io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.MythosCurrentCollatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model.CurrentMythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model.MythosDelegationStatus
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.CurrentStakeTargetsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.actions.ManageCurrentStakeTargetsAction
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Active
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.Inactive
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.formatStakeTargetRewardsOrNull
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details.mythos
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.accountIdKeyOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MythosCurrentCollatorsViewModel(
    private val router: MythosStakingRouter,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
    private val currentCollatorsInteractor: MythosCurrentCollatorsInteractor,
    private val stakingSharedState: AnySelectedAssetOptionSharedState,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    tokenUseCase: TokenUseCase,
) : CurrentStakeTargetsViewModel() {

    private val groupedCurrentCollatorsFlow = currentCollatorsInteractor.currentCollatorsFlow()
        .shareInBackground()

    private val flattenCurrentCollators = groupedCurrentCollatorsFlow
        .map { it.toValueList() }
        .shareInBackground()

    override val currentStakeTargetsFlow = combine(
        groupedCurrentCollatorsFlow,
        tokenUseCase.currentTokenFlow()
    ) { gropedList, token ->
        val chain = stakingSharedState.chain()

        gropedList.mapKeys { (statusGroup, collators) -> mapGroupToUi(statusGroup, collators.size) }
            .mapValues { (_, nominatedValidators) -> nominatedValidators.map { mapCurrentCollatorToUi(chain, it, token) } }
            .toListWithHeaders()
    }
        .withLoading()
        .shareInBackground()

    override val warningFlow = groupedCurrentCollatorsFlow.map { collatorsByGroup ->
        val hasNonRewardedDelegations = MythosDelegationStatus.NOT_ACTIVE in collatorsByGroup

        if (hasNonRewardedDelegations) {
            resourceManager.getString(R.string.staking_parachain_your_collaotrs_no_rewards)
        } else {
            null
        }
    }
        .shareInBackground()

    override val titleFlow: Flow<String> = flowOf {
        resourceManager.getString(R.string.staking_parachain_your_collators)
    }

    val selectManageCurrentStakeTargetsAction = actionAwaitableMixinFactory.create<Unit, ManageCurrentStakeTargetsAction>()

    override fun stakeTargetInfoClicked(address: String) = launchUnit {
        val chain = stakingSharedState.chain()
        val accountId = chain.accountIdKeyOf(address)

        val allCollators = flattenCurrentCollators.first()
        val clickedCollator = allCollators.first { it.collator.accountId == accountId }

        val payload = StakeTargetDetailsPayload.mythos(clickedCollator.collator)
        router.openCollatorDetails(payload)
    }

    override fun backClicked() {
        router.back()
    }

    override fun changeClicked() {
        launch {
            when (selectManageCurrentStakeTargetsAction.awaitAction()) {
                ManageCurrentStakeTargetsAction.BOND_MORE -> router.openBondMore()
                ManageCurrentStakeTargetsAction.UNBOND -> router.openUnbond()
            }
        }
    }

    private suspend fun mapCurrentCollatorToUi(
        chain: Chain,
        currentCollator: CurrentMythosCollator,
        token: Token
    ): SelectedStakeTargetModel {
        val collator = currentCollator.collator

        return SelectedStakeTargetModel(
            addressModel = iconGenerator.collatorAddressModel(collator, chain),
            nominated = mapAmountToAmountModel(currentCollator.userStake, token),
            isOversubscribed = false,
            isSlashed = false,
            apy = formatStakeTargetRewardsOrNull(collator.apr)
        )
    }

    private fun mapGroupToUi(status: MythosDelegationStatus, groupSize: Int) = when (status) {
        MythosDelegationStatus.ACTIVE -> SelectedStakeTargetStatusModel.Active(
            resourceManager = resourceManager,
            groupSize = groupSize,
            description = R.string.staking_parachain_your_collators_active
        )

        MythosDelegationStatus.NOT_ACTIVE -> SelectedStakeTargetStatusModel.Inactive(
            resourceManager = resourceManager,
            groupSize = groupSize,
            description = R.string.staking_parachain_your_collators_inactive,
        )
    }
}
