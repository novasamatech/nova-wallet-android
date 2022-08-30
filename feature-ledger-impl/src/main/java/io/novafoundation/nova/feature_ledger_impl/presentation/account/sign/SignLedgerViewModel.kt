package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.sign.signed
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

//sealed class BottomSheetCommand {
//
//    class Sign(val validityPeriod: ValidityPeriod) : BottomSheetCommand()
//
//    object Hide : BottomSheetCommand()
//
//    class PresentError(val title: String, val message: String) : BottomSheetCommand()
//}

class SignLedgerViewModel(
    private val substrateApplication: SubstrateLedgerApplication,
    private val selectLedgerPayload: SelectLedgerPayload,
    private val router: LedgerRouter,
    private val resourceManager: ResourceManager,
    private val signPayloadState: SharedState<SignerPayloadExtrinsic>,
    private val extrinsicValidityUseCase: ExtrinsicValidityUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val request: SignInterScreenCommunicator.Request,
    private val responder: SignInterScreenResponder,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    chainRegistry: ChainRegistry,
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    router = router,
    resourceManager = resourceManager,
    chainRegistry = chainRegistry,
    payload = selectLedgerPayload
) {

//    private val validityPeriod = flowOf {
//        extrinsicValidityUseCase.extrinsicValidityPeriod(signPayloadState.getOrThrow())
//    }.shareInBackground()
//
//    private val _bottomSheetCommand = MutableLiveData<Event<BottomSheetCommand>>()
//    val bottomSheetCommand: LiveData<Event<BottomSheetCommand>> = _bottomSheetCommand

    private var signingJob: Deferred<SignatureWrapper>? = null

//    fun timerExpired() {
//        _bottomSheetCommand.value = BottomSheetCommand.PresentError(
//            title = resourceManager.getString(R.string.ledger_sign_transaction_expired_title),
//            message = resourceManager.getString(R.string.ledger_sign_transaction_expired_message)
//        ).event()
//    }
//
//    fun bottomSheetClosed() {
//        signingJob?.cancel()
//    }
//
//    fun errorAcknowledged() {
//
//        router.back()
//    }
//
//    private fun hideBottomSheet() {
//        _bottomSheetCommand.value = BottomSheetCommand.Hide.event()
//    }

    override suspend fun verifyConnection(device: LedgerDevice) {
//        _bottomSheetCommand.value = BottomSheetCommand.Sign(validityPeriod = validityPeriod.first()).event()

        val selectedMetaAccount = selectedAccountUseCase.getSelectedMetaAccount()

        signingJob?.cancel()
        signingJob = async {
            substrateApplication.getSignature(
                device = device,
                metaId = selectedMetaAccount.id,
                chainId = selectLedgerPayload.chainId,
                payload = signPayloadState.getOrThrow()
            )
        }

        val signature = signingJob!!.await()
        val response = request.signed(signature)

        responder.respond(response)

        router.back()
//        hideBottomSheet()
    }
}
