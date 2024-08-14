package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress

import android.util.Log
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
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
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.items.AccountUi
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccountWithBalance
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.reviewAddress
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class SelectAddressLedgerViewModel(
    private val router: LedgerRouter,
    internal val interactor: SelectAddressLedgerInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val payload: SelectLedgerAddressPayload,
    private val chainRegistry: ChainRegistry,
    private val messageFormatter: LedgerMessageFormatter
) : BaseViewModel(),
    LedgerMessageCommands,
    Browserable.Presentation by Browserable() {

    override val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>> = MutableLiveData()

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

    val device = flowOf {
        interactor.getDevice(payload.deviceId)
    }

    init {
        loadNewAccount()
    }

    abstract fun onAccountVerified(account: LedgerAccountWithBalance)

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
        verifyAddressJob?.cancel()
        verifyAddressJob = launch {
            val account = loadedAccounts.value.first { it.index == id.toInt() }

            ledgerMessageCommands.value = LedgerMessageCommand.reviewAddress(
                resourceManager = resourceManager,
                address = account.account.address,
                deviceName = device.first().name,
                onCancel = ::verifyAddressCancelled,
            ).event()

            val result = withContext(Dispatchers.Default) {
                interactor.verifyLedgerAccount(chain(), payload.deviceId, account.index)
            }

            result.onFailure {
                handleLedgerError(it) { verifyAccount(id) }
            }.onSuccess {
                ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

                onAccountVerified(account)
            }
        }
    }

    private fun handleLedgerError(error: Throwable, retry: () -> Unit) {
        handleLedgerError(error, messageFormatter, resourceManager, retry)
    }

    private fun verifyAddressCancelled() {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
        verifyAddressJob?.cancel()
        verifyAddressJob = null
    }

    private fun loadNewAccount() {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

        launch(Dispatchers.Default) {
            loadingAccount.withFlagSet {
                val nextAccountIndex = loadedAccounts.value.size

                interactor.loadLedgerAccount(chain(), payload.deviceId, nextAccountIndex)
                    .onSuccess {
                        loadedAccounts.value = loadedAccounts.value.added(it)
                    }.onFailure {
                        Log.d("Ledger", "Error", it)
                        handleLedgerError(it) { loadNewAccount() }
                    }
            }
        }
    }

    private suspend fun mapLedgerAccountWithBalanceToUi(account: LedgerAccountWithBalance): AccountUi {
        return with(account) {
            val tokenBalance = balance.formatPlanks(account.chainAsset)
            val addressModel = addressIconGenerator.createAccountAddressModel(chain(), account.account.address)

            AccountUi(
                id = index.toLong(),
                title = addressModel.address,
                subtitle = tokenBalance,
                isSelected = false,
                isClickable = true,
                picture = addressModel.image,
                chainIcon = null,
                updateIndicator = false,
                subtitleIconRes = null,
                isEditable = false
            )
        }
    }
}
