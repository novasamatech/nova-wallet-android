package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan

import io.novafoundation.nova.common.utils.SignatureWrapperEcdsa
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.verifyByAccountId
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos.fromParitySignerCryptoType
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType
import jp.co.soramitsu.fearless_utils.encrypt.SignatureVerifier
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.encodedSignaturePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanSignParitySignerInteractor {

    suspend fun encodeAndVerifySignature(payload: SignerPayloadExtrinsic, signature: String): Result<SignatureWrapper>
}

class RealScanSignParitySignerInteractor(
    private val chainRegistry: ChainRegistry
): ScanSignParitySignerInteractor {

    override suspend fun encodeAndVerifySignature(payload: SignerPayloadExtrinsic, signature: String) = withContext(Dispatchers.Default) {
        runCatching {
            val chain = chainRegistry.getChain(payload.chainId)

            val signaturePayload = payload.encodedSignaturePayload(hashBigPayloads = true)
            val multiSignatureBytes = signature.fromHex()

            val signatureType = multiSignatureBytes.first()
            val signatureBytes = multiSignatureBytes.dropBytes(1)

            val signatureWrapper = when(EncryptionType.fromParitySignerCryptoType(signatureType)) {
                EncryptionType.ED25519 -> SignatureWrapper.Ed25519(signatureBytes)
                EncryptionType.SR25519 -> SignatureWrapper.Sr25519(signatureBytes)
                EncryptionType.ECDSA -> SignatureWrapperEcdsa(signatureBytes)
            }

            val messageHashing = if (chain.isEthereumBased) {
                Signer.MessageHashing.ETHEREUM
            } else {
                Signer.MessageHashing.SUBSTRATE
            }

            val valid = SignatureVerifier.verifyByAccountId(signatureWrapper, signaturePayload, payload.accountId, messageHashing)

            if (!valid) {
                throw IllegalArgumentException("Invalid signature")
            }

            signatureWrapper
        }
    }
}
