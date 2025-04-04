package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inserted
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.legacy.fillWallet.FillWalletImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.LedgerChainAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenRequester
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.model.FillableChainAccountModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish.FinishImportLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FillWalletImportLedgerViewModel(
    private val router: LedgerRouter,
    private val interactor: FillWalletImportLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val actionAwaitableMixin: ActionAwaitableMixin.Factory,
    private val selectLedgerAddressRequester: SelectLedgerAddressInterScreenRequester,
    private val payload: FillWalletImportLedgerLegacyPayload
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

    val confirmExit = actionAwaitableMixin.confirmingOrDenyingAction<Unit>()

    init {
        selectLedgerAddressRequester.responseFlow
            .onEach(::addAccount)
            .launchIn(this)
    }

    fun continueClicked() = launch {
        val payload = buildFinishPayload()

        router.openFinishImportLedger(payload)
    }

    private suspend fun buildFinishPayload(): FinishImportLedgerPayload {
        val filledAccounts = filledAccountsFlow.first()
        val parcelableAccounts = filledAccounts.map { (chainId, account) ->
            LedgerChainAccount(
                publicKey = account.publicKey,
                encryptionType = account.encryptionType,
                address = account.address,
                derivationPath = account.derivationPath,
                chainId = chainId
            )
        }

        return FinishImportLedgerPayload(parcelableAccounts)
    }

    fun itemClicked(item: FillableChainAccountModel) {
        val payload = SelectLedgerLegacyPayload(item.chainUi.id, payload.connectionMode)

        selectLedgerAddressRequester.openRequest(payload)
    }

    fun backClicked() = launch {
        val filledAccounts = filledAccountsFlow.first()

        if (filledAccounts.isEmpty() || confirmExit.awaitAction()) {
            router.back()
        }
    }

    private suspend fun createFillableChainAccountModel(chain: Chain, account: LedgerSubstrateAccount?): FillableChainAccountModel {
        return FillableChainAccountModel(
            filledAddressModel = account?.let {
                addressIconGenerator.createAccountAddressModel(chain, it.address)
            },
            chainUi = mapChainToUi(chain)
        )
    }

    private fun addAccount(response: LedgerChainAccount) {
        val account = LedgerSubstrateAccount(response.address, response.publicKey, response.encryptionType, response.derivationPath)

        filledAccountsFlow.value = filledAccountsFlow.value.inserted(response.chainId, account)
    }
}
