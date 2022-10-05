package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.show

import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame.LegacyMultiPart
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction.paritySignerTxPayload
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSContentCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.ParitySignerUOSPayloadCode
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.UOS
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.paritySignerUOSCryptoType
import io.novafoundation.nova.runtime.ext.type
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ShowSignParitySignerInteractor {

    suspend fun qrCodeContent(payload: SignerPayloadExtrinsic): ParitySignerSignRequest
}

class ParitySignerSignRequest(
    val frame: String
)

class RealShowSignParitySignerInteractor(
    private val chainRegistry: ChainRegistry,
) : ShowSignParitySignerInteractor {

    override suspend fun qrCodeContent(payload: SignerPayloadExtrinsic): ParitySignerSignRequest = withContext(Dispatchers.Default) {
        val txPayload = payload.paritySignerTxPayload()
        val chain = chainRegistry.getChain(payload.chainId)

        val multiChainEncryption = when (chain.type) {
            Chain.Type.SUBSTRATE -> MultiChainEncryption.Substrate(EncryptionType.SR25519)
            Chain.Type.ETHEREUM -> MultiChainEncryption.Ethereum
        }

        val uosPayload = UOS.createUOSPayload(
            payload = txPayload,
            contentCode = ParitySignerUOSContentCode.SUBSTRATE,
            cryptoCode = multiChainEncryption.paritySignerUOSCryptoType(),
            payloadCode = ParitySignerUOSPayloadCode.TRANSACTION
        )
        val multiFramePayload = LegacyMultiPart.createSingle(uosPayload)

        val frame = multiFramePayload.toString(Charsets.ISO_8859_1)

        ParitySignerSignRequest(frame)
    }
}
