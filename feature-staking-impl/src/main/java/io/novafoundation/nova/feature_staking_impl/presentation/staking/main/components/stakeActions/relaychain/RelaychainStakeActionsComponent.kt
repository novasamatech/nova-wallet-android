package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_ADD_PROXY
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_CONTROLLER
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PAYOUTS
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PROXIES
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_REWARD_DESTINATION
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_VALIDATORS
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.addStakingProxy
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.bondMore
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.controller
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.payouts
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.rewardDestination
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.stakingProxies
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.unbond
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.validators
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class RelaychainStakeActionsComponentFactory(
    private val stakingSharedComputation: StakingSharedComputation,
    private val resourceManager: ResourceManager,
    private val stakeActionsValidations: Map<String, StakeActionsValidationSystem>,
    private val router: StakingRouter,
    private val accountRepository: AccountRepository,
    private val getProxyRepository: GetProxyRepository
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = RelaychainStakeActionsComponent(
        stakingSharedComputation = stakingSharedComputation,
        resourceManager = resourceManager,
        router = router,
        stakeActionsValidations = stakeActionsValidations,
        stakingOption = stakingOption,
        hostContext = hostContext,
        accountRepository = accountRepository,
        getProxyRepository = getProxyRepository
    )
}

private class RelaychainStakeActionsComponent(
    private val stakingSharedComputation: StakingSharedComputation,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val stakeActionsValidations: Map<String, StakeActionsValidationSystem>,
    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
    private val accountRepository: AccountRepository,
    private val getProxyRepository: GetProxyRepository
) : StakeActionsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val stakingProxiesQuantity = accountRepository.selectedMetaAccountFlow()
        .flatMapLatest { metaAccount ->
            getProxiesQuantity(metaAccount)
        }

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    override val state = combineTransform(
        selectedAccountStakingStateFlow,
        stakingProxiesQuantity
    ) { stakingState, proxiesQuantity ->
        if (stakingState is StakingState.Stash) {
            emit(StakeActionsState(availableActionsFor(stakingState, proxiesQuantity)))
        } else {
            emit(null)
        }
    }
        .shareInBackground()

    override fun onAction(action: StakeActionsAction) {
        when (action) {
            is StakeActionsAction.ActionClicked -> manageStakeActionChosen(action.action)
        }
    }

    private suspend fun getProxiesQuantity(metaAccount: MetaAccount): Flow<Int> {
        val chain = stakingOption.assetWithChain.chain
        if (chain.supportProxy.not()) return flowOf(0)

        val accountId = metaAccount.requireAccountIdIn(chain)
        return getProxyRepository.proxiesQuantityByTypeFlow(chain, accountId, ProxyType.Staking)
    }

    private fun manageStakeActionChosen(manageStakeAction: ManageStakeAction) {
        val validationSystem = stakeActionsValidations[manageStakeAction.id]

        if (validationSystem != null) {
            launch {
                val stakingState = selectedAccountStakingStateFlow.filterIsInstance<StakingState.Stash>().first()
                val payload = StakeActionsValidationPayload(stakingState)

                hostContext.validationExecutor.requireValid(
                    validationSystem = validationSystem,
                    payload = payload,
                    errorDisplayer = hostContext.errorDisplayer,
                    validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                    scope = hostContext.scope
                ) {
                    navigateToAction(manageStakeAction)
                }
            }
        } else {
            navigateToAction(manageStakeAction)
        }
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action.id) {
            SYSTEM_MANAGE_PAYOUTS -> router.openPayouts()
            SYSTEM_MANAGE_STAKING_BOND_MORE -> router.openBondMore()
            SYSTEM_MANAGE_STAKING_UNBOND -> router.openSelectUnbond()
            SYSTEM_MANAGE_CONTROLLER -> router.openControllerAccount()
            SYSTEM_MANAGE_VALIDATORS -> router.openCurrentValidators()
            SYSTEM_MANAGE_REWARD_DESTINATION -> router.openChangeRewardDestination()
            SYSTEM_ADD_PROXY -> router.openAddStakingProxy()
            SYSTEM_MANAGE_PROXIES -> router.openStakingProxyList()
        }
    }

    private fun availableActionsFor(stakingState: StakingState.Stash, proxiesQuantity: Int): List<ManageStakeAction> = buildList {
        add(ManageStakeAction.bondMore(resourceManager))
        add(ManageStakeAction.unbond(resourceManager))
        add(ManageStakeAction.rewardDestination(resourceManager))

        if (stakingState !is StakingState.Stash.None) {
            add(ManageStakeAction.payouts(resourceManager))
        }

        if (stakingState !is StakingState.Stash.Validator) {
            add(ManageStakeAction.validators(resourceManager))
        }

        if (stakingOption.chain.supportProxy) {
            add(proxiesAction(proxiesQuantity))
        }

        add(ManageStakeAction.controller(resourceManager))
    }

    private fun proxiesAction(proxiesQuantity: Int): ManageStakeAction {
        return if (proxiesQuantity == 0) {
            ManageStakeAction.addStakingProxy(resourceManager)
        } else {
            ManageStakeAction.stakingProxies(resourceManager, proxiesQuantity.toString())
        }
    }
}
