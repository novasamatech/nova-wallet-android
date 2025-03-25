package io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.model.ChainAccountPreview
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseChainAccountsPreviewViewModel(
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val router: ReturnableRouter,
) : BaseViewModel(), ExternalActions.Presentation by externalActions {

    open val subtitle: String? = null

    abstract val chainAccountProjections: Flow<List<AccountInChainUi>>

    abstract val buttonState: Flow<DescriptiveButtonState>

    abstract fun continueClicked()

    fun backClicked() {
        router.back()
    }

    fun chainAccountClicked(item: AccountInChainUi) = launch {
        val chain = chainRegistry.getChain(item.chainUi.id)

        externalActions.showAddressActions(item.address, chain)
    }

    protected fun Flow<List<ChainAccountPreview>>.defaultFormat(): Flow<List<AccountInChainUi>> {
        return mapList(::mapParitySignerAccountInChainToUi)
    }

    private suspend fun mapParitySignerAccountInChainToUi(account: ChainAccountPreview): AccountInChainUi = with(account) {
        val address = chain.addressOf(accountId)

        val icon = iconGenerator.createAccountAddressModel(chain, accountId).image

        AccountInChainUi(
            chainUi = mapChainToUi(chain),
            addressOrHint = address,
            address = address,
            accountIcon = icon,
            actionsAvailable = true
        )
    }
}
