package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.fillWallet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inserted
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.fillWallet.FillWalletImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.fillWallet.model.FillableChainAccountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.random.Random

class FillWalletImportLedgerViewModel(
    private val router: LedgerRouter,
    private val interactor: FillWalletImportLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val filledAccountsFlow = MutableStateFlow<Map<ChainId, LedgerSubstrateAccount>>(emptyMap())

    private val availableChainsFlow = flowOf { interactor.availableLedgerChains() }
        .shareInBackground()

    val fillableChainAccountModels = combine(filledAccountsFlow, availableChainsFlow) { filledAccounts, availableChains ->
        availableChains.map { chain -> createFillableChainAccountModel(chain, filledAccounts[chain.id]) }
    }.shareInBackground()

    val continueState = filledAccountsFlow.map {
        if (it.isEmpty()) {
            DescriptiveButtonState.Disabled(resourceManager.getString(R.string.account_ledger_import_fill_disabled_hint))
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_done))
        }
    }.shareInBackground()

    fun continueClicked() {
       showMessage("TODO")
    }

    fun itemClicked(item: FillableChainAccountModel) = launch(Dispatchers.Default) {
        // TODO receive account from ledger
        val account = randomAccount(item)

        addAccount(item.chainUi.id, account)
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun randomAccount(item: FillableChainAccountModel): LedgerSubstrateAccount {
        val chain = availableChainsFlow.first().first { it.id ==  item.chainUi.id }

        val publicKey = Random.nextBytes(32)
        val address = chain.addressOf(publicKey)

        return LedgerSubstrateAccount(
            address = address,
            publicKey = publicKey,
        )
    }

    private suspend fun createFillableChainAccountModel(chain: Chain, account: LedgerSubstrateAccount?) : FillableChainAccountModel {
        return FillableChainAccountModel(
            filledAddressModel = account?.let {
                addressIconGenerator.createAccountAddressModel(chain, it.address)
            },
            chainUi = mapChainToUi(chain)
        )
    }

    private fun addAccount(chainId: ChainId, account: LedgerSubstrateAccount){
        filledAccountsFlow.value = filledAccountsFlow.value.inserted(chainId, account)
    }
}
