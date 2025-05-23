package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.ChainAccountGroupUi
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.BaseChainAccountsPreviewViewModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.preview.PreviewImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.createLedgerReviewAddresses
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.extensions.asEthereumAccountId
import io.novasama.substrate_sdk_android.extensions.toAddress
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
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
    private val messageCommandFormatter: MessageCommandFormatter,
    private val addressSchemeFormatter: AddressSchemeFormatter,
) : BaseChainAccountsPreviewViewModel(
    iconGenerator = iconGenerator,
    externalActions = externalActions,
    chainRegistry = chainRegistry,
    router = router
),
    LedgerMessageCommands {

    override val ledgerMessageCommands: MutableLiveData<Event<LedgerMessageCommand>> = MutableLiveData()

    override val chainAccountProjections = flowOf {
        interactor.availableChainAccounts(
            substrateAccountId = payload.substrateAccount.address.toAccountId(),
            evmAccountId = payload.evmAccount?.accountId
        ).toListWithHeaders(
            keyMapper = { scheme, _ -> formatGroupHeader(scheme) },
            valueMapper = { account -> mapChainAccountPreviewToUi(account) }
        )
    }
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

    private fun formatGroupHeader(addressScheme: AddressScheme): ChainAccountGroupUi {
        return ChainAccountGroupUi(
            id = addressScheme.name,
            title = addressSchemeFormatter.accountsLabel(addressScheme),
            action = null
        )
    }

    private suspend fun verifyAccount() {
        val device = device.first()

        ledgerMessageCommands.value = messageCommandFormatter.reviewAddressCommand(
            addresses = createLedgerReviewAddresses(
                allowedAddressSchemes = AddressScheme.entries,
                AddressScheme.SUBSTRATE to payload.substrateAccount.address,
                AddressScheme.EVM to payload.evmAccount?.accountId?.asEthereumAccountId()?.toAddress()?.value
            ),
            device = device,
            onCancel = ::verifyAddressCancelled,
        ).event()
        val result = withContext(Dispatchers.Default) {
            interactor.verifyAddressOnLedger(payload.accountIndex, payload.deviceId)
        }

        result.onFailure {
            handleLedgerError(
                reason = it,
                device = device,
                commandFormatter = messageCommandFormatter,
                onRetry = ::continueClicked
            )
        }.onSuccess {
            ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

            onAccountVerified()
        }
    }

    private fun onAccountVerified() {
        val nextPayload = FinishImportGenericLedgerPayload(payload.substrateAccount, payload.evmAccount)
        router.openFinishImportLedgerGeneric(nextPayload)
    }

    private fun verifyAddressCancelled() {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()
        verifyAddressJob?.cancel()
        verifyAddressJob = null
    }
}
