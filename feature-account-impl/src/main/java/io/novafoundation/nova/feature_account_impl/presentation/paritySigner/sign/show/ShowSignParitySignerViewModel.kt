package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.getOrThrow
import io.novafoundation.nova.common.utils.mediatorLiveData
import io.novafoundation.nova.common.utils.updateFrom
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenResponder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.cancelled
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.genesisHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class ShowSignParitySignerViewModel(
    private val router: AccountRouter,
    private val interactor: ShowSignParitySignerInteractor,
    private val signSharedState: SharedState<SignerPayloadExtrinsic>,
    private val qrCodeGenerator: QrCodeGenerator,
    private val responder: ParitySignerSignInterScreenResponder,
    private val request: ParitySignerSignInterScreenCommunicator.Request,
    private val chainRegistry: ChainRegistry,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val externalActions: ExternalActions.Presentation,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val appLinksProvider: AppLinksProvider,
) : BaseViewModel(), ExternalActions by externalActions, Browserable {

    override val openBrowserEvent = mediatorLiveData<Event<String>> { updateFrom(externalActions.openBrowserEvent) }

    val acknowledgeExpired = actionAwaitableMixinFactory.confirmingAction<String>()

    val chain = flowOf {
        val signPayload = signSharedState.getOrThrow()
        val chainId = signPayload.genesisHash.toHexString()

        chainRegistry.getChain(chainId)
    }.shareInBackground()

    val qrCode = flowOf {
        val signPayload = signSharedState.getOrThrow()

        val qrContent = interactor.qrCodeContent(signPayload)

        qrCodeGenerator.generateQrBitmap(qrContent.frame)
    }.shareInBackground()

    val addressModel = chain.map { chain ->
        val signPayload = signSharedState.getOrThrow()

        addressIconGenerator.createAccountAddressModel(chain, signPayload.accountId, addressDisplayUseCase)
    }.shareInBackground()

    val validityPeriod = flowOf {
        val timerValue = interactor.extrinsicValidityPeriod(signSharedState.getOrThrow())

        ValidityPeriod(timerValue)
    }.shareInBackground()

    fun backClicked() {
        responder.respond(request.cancelled())

        router.back()
    }

    fun continueClicked() {
        val payload = ScanSignParitySignerPayload(request)

        router.openScanParitySignerSignature(payload)
    }

    fun troublesClicked() {
        openBrowserEvent.value = appLinksProvider.paritySignerTroubleShooting.event()
    }

    @OptIn(ExperimentalTime::class)
    fun timerFinished() = launch {
        val message = withContext(Dispatchers.Default) {
            val validityPeriodMillis = validityPeriod.first().period.millis
            val durationFormatted = resourceManager.formatDuration(validityPeriodMillis.milliseconds, estimated = false)
            resourceManager.getString(R.string.account_parity_signer_sign_qr_code_expired_descrition, durationFormatted)
        }

        acknowledgeExpired.awaitAction(message)

        backClicked()
    }

    fun addressClicked() = launch {
        val address = addressModel.first().address
        val chain = chain.first()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }
}
