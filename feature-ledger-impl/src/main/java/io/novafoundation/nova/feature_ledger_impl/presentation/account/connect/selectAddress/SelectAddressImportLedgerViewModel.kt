package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.event
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
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.SelectLedgerAddressInterScreenResponder
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class VerifyCommand {

    object Hide : VerifyCommand()

    class Show(val onCancel: () -> Unit) : VerifyCommand()
}

class SelectAddressImportLedgerViewModel(
    private val router: LedgerRouter,
    private val interactor: SelectAddressImportLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val payload: SelectLedgerAddressPayload,
    private val chainRegistry: ChainRegistry,
    private val responder: SelectLedgerAddressInterScreenResponder,
) : BaseViewModel(), Retriable {

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    private val _verifyAddressCommandEvent = MutableLiveData<Event<VerifyCommand>>()
    val verifyAddressCommandEvent: LiveData<Event<VerifyCommand>> = _verifyAddressCommandEvent

    private val chain by lazyAsync { chainRegistry.getChain(payload.chainId) }

    private val loadingAccount = MutableStateFlow(false)
    private val loadedAccounts: MutableStateFlow<List<LedgerAccountWithBalance>> = MutableStateFlow(emptyList())

    private var verifyAddressJob: Job? = null

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
        router.back()
    }

    fun accountClicked(accountUi: AccountUi) {
        verifyAccount(accountUi.id)
    }

    private fun verifyAccount(id: Long) {
        _verifyAddressCommandEvent.value = VerifyCommand.Show(::verifyAddressCancelled).event()

        verifyAddressJob?.cancel()
        verifyAddressJob = launch {
            val account = loadedAccounts.value.first { it.index == id.toInt() }

            val result = withContext(Dispatchers.Default) {
                interactor.verifyLedgerAccount(chain(), payload.deviceId, account.index)
            }

            _verifyAddressCommandEvent.value = VerifyCommand.Hide.event()

            result.onFailure {
                handleLedgerError(it) { verifyAccount(id) }
            }.onSuccess {
                responder.respond(screenResponseFrom(account))
                router.returnToImportFillWallet()
            }
        }
    }

    private suspend fun handleLedgerError(error: Throwable, retry: () -> Unit) {
        handleLedgerError(error, chain, resourceManager, retry)
    }

    private fun verifyAddressCancelled() {
        verifyAddressJob?.cancel()
        verifyAddressJob = null
    }

    private fun screenResponseFrom(account: LedgerAccountWithBalance): SelectLedgerAddressInterScreenCommunicator.Response {
        return SelectLedgerAddressInterScreenCommunicator.Response(
            publicKey = account.account.publicKey,
            address = account.account.address,
            chainId = payload.chainId
        )
    }

    private fun loadNewAccount() {
        launch(Dispatchers.Default) {
            loadingAccount.withFlagSet {
                val nextAccountIndex = loadedAccounts.value.size

                interactor.loadLedgerAccount(chain(), payload.deviceId, nextAccountIndex)
                    .onSuccess {
                        loadedAccounts.value = loadedAccounts.value.added(it)
                    }.onFailure {
                        handleLedgerError(it) { loadNewAccount() }
                    }
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
