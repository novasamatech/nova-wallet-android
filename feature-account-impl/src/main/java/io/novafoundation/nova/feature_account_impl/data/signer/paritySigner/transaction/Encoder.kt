package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction

import io.novafoundation.nova.runtime.extrinsic.metadata.ExtrinsicProof
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.getAccountIdOrThrow
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.getGenesisHashOrThrow
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.transientEncodedCallData

fun InheritedImplication.paritySignerLegacyTxPayload(): ByteArray {
    return getAccountIdOrThrow() + transientEncodedCallData() + encodedExtensions() + getGenesisHashOrThrow()
}

fun InheritedImplication.paritySignerTxPayloadWithProof(proof: ExtrinsicProof): ByteArray {
    return getAccountIdOrThrow() + proof.value + transientEncodedCallData() + encodedExtensions() + getGenesisHashOrThrow()
}
