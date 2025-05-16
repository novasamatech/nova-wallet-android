package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.address
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.LedgerAccountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SelectAddressLedgerViewModel(
    private val router: LedgerRouter,
    protected val interactor: SelectAddressLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val payload: SelectLedgerAddressPayload,
    private val chainRegistry: ChainRegistry,
    private val messageCommandFormatter: MessageCommandFormatter
) : BaseViewModel(),
    LedgerMessageCommands,
    Browserable.Presentation by Browserable() {

    abstract val ledgerVariant: LedgerVariant

    override val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>> = MutableLiveData()

    protected open val needToVerifyAccount = true

    private val substratePreviewChain by lazyAsync { chainRegistry.getChain(payload.substrateChainId) }

    private val loadingAccount = MutableStateFlow(false)
    protected val loadedAccounts: MutableStateFlow<List<LedgerAccount>> = MutableStateFlow(emptyList())

    private var verifyAddressJob: Job? = null

    val loadedAccountModels = loadedAccounts.mapList { it.toUi() }
        .shareInBackground()

    val chainUi = flowOf { mapChainToUi(substratePreviewChain()) }
        .shareInBackground()

    val loadMoreState = loadingAccount.map { loading ->
        if (loading) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.ledger_import_select_address_load_more))
        }
    }.shareInBackground()

    protected val _alertFlow = MutableStateFlow<AlertModel?>(null)
    val alertFlow: Flow<AlertModel?> = _alertFlow

    val device = flowOf {
        interactor.getDevice(payload.deviceId)
    }

    init {
        loadNewAccount()
    }

    abstract fun onAccountVerified(account: LedgerAccount)

    fun loadMoreClicked() {
        if (loadingAccount.value) return

        loadNewAccount()
    }

    fun backClicked() {
        router.back()
    }

    fun accountClicked(accountUi: LedgerAccountModel) {
        verifyAccount(accountUi.id)
    }

    private fun verifyAccount(id: Int) {
        verifyAddressJob?.cancel()
        verifyAddressJob = launch {
            val account = loadedAccounts.value.first { it.index == id }

            if (needToVerifyAccount) {
                verifyAccountInternal(account)
            } else {
                onAccountVerified(account)
            }
        }
    }

    private suspend fun verifyAccountInternal(account: LedgerAccount) {
        val device = device.first()

        ledgerMessageCommands.value = messageCommandFormatter.reviewAddressCommand(
            substrateAddress = account.substrate.address,
            evmAddress = account.evm?.address(),
            device = device,
            onCancel = ::verifyAddressCancelled,
        ).event()

        val result = withContext(Dispatchers.Default) {
            interactor.verifyLedgerAccount(substratePreviewChain(), payload.deviceId, account.index, ledgerVariant)
        }

        result.onFailure {
            handleLedgerError(it, device) { verifyAccount(account.index) }
        }.onSuccess {
            ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

            onAccountVerified(account)
        }
    }

    private fun handleLedgerError(error: Throwable, device: LedgerDevice, retry: () -> Unit) {
        handleLedgerError(
            reason = error,
            device = device,
            commandFormatter = messageCommandFormatter,
            onRetry = retry
        )
    }

    private fun verifyAddressCancelled() {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()
        verifyAddressJob?.cancel()
        verifyAddressJob = null
    }

    private fun loadNewAccount() {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

        launch(Dispatchers.Default) {
            loadingAccount.withFlagSet {
                val nextAccountIndex = loadedAccounts.value.size

                interactor.loadLedgerAccount(substratePreviewChain(), payload.deviceId, nextAccountIndex, ledgerVariant)
                    .onSuccess {
                        loadedAccounts.value = loadedAccounts.value.added(it)
                    }.onFailure {
                        Log.e("Ledger", "Failed to load Ledger account", it)
                        handleLedgerError(it, device.first()) { loadNewAccount() }
                    }
            }
        }
    }

    private suspend fun LedgerAccount.toUi(): LedgerAccountModel {
        return LedgerAccountModel(
            id = index,
            label = resourceManager.getString(R.string.ledger_select_address_account_label, (index + 1).format()),
            substrate = addressIconGenerator.createAccountAddressModel(substratePreviewChain(), substrate.address),
            evm = evm?.let { addressIconGenerator.createAccountAddressModel(AddressFormat.evm(), it.accountId) }
        )
    }
}
