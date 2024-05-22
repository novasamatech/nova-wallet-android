package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.BaseChainAccountsPreviewViewModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview.PreviewImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.reviewAddress
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewImportGenericLedgerViewModel(
    private val interactor: PreviewImportGenericLedgerInteractor,
    private val router: LedgerRouter,
    private val iconGenerator: AddressIconGenerator,
    private val payload: PreviewImportGenericLedgerPayload,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
    private val messageFormatter: LedgerMessageFormatter,
) : BaseChainAccountsPreviewViewModel(
    iconGenerator = iconGenerator,
    externalActions = externalActions,
    chainRegistry = chainRegistry,
    router = router
),
    LedgerMessageCommands {

    override val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>> = MutableLiveData()

    override val chainAccountProjections = flowOf { interactor.availableChainAccounts(payload.account.address) }
        .defaultFormat()
        .shareInBackground()

    override val buttonState: Flow<DescriptiveButtonState> = flowOf {
        DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
    }

    val device = flowOf {
        interactor.getDevice(payload.deviceId)
    }

    private var verifyAddressJob: Job? = null

    override fun continueClicked() {
        verifyAddressJob?.cancel()
        verifyAddressJob = launch {
            verifyAccount()
        }
    }

    private suspend fun verifyAccount() {
        ledgerMessageCommands.value = LedgerMessageCommand.reviewAddress(
            resourceManager = resourceManager,
            address = payload.account.address,
            deviceName = device.first().name,
            onCancel = ::verifyAddressCancelled,
        ).event()

        val result = withContext(Dispatchers.Default) {
            interactor.verifyAddressOnLedger(payload.deviceId)
        }

        result.onFailure {
            handleLedgerError(
                reason = it,
                messageFormatter = messageFormatter,
                resourceManager = resourceManager,
                retry = ::continueClicked
            )
        }.onSuccess {
            ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

            onAccountVerified()
        }
    }

    private fun onAccountVerified() {
        showMessage("Open finish connect")
    }

    private fun verifyAddressCancelled() {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
        verifyAddressJob?.cancel()
        verifyAddressJob = null
    }
}
