package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan

import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.feature_account_api.data.signer.SignerPayload
import io.novafoundation.nova.feature_account_api.data.signer.accountId
import io.novafoundation.nova.feature_account_api.data.signer.signaturePayload
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanSignParitySignerInteractor {

    suspend fun encodeAndVerifySignature(payload: SignerPayload, signature: String): Result<ByteArray>
}

class RealScanSignParitySignerInteractor : ScanSignParitySignerInteractor {

    override suspend fun encodeAndVerifySignature(payload: SignerPayload, signature: String) = withContext(Dispatchers.Default) {
        runCatching {
            val signaturePayload = payload.signaturePayload()
            val signatureWrapper = payload.constructSignatureWrapper(signature)

            val valid = SignatureVerifier.verify(signatureWrapper, Signer.MessageHashing.SUBSTRATE, signaturePayload, payload.accountId())

            if (!valid) {
                throw IllegalArgumentException("Invalid signature")
            }

            signatureWrapper.signature
        }
    }

    private fun SignerPayload.constructSignatureWrapper(signature: String): SignatureWrapper {
        val allBytes = signature.fromHex()
        val signatureBytes = when (this) {
            // first byte indicates encryption type (aka MultiSignature)
            is SignerPayload.Extrinsic -> allBytes.dropBytes(1)

            is SignerPayload.Raw -> allBytes
        }

        return SignatureWrapper.Sr25519(signatureBytes)
    }
}
