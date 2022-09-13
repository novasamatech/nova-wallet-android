package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
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
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.cancelled
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.mapValidityPeriodToParcel
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.genesisHash
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ShowSignParitySignerViewModel(
    private val router: AccountRouter,
    private val interactor: ShowSignParitySignerInteractor,
    private val signSharedState: SharedState<SignerPayloadExtrinsic>,
    private val qrCodeGenerator: QrCodeGenerator,
    private val responder: SignInterScreenCommunicator,
    private val request: SignInterScreenCommunicator.Request,
    private val chainRegistry: ChainRegistry,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val externalActions: ExternalActions.Presentation,
    private val appLinksProvider: AppLinksProvider,
    private val qrCodeExpiredPresentableFactory: QrCodeExpiredPresentableFactory,
    private val extrinsicValidityUseCase: ExtrinsicValidityUseCase,
) : BaseViewModel(), ExternalActions by externalActions, Browserable {

    override val openBrowserEvent = mediatorLiveData { updateFrom(externalActions.openBrowserEvent) }

    val qrCodeExpiredPresentable = qrCodeExpiredPresentableFactory.create(request)

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
        extrinsicValidityUseCase.extrinsicValidityPeriod(signSharedState.getOrThrow())
    }.shareInBackground()

    fun backClicked() {
        responder.respond(request.cancelled())

        router.back()
    }

    fun continueClicked() = launch {
        val validityPeriodParcel = mapValidityPeriodToParcel(validityPeriod.first())
        val payload = ScanSignParitySignerPayload(request, validityPeriodParcel)

        router.openScanParitySignerSignature(payload)
    }

    fun troublesClicked() {
        openBrowserEvent.value = appLinksProvider.paritySignerTroubleShooting.event()
    }

    fun timerFinished() {
        launch {
            qrCodeExpiredPresentable.showQrCodeExpired(validityPeriod.first())
        }
    }

    fun addressClicked() = launch {
        val address = addressModel.first().address
        val chain = chain.first()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }
}
