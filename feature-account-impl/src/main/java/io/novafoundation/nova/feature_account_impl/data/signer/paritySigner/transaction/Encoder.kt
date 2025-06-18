package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction

import io.novafoundation.nova.feature_account_api.data.signer.SignerPayload
import io.novafoundation.nova.runtime.extrinsic.metadata.ExtrinsicProof
import io.novafoundation.nova.runtime.extrinsic.signer.SignerPayloadRawWithChain
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.getGenesisHashOrThrow
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.transientEncodedCallData

fun SignerPayload.Extrinsic.paritySignerLegacyTxPayload(): ByteArray {
    return accountId +  extrinsic.transientEncodedCallData() + extrinsic.encodedExtensions() + extrinsic.getGenesisHashOrThrow()
}

fun SignerPayload.Extrinsic.paritySignerTxPayloadWithProof(proof: ExtrinsicProof): ByteArray {
    return accountId + proof.value + extrinsic.transientEncodedCallData() + extrinsic.encodedExtensions() + extrinsic.getGenesisHashOrThrow()
}

fun SignerPayloadRawWithChain.polkadotVaultSignRawPayload(): ByteArray {
    return accountId + message + chainId.fromHex()
}
