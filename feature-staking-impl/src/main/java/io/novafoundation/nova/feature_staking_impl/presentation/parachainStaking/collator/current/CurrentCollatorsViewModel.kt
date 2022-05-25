package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.list.toValueList
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.CurrentCollatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.DelegatedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current.DelegatedCollatorGroup
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
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class CurrentCollatorsViewModel(
    private val router: ParachainStakingRouter,
    private val resourceManager: ResourceManager,
    private val iconGenerator: AddressIconGenerator,
    private val currentCollatorsInteractor: CurrentCollatorInteractor,
    private val selectedChainStale: SingleAssetSharedState,
    private val delegatorStateUseCase: DelegatorStateUseCase,
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
            title =  resourceManager.getString(R.string.staking_parachain_your_collators_waiting_title, statusGroup.numberOfCollators),
            description = R.string.staking_parachain_your_collators_waiting
        )
    }

    override fun stakeTargetInfoClicked(address: String) {
//        val payload = withContext(Dispatchers.Default) {
//            val accountId = address.toHexAccountId()
//            val allValidators = flattenCurrentCollators.first()
//
//            val nominatedValidator = allValidators.first { it.validator.accountIdHex == accountId }
//
//            val stakeTarget = mapValidatorToValidatorDetailsWithStakeFlagParcelModel(nominatedValidator)
//            StakeTargetDetailsPayload.relaychain(stakeTarget, stakingInteractor)
//        }
//
//        router.openValidatorDetails(payload)
    }

    override fun backClicked() {
        router.back()
    }

    override fun changeClicked() {
        // TODO
    }
}
