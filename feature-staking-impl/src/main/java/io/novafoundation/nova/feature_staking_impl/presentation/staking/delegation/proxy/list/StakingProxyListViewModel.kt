package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_BIG
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.StakingProxyListInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.model.StakingProxyAccount
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model.StakingProxyGroupRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model.StakingProxyRvItem
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyPayload
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StakingProxyListViewModel(
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val externalActions: ExternalActions.Presentation,
    private val accountRepository: AccountRepository,
    private val stakingProxyListInteractor: StakingProxyListInteractor,
    private val resourceManager: ResourceManager,
    private val stakingRouter: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator
) : BaseViewModel(), ExternalActions by externalActions {

    val selectedMetaAccount = accountRepository.selectedMetaAccountFlow()
        .shareInBackground()

    val proxies = selectedMetaAccount.flatMapLatest {
        val chain = selectedAssetState.chain()
        val accountId = it.requireAccountIdIn(chain)
        stakingProxyListInteractor.stakingProxyListFlow(chain, accountId)
    }
        .shareInBackground()

    val proxyModels: Flow<List<Any>> = proxies.map {
        mapToProxyList(it)
    }
        .shareInBackground()

    fun backClicked() {
        stakingRouter.back()
    }

    fun addProxyClicked() {
        stakingRouter.openAddStakingProxy()
    }

    fun proxyClicked(item: StakingProxyRvItem) {
        launch {
            val chain = selectedAssetState.chain()
            externalActions.showAddressActions(item.accountAddress, chain)
        }
    }

    fun rewokeAccess(externalActionPayload: ExternalActions.Payload) {
        val payload = ConfirmRemoveStakingProxyPayload(externalActionPayload.requireAddress())
        stakingRouter.openConfirmRemoveStakingProxy(payload)
    }

    private suspend fun mapToProxyList(proxies: List<StakingProxyAccount>): List<Any> {
        val chain = selectedAssetState.chain()
        return buildList {
            val groupTitle = resourceManager.getString(R.string.staking_proxies_group_title)
            add(StakingProxyGroupRvItem(groupTitle))

            val proxyRvItems = proxies.map { stakingProxyAccount ->
                val accountAddress = chain.addressOf(stakingProxyAccount.proxyAccountId)
                StakingProxyRvItem(
                    addressIconGenerator.createAddressIcon(stakingProxyAccount.proxyAccountId, SIZE_BIG),
                    chain.icon,
                    stakingProxyAccount.metaAccount?.name ?: accountAddress,
                    accountAddress
                )
            }

            addAll(proxyRvItems)
        }
    }

    private fun ExternalActions.Payload.requireAddress() = type.castOrNull<ExternalActions.Type.Address>()!!.address!!
}
