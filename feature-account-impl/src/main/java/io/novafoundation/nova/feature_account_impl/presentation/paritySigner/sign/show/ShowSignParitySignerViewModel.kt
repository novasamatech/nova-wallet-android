package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenResponder
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.cancelled
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
    private val responder: ParitySignerSignInterScreenResponder,
    private val request: ParitySignerSignInterScreenCommunicator.Request,
    private val chainRegistry: ChainRegistry,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(), ExternalActions by externalActions {

    val chain = flowOf {
        val signPayload = signSharedState.get()!!
        val chainId = signPayload.genesisHash.toHexString()

        chainRegistry.getChain(chainId)
    }.shareInBackground()

    val qrCode = flowOf {
        val signPayload = signSharedState.get()!!

        val qrContent = interactor.qrCodeContent(signPayload)

        qrCodeGenerator.generateQrBitmap(qrContent.frame)
    }.shareInBackground()

    val addressModel = chain.map { chain ->
        val signPayload = signSharedState.get()!!

        addressIconGenerator.createAccountAddressModel(chain, signPayload.accountId, addressDisplayUseCase)
    }.shareInBackground()

    val validityPeriod = flowOf {
        interactor.extrinsicValidityPeriod(signSharedState.get()!!)
    }.shareInBackground()

    fun backClicked() {
        responder.respond(request.cancelled())

        router.back()
    }

    fun timerFinished() {
        showMessage("TODO - timer expired")
    }

    fun addressClicked()  = launch {
        val address = addressModel.first().address
        val chain = chain.first()

        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
    }
}
