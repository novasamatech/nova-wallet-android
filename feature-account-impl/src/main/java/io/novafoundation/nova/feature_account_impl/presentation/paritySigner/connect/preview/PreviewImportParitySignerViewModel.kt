package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview.ParitySignerAccountInChain
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview.PreviewImportParitySignerInteractor
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.chainAccounts.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class PreviewImportParitySignerViewModel(
    private val interactor: PreviewImportParitySignerInteractor,
    private val accountRouter: AccountRouter,
    private val iconGenerator: AddressIconGenerator,
    private val payload: ParitySignerAccountPayload,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
) : BaseViewModel(), ExternalActions by externalActions {

    val title = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_import_preview_description, payload.variant)

    val chainAccountProjections = flowOf { interactor.deriveSubstrateChainAccounts(payload.accountId) }
        .mapList(::mapParitySignerAccountInChainToUi)
        .shareInBackground()

    fun backClicked() {
        accountRouter.back()
    }

    fun chainAccountClicked(item: AccountInChainUi) = launch {
        val chain = chainRegistry.getChain(item.chainUi.id)

        val type = ExternalActions.Type.Address(item.address)

        externalActions.showExternalActions(type, chain)
    }

    private suspend fun mapParitySignerAccountInChainToUi(account: ParitySignerAccountInChain): AccountInChainUi = with(account) {
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

    fun continueClicked() {
        accountRouter.openFinishImportParitySigner(payload)
    }
}
