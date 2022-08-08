package io.novafoundation.nova.feature_account_impl.domain.paritySigner.sign.scan

import io.novafoundation.nova.common.utils.dropBytes
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.encodedSignaturePayload
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

            val valid = Signer.verifySr25519(signaturePayload, signatureBytes, payload.accountId)

            if (!valid) {
                throw IllegalArgumentException("Invalid signature")
            }

            signatureBytes
        }
    }
}
