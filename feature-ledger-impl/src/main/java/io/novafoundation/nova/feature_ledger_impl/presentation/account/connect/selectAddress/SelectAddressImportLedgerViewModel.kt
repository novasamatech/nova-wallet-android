package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.selectAddress.LedgerAccountWithBalance
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.selectAddress.SelectAddressImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SelectAddressImportLedgerViewModel(
    private val router: LedgerRouter,
    private val interactor: SelectAddressImportLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val payload: SelectLedgerAddressPayload,
    private val chainRegistry: ChainRegistry,
) : BaseViewModel() {

    private val chain by lazyAsync { chainRegistry.getChain(payload.chainId) }

    private val loadingAccount = MutableStateFlow(false)
    private val loadedAccounts: MutableStateFlow<List<LedgerAccountWithBalance>> = MutableStateFlow(emptyList())

    val loadedAccountModels = loadedAccounts.mapList(::mapLedgerAccountWithBalanceToUi)
        .shareInBackground()

    val chainUi = flowOf { mapChainToUi(chain()) }
        .shareInBackground()

    val loadMoreState = loadingAccount.map { loading ->
        if (loading) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.ledger_import_select_address_load_more))
        }
    }.shareInBackground()

    init {
        loadNewAccount()
    }

    fun loadMoreClicked() {
        if (loadingAccount.value) return

        loadNewAccount()
    }

    fun backClicked() {
        // TODO notify previous screen that flow is cancelled

        router.back()
    }

    fun accountClicked(accountUi: AccountUi) = launch {
        val account = loadedAccounts.value.first { it.index == accountUi.id.toInt() }

        showMessage("TODO - confirm ${account.account.address} address")
    }

    private fun loadNewAccount() = launch(Dispatchers.Default) {
        loadingAccount.withFlagSet {
            val nextAccountIndex = loadedAccounts.value.size

            interactor.loadLedgerAccount(chain(), payload.deviceId, nextAccountIndex)
                .onSuccess {
                    loadedAccounts.value = loadedAccounts.value.added(it)
                }.onFailure {
                    // TODO error handling
                    showError(it)
                }
        }
    }

    private suspend fun mapLedgerAccountWithBalanceToUi(account: LedgerAccountWithBalance): AccountUi {
        return with(account) {
            val amountModel = mapAmountToAmountModel(balance, token)
            val addressModel = addressIconGenerator.createAccountAddressModel(chain(), account.account.address)

            AccountUi(
                id = index.toLong(),
                title = addressModel.address,
                subtitle = amountModel.token,
                isSelected = false,
                isClickable = true,
                picture = addressModel.image,
                subtitleIconRes = null
            )
        }
    }
}
