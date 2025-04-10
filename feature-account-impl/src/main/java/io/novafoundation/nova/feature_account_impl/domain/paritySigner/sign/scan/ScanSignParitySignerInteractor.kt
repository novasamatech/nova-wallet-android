package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan

import io.novafoundation.nova.common.utils.dropBytes
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.getAccountIdOrThrow
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.signingPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ScanSignParitySignerInteractor {

    suspend fun encodeAndVerifySignature(payload: InheritedImplication, signature: String): Result<ByteArray>
}

class RealScanSignParitySignerInteractor : ScanSignParitySignerInteractor {

    override suspend fun encodeAndVerifySignature(payload: InheritedImplication, signature: String) = withContext(Dispatchers.Default) {
        runCatching {
            val signaturePayload = payload.signingPayload()
            val multiSignatureBytes = signature.fromHex()
            // first byte indicates encryption type
            val signatureBytes = multiSignatureBytes.dropBytes(1)
            val signatureWrapper = SignatureWrapper.Sr25519(signatureBytes)
            val accountId = payload.getAccountIdOrThrow()

            val valid = SignatureVerifier.verify(signatureWrapper, Signer.MessageHashing.SUBSTRATE, signaturePayload, accountId)

            if (!valid) {
                throw IllegalArgumentException("Invalid signature")
            }

            signatureBytes
        }
    }
}
