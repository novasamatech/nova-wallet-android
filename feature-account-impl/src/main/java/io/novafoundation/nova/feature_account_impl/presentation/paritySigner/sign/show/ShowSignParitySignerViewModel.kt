package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.show

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show.ShowSignParitySignerInteractor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic

class ShowSignParitySignerViewModel(
    private val interactor: ShowSignParitySignerInteractor,
    private val signSharedState: SharedState<SignerPayloadExtrinsic>,
    private val qrCodeGenerator: QrCodeGenerator
) : BaseViewModel() {

    val qrCode = flowOf {
        val signPayload = signSharedState.get()!!

        val qrContent = interactor.qrCodeContent(signPayload)

        qrCodeGenerator.generateQrBitmap(qrContent.frame)
    }.shareInBackground()
}
