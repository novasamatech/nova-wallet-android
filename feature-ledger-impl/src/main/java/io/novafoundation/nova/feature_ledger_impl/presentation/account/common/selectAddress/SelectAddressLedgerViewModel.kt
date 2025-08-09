package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.GENERIC_ADDRESS_PREFIX
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.showAddressActions
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.address
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.createLedgerReviewAddresses
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.AddressVerificationMode
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.LedgerAccountRvItem
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private val messageCommandFormatter: MessageCommandFormatter,
    private val addressActionsMixinFactory: AddressActionsMixin.Factory
) : BaseViewModel(),
    LedgerMessageCommands,
    Browserable.Presentation by Browserable() {

    abstract val ledgerVariant: LedgerVariant

    abstract val addressVerificationMode: AddressVerificationMode

    override val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>> = MutableLiveData()

    private val substratePreviewChain by lazyAsync { chainRegistry.getChain(payload.substrateChainId) }

    private val loadingState = MutableStateFlow(AccountLoadingState.CAN_LOAD)
    protected val loadedAccounts: MutableStateFlow<List<LedgerAccount>> = MutableStateFlow(emptyList())

    private var verifyAddressJob: Job? = null

    val loadedAccountModels = loadedAccounts.mapList { it.toUi() }
        .shareInBackground()

    val chainUi = flowOf { mapChainToUi(substratePreviewChain()) }
        .shareInBackground()

    val loadMoreState = loadingState.map { loadingState ->
        when (loadingState) {
            AccountLoadingState.CAN_LOAD -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.ledger_import_select_address_load_more))

            AccountLoadingState.LOADING -> DescriptiveButtonState.Loading
            AccountLoadingState.NOTHING_TO_LOAD -> DescriptiveButtonState.Gone
        }
    }.shareInBackground()

    protected val _alertFlow = MutableStateFlow<AlertModel?>(null)
    val alertFlow: Flow<AlertModel?> = _alertFlow

    val device = flowOf {
        interactor.getDevice(payload.deviceId)
    }

    val addressActionsMixin = addressActionsMixinFactory.create(this)

    init {
        loadNewAccount()
    }

    abstract fun onAccountVerified(account: LedgerAccount)

    /**
     * Loads ledger account. Can return Success(null) to indicate there is nothing to load and "load more" button should be hidden
     */
    protected open suspend fun loadLedgerAccount(
        substratePreviewChain: Chain,
        deviceId: String,
        accountIndex: Int,
        ledgerVariant: LedgerVariant
    ): Result<LedgerAccount?> {
        return interactor.loadLedgerAccount(substratePreviewChain, deviceId, accountIndex, ledgerVariant)
    }

    fun loadMoreClicked() {
        if (loadingState.value != AccountLoadingState.CAN_LOAD) return

        loadNewAccount()
    }

    fun backClicked() {
        router.back()
    }

    fun accountClicked(accountUi: LedgerAccountRvItem) {
        verifyAccount(accountUi.id)
    }

    fun addressInfoClicked(addressModel: AddressModel, addressScheme: AddressScheme) {
        addressActionsMixin.showAddressActions(addressModel.address, AddressFormat.defaultForScheme(addressScheme, SS58Encoder.GENERIC_ADDRESS_PREFIX))
    }

    private fun verifyAccount(id: Int) {
        verifyAddressJob?.cancel()
        verifyAddressJob = launch {
            val account = loadedAccounts.value.first { it.index == id }
            val verificationMode = addressVerificationMode

            if (verificationMode is AddressVerificationMode.Enabled) {
                verifyAccountInternal(account, verificationMode.addressSchemesToVerify)
            } else {
                onAccountVerified(account)
            }
        }
    }

    private suspend fun verifyAccountInternal(account: LedgerAccount, reviewAddressSchemes: List<AddressScheme>) {
        val device = device.first()

        ledgerMessageCommands.value = messageCommandFormatter.reviewAddressCommand(
            addresses = createLedgerReviewAddresses(
                allowedAddressSchemes = reviewAddressSchemes,
                AddressScheme.SUBSTRATE to account.substrate.address,
                AddressScheme.EVM to account.evm?.address()
            ),
            device = device,
            onCancel = ::verifyAddressCancelled,
        ).event()

        val result = withContext(Dispatchers.Default) {
            interactor.verifyLedgerAccount(substratePreviewChain(), payload.deviceId, account.index, ledgerVariant, reviewAddressSchemes)
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

    private fun loadNewAccount(): Unit = launchUnit(Dispatchers.Default) {
        ledgerMessageCommands.postValue(messageCommandFormatter.hideCommand().event())

        loadingState.value = AccountLoadingState.LOADING
        val nextAccountIndex = loadedAccounts.value.size

        loadLedgerAccount(substratePreviewChain(), payload.deviceId, nextAccountIndex, ledgerVariant)
            .onSuccess { newAccount ->
                if (newAccount != null) {
                    loadedAccounts.value = loadedAccounts.value.added(newAccount)
                    loadingState.value = AccountLoadingState.CAN_LOAD
                } else {
                    loadingState.value = AccountLoadingState.NOTHING_TO_LOAD
                }
            }.onFailure {
                Log.e("Ledger", "Failed to load Ledger account", it)
                handleLedgerError(it, device.first()) { loadNewAccount() }
                loadingState.value = AccountLoadingState.CAN_LOAD
            }
    }

    private suspend fun LedgerAccount.toUi(): LedgerAccountRvItem {
        return LedgerAccountRvItem(
            id = index,
            label = resourceManager.getString(R.string.ledger_select_address_account_label, (index + 1).format()),
            substrate = addressIconGenerator.createAccountAddressModel(substratePreviewChain(), substrate.address),
            evm = evm?.let { addressIconGenerator.createAccountAddressModel(AddressFormat.evm(), it.accountId) }
        )
    }

    enum class AccountLoadingState {
        CAN_LOAD, LOADING, NOTHING_TO_LOAD
    }
}
