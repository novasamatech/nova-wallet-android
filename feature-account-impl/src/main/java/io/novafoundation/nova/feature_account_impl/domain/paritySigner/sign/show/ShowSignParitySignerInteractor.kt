package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame.LegacyMultiPart
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerTxPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSContentCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSPayloadCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.UOS
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.paritySignerUOSCryptoType
import io.novafoundation.nova.runtime.extrinsic.MortalityConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ShowSignParitySignerInteractor {

    suspend fun extrinsicValidityPeriod(payload: SignerPayloadExtrinsic): TimerValue

    suspend fun qrCodeContent(payload: SignerPayloadExtrinsic): ParitySignerSignRequest
}

class ParitySignerSignRequest(
    val frame: String
)

class RealShowSignParitySignerInteractor(
    private val mortalityConstructor: MortalityConstructor,
) : ShowSignParitySignerInteractor {

    override suspend fun extrinsicValidityPeriod(payload: SignerPayloadExtrinsic): TimerValue {
        return TimerValue(
            millis = mortalityConstructor.mortalPeriodMillis(),
            millisCalculatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun qrCodeContent(payload: SignerPayloadExtrinsic): ParitySignerSignRequest = withContext(Dispatchers.Default) {
        val txPayload = payload.paritySignerTxPayload()
        val uosPayload = UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = CryptoType.SR25519.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.TRANSACTION
        )
        val multiFramePayload = LegacyMultiPart.createSingle(uosPayload)

        val frame = multiFramePayload.toString(Charsets.ISO_8859_1)

        ParitySignerSignRequest(frame)
    }
}
