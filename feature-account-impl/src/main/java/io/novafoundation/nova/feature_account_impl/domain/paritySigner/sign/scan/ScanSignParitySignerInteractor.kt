package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan

import io.novafoundation.nova.common.utils.dropBytes
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanSignParitySignerInteractor {

    suspend fun encodeAndVerifySignature(payload: SignerPayloadExtrinsic, signature: String): Result<ByteArray>
}

class RealScanSignParitySignerInteractor : ScanSignParitySignerInteractor {

    override suspend fun encodeAndVerifySignature(payload: SignerPayloadExtrinsic, signature: String) = withContext(Dispatchers.Default) {
        runCatching {
            val signaturePayload = payload.encodedSignaturePayload(hashBigPayloads = true)
            val multiSignatureBytes = signature.fromHex()
            // first byte indicates encryption type
            val signatureBytes = multiSignatureBytes.dropBytes(1)
            val signatureWrapper = SignatureWrapper.Sr25519(signatureBytes)

            val valid = SignatureVerifier.verify(signatureWrapper, Signer.MessageHashing.SUBSTRATE, signaturePayload, payload.accountId)

            if (!valid) {
                throw IllegalArgumentException("Invalid signature")
            }

            signatureBytes
        }
    }
}
