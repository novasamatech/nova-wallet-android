package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show

import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.common.utils.windowed
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.signer.SignerPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame.LegacyMultiPart
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerLegacyTxPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerTxPayloadWithProof
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.polkadotVaultSignRawPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSContentCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSPayloadCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.UOS
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.paritySignerUOSCryptoType
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.signer.SignerPayloadRawWithChain
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ShowSignParitySignerInteractor {

    suspend fun qrCodeContent(
        payload: SignerPayload,
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
        payload: SignerPayload,
        mode: ParitySignerSignMode
    ): ParitySignerSignRequest = withContext(Dispatchers.Default) {
        val uosPayload = when (payload) {
            is SignerPayload.Extrinsic -> mode.createUOSPayloadFor(payload.extrinsic)
            is SignerPayload.Raw -> createRawMessagePayload(payload.raw)
        }

        val windowed = uosPayload.windowed(QrCodeGenerator.MAX_PAYLOAD_LENGTH)
        val multiFramePayloads = LegacyMultiPart.createMultiple(windowed)

        val frame = multiFramePayloads.map { it.toString(Charsets.ISO_8859_1) }

        ParitySignerSignRequest(frame)
    }

    private fun createRawMessagePayload(payload: SignerPayloadRawWithChain): ByteArray {
        val txPayload = payload.polkadotVaultSignRawPayload()
        return UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = CryptoType.SR25519.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.MESSAGE
        )
    }

    private suspend fun ParitySignerSignMode.createUOSPayloadFor(payload: InheritedImplication): ByteArray {
        return when (this) {
            ParitySignerSignMode.LEGACY -> createLegacyUOSPayload(payload)
            ParitySignerSignMode.WITH_METADATA_PROOF -> createUOSPayloadWithProof(payload)
        }
    }

    private fun createLegacyUOSPayload(payload: InheritedImplication): ByteArray {
        val txPayload = payload.paritySignerLegacyTxPayload()
        return UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = CryptoType.SR25519.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.TRANSACTION
        )
    }

    private suspend fun createUOSPayloadWithProof(payload: InheritedImplication): ByteArray {
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
