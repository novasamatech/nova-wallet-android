package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction

import io.novafoundation.nova.runtime.extrinsic.metadata.ExtrinsicProof
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataProof
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedCallData
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedExtensions
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.genesisHash
import io.novasama.substrate_sdk_android.scale.dataType.compactInt
import io.novasama.substrate_sdk_android.scale.dataType.toByteArray

fun SignerPayloadExtrinsic.paritySignerLegacyTxPayload(): ByteArray {
    return accountId + transientCallData() + encodedExtensions() + genesisHash
}

fun SignerPayloadExtrinsic.paritySignerTxPayloadWithProof(proof: ExtrinsicProof): ByteArray {
    return accountId + proof.value + transientCallData() + encodedExtensions() + genesisHash
}

private fun SignerPayloadExtrinsic.transientCallData(): ByteArray {
    val encodedCallData = encodedCallData()
    val encodedCallSize = encodedCallData.size.toBigInteger()
    val encodedCallCompact = compactInt.toByteArray(encodedCallSize)

    return encodedCallCompact + encodedCallData
}
