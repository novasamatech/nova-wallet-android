package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show

import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.windowed
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame.LegacyMultiPart
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerLegacyTxPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerTxPayloadWithProof
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSContentCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSPayloadCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.UOS
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.paritySignerUOSCryptoType
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ShowSignParitySignerInteractor {

    suspend fun qrCodeContent(
        payload: SignerPayloadExtrinsic,
        mode: ParitySignerSignMode,
    ): ParitySignerSignRequest
}

class ParitySignerSignRequest(
    val frames: List<String>
)

class RealShowSignParitySignerInteractor(
    private val metadataShortenerService: MetadataShortenerService,
) : ShowSignParitySignerInteractor {

    override suspend fun qrCodeContent(
        payload: SignerPayloadExtrinsic,
        mode: ParitySignerSignMode,
    ): ParitySignerSignRequest = withContext(Dispatchers.Default) {
        val uosPayload = mode.createUOSPayloadFor(payload)
        val windowed = uosPayload.windowed(QrCodeGenerator.MAX_PAYLOAD_LENGTH)
        val multiFramePayloads = LegacyMultiPart.createMultiple(windowed)

        val frame = multiFramePayloads.map { it.toString(Charsets.ISO_8859_1) }

        ParitySignerSignRequest(frame)
    }

    private suspend fun ParitySignerSignMode.createUOSPayloadFor(payload: SignerPayloadExtrinsic): ByteArray {
        return when (this) {
            ParitySignerSignMode.LEGACY -> createLegacyUOSPayload(payload)
            ParitySignerSignMode.WITH_METADATA_PROOF -> createUOSPayloadWithProof(payload)
        }
    }

    private fun createLegacyUOSPayload(payload: SignerPayloadExtrinsic): ByteArray {
        val txPayload = payload.paritySignerLegacyTxPayload()
        return UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = CryptoType.SR25519.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.TRANSACTION
        )
    }

    private suspend fun createUOSPayloadWithProof(payload: SignerPayloadExtrinsic): ByteArray {
        val proof = metadataShortenerService.generateExtrinsicProof(payload)
        val txPayload = payload.paritySignerTxPayloadWithProof(proof)
        return UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = CryptoType.SR25519.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.TRANSACTION_WITH_PROOF
        )
    }
}
