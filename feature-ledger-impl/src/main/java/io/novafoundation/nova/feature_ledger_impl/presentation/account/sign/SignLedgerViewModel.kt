package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.sign.cancelled
import io.novafoundation.nova.feature_account_api.presenatation.sign.signed
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse.INVALID_DATA
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.SignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.ended
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class InvalidDataError {
    TX_NOT_SUPPORTED,
    METADATA_OUTDATED,
}

private class InvalidSignatureError : Exception()

class SignLedgerViewModel(
    private val router: LedgerRouter,
    private val resourceManager: ResourceManager,
    private val signPayloadState: SigningSharedState,
    private val extrinsicValidityUseCase: ExtrinsicValidityUseCase,
    private val responder: SignInterScreenResponder,
    private val interactor: SignLedgerInteractor,
    private val messageFormatter: LedgerMessageFormatter,
    private val messageCommandFormatter: MessageCommandFormatter,
    private val payload: SignLedgerPayload,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    ledgerDeviceFormatter: LedgerDeviceFormatter,
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    messageFormatter = messageFormatter,
    ledgerDeviceFormatter = ledgerDeviceFormatter,
    messageCommandFormatter = messageCommandFormatter,
    payload = payload
) {

    private val validityPeriod = flowOf {
        extrinsicValidityUseCase.extrinsicValidityPeriod(signPayloadState.getOrThrow().extrinsic)
    }.shareInBackground()

    private var signingJob: Deferred<SignatureWrapper>? = null

    private val fatalErrorDetected = MutableStateFlow(false)

    override fun backClicked() {
        exit()
    }

    override suspend fun handleLedgerError(reason: Throwable, device: LedgerDevice) {
        if (fatalErrorDetected.value) return

        when {
            reason is SubstrateLedgerApplicationError.Response && reason.response == INVALID_DATA -> {
                handleInvalidData(reason.errorMessage, device)
            }

            reason is InvalidSignatureError -> handleInvalidSignature(device)

            else -> super.handleLedgerError(reason, device)
        }
    }

    override suspend fun verifyConnection(device: LedgerDevice) {
        val validityPeriod = validityPeriod.first()

        if (validityPeriod.ended()) {
            timerExpired(device)
            return
        }

        val signState = signPayloadState.getOrThrow()

        ledgerMessageCommands.value = messageCommandFormatter.signCommand(
            validityPeriod,
            device,
            onTimeFinished = { timerExpired(device) },
            ::bottomSheetClosed
        ).event()

        val signingMetaAccount = signState.metaAccount

        signingJob?.cancel()
        signingJob = async {
            interactor.getSignature(
                device = device,
                metaId = signingMetaAccount.id,
                payload = signState.extrinsic
            )
        }

        val signature = signingJob!!.await()

        if (interactor.verifySignature(signState, signature)) {
            responder.respond(payload.request.signed(signature))
            hideBottomSheet()
            router.finishSignFlow()
        } else {
            throw InvalidSignatureError()
        }
    }

    private suspend fun handleInvalidSignature(ledgerDevice: LedgerDevice) {
        showFatalError(
            title = resourceManager.getString(R.string.common_signature_invalid),
            subtitle = resourceManager.getString(R.string.ledger_sign_signature_invalid_message),
            ledgerDevice = ledgerDevice
        )
    }

    private suspend fun handleInvalidData(invalidDataMessage: String?, ledgerDevice: LedgerDevice) {
        val errorTitle: String
        val errorMessage: String

        when (matchInvalidDataMessage(invalidDataMessage)) {
            InvalidDataError.TX_NOT_SUPPORTED -> {
                errorTitle = resourceManager.getString(R.string.ledger_sign_tx_not_supported_title)
                errorMessage = resourceManager.getString(R.string.ledger_sign_tx_not_supported_subtitle)
            }

            InvalidDataError.METADATA_OUTDATED -> {
                errorTitle = resourceManager.getString(R.string.ledger_sign_metadata_outdated_title)
                errorMessage = resourceManager.getString(R.string.ledger_sign_metadata_outdated_subtitle, messageFormatter.appName())
            }

            null -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
                errorMessage = invalidDataMessage ?: resourceManager.getString(R.string.ledger_error_general_message)
            }
        }

        showFatalError(errorTitle, errorMessage, ledgerDevice)
    }

    private fun timerExpired(ledgerDevice: LedgerDevice) {
        signingJob?.cancel()
        launch {
            val period = validityPeriod.first().period.millis.milliseconds
            val periodFormatted = resourceManager.formatDuration(period, estimated = false)

            showFatalError(
                ledgerDevice = ledgerDevice,
                title = resourceManager.getString(R.string.ledger_sign_transaction_expired_title),
                subtitle = resourceManager.getString(R.string.ledger_sign_transaction_expired_message, periodFormatted),
            )
        }
    }

    private suspend fun showFatalError(
        title: String,
        subtitle: String,
        ledgerDevice: LedgerDevice
    ) {
        fatalErrorDetected.value = true

        ledgerMessageCommands.value = messageCommandFormatter.fatalErrorCommand(title, subtitle, ledgerDevice, ::bottomSheetClosed, ::errorAcknowledged).event()
    }

    private fun bottomSheetClosed() {
        signingJob?.cancel()
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()
    }

    private fun errorAcknowledged() {
        hideBottomSheet()

        exit()
    }

    private fun exit() {
        responder.respond(payload.request.cancelled())
        router.finishSignFlow()
    }

    private fun hideBottomSheet() {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()
    }

    private fun matchInvalidDataMessage(message: String?): InvalidDataError? {
        return when (message) {
            "Method not supported", "Call nesting not supported" -> InvalidDataError.TX_NOT_SUPPORTED
            "Spec version not supported", "Txn version not supported" -> InvalidDataError.METADATA_OUTDATED
            else -> null
        }
    }
}
