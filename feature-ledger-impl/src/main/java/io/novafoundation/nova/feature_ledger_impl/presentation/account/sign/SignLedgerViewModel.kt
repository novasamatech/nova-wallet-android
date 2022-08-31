package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.sign.cancelled
import io.novafoundation.nova.feature_account_api.presenatation.sign.signed
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.domain.account.sign.SignLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand.Graphics
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.closeToExpire
import io.novafoundation.nova.runtime.extrinsic.remainingTime
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
    private val substrateApplication: SubstrateLedgerApplication,
    private val selectLedgerPayload: SelectLedgerPayload,
    private val router: LedgerRouter,
    private val resourceManager: ResourceManager,
    private val signPayloadState: SharedState<SignerPayloadExtrinsic>,
    private val extrinsicValidityUseCase: ExtrinsicValidityUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val request: SignInterScreenCommunicator.Request,
    private val responder: SignInterScreenResponder,
    private val interactor: SignLedgerInteractor,
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

    private val validityPeriod = flowOf {
        extrinsicValidityUseCase.extrinsicValidityPeriod(signPayloadState.getOrThrow())
    }.shareInBackground()

    private var signingJob: Deferred<SignatureWrapper>? = null

    private val fatalErrorDetected = MutableStateFlow(false)

    override fun backClicked() {
        exit()
    }

    init {
        listenExpire()
    }

    override suspend fun handleLedgerError(reason: Throwable, device: LedgerDevice) {
        if (fatalErrorDetected.value) return

        when {
            reason is SubstrateLedgerApplicationError.Response && reason.response == LedgerApplicationResponse.invalidData -> {
                handleInvalidData(reason.errorMessage)
            }

            reason is InvalidSignatureError -> handleInvalidSignature()

            else -> super.handleLedgerError(reason, device)
        }
    }

    override suspend fun verifyConnection(device: LedgerDevice) {
        val validityPeriod = validityPeriod.first()

        ledgerMessageCommands.value = LedgerMessageCommand.Show.Info(
            title = resourceManager.getString(R.string.ledger_sign_approve_title),
            subtitle = resourceManager.getString(R.string.ledger_sign_approve_message, device.name),
            graphics = Graphics(R.drawable.ic_eye_filled, R.color.white_64),
            onCancel = ::bottomSheetClosed,
            footer = LedgerMessageCommand.Footer.Timer(
                timerValue = validityPeriod.period,
                closeToExpire = { validityPeriod.closeToExpire() },
                timerFinished = { },
                messageFormat = R.string.ledger_sign_transaction_validity_format
            )
        ).event()

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

        if (interactor.verifySignature(signPayloadState.getOrThrow(), signature)) {
            responder.respond(request.signed(signature))
            hideBottomSheet()
            router.finishSignFlow()
        } else {
            throw InvalidSignatureError()
        }
    }

    private fun listenExpire() = launch {
        val remainingTime = validityPeriod.first().remainingTime()
        delay(remainingTime)

        timerExpired()
    }

    private fun handleInvalidSignature() {
        showFatalError(
            title = resourceManager.getString(R.string.common_signature_invalid),
            subtitle = resourceManager.getString(R.string.ledger_sign_signature_invalid_message),
        )
    }

    private suspend fun handleInvalidData(invalidDataMessage: String?) {
        val errorTitle: String
        val errorMessage: String

        when (matchInvalidDataMessage(invalidDataMessage)) {
            InvalidDataError.TX_NOT_SUPPORTED -> {
                errorTitle = resourceManager.getString(R.string.ledger_sign_tx_not_supported_title)
                errorMessage = resourceManager.getString(R.string.ledger_sign_tx_not_supported_subtitle)
            }
            InvalidDataError.METADATA_OUTDATED -> {
                errorTitle = resourceManager.getString(R.string.ledger_sign_metadata_outdated_title)
                errorMessage = resourceManager.getString(R.string.ledger_sign_metadata_outdated_subtitle, chain().name)
            }
            null -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
                errorMessage = invalidDataMessage ?: resourceManager.getString(R.string.ledger_error_general_message)
            }
        }

        showFatalError(errorTitle, errorMessage)
    }

    private suspend fun timerExpired() {
        signingJob?.cancel()

        val period = validityPeriod.first().period.millis.milliseconds
        val periodFormatted = resourceManager.formatDuration(period, estimated = false)

        showFatalError(
            title = resourceManager.getString(R.string.ledger_sign_transaction_expired_title),
            subtitle = resourceManager.getString(R.string.ledger_sign_transaction_expired_message, periodFormatted),
        )
    }

    private fun showFatalError(
        title: String,
        subtitle: String,
    ) {
        fatalErrorDetected.value = true

        ledgerMessageCommands.value = LedgerMessageCommand.Show.Actions.FatalError(
            title = title,
            subtitle = subtitle,
            graphics = Graphics(R.drawable.ic_warning_filled),
            onConfirm = ::errorAcknowledged
        ).event()
    }

    private fun bottomSheetClosed() {
        signingJob?.cancel()
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
    }

    private fun errorAcknowledged() {
        hideBottomSheet()

        exit()
    }

    private fun exit() {
        responder.respond(request.cancelled())
        router.finishSignFlow()
    }

    private fun hideBottomSheet() {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()
    }

    private fun matchInvalidDataMessage(message: String?): InvalidDataError? {
        return when (message) {
            "Method not supported" -> InvalidDataError.TX_NOT_SUPPORTED
            "Spec version not supported", "Txn version not supported" -> InvalidDataError.METADATA_OUTDATED
            else -> null
        }
    }
}
