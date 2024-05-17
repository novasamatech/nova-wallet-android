package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.metadata

import io.novafoundation.nova.common.utils.encodedIncludedInExtrinsic
import io.novafoundation.nova.common.utils.encodedIncludedInSignature
import io.novafoundation.nova.metadata_shortener.MetadataShortener
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRawMetadata
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedCallData
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.genesisHash

interface MetadataShortenerService {

    suspend fun generateExtrinsicProof(payloadExtrinsic: SignerPayloadExtrinsic): ByteArray
}

class RealMetadataShortenerService(
    private val chainRegistry: ChainRegistry,
    private val rpcCalls: RpcCalls,
) : MetadataShortenerService {

    override suspend fun generateExtrinsicProof(payloadExtrinsic: SignerPayloadExtrinsic): ByteArray {
        val chainId = payloadExtrinsic.genesisHash.toHexString(withPrefix = false)

        val chain = chainRegistry.getChain(chainId)
        val mainAsset = chain.utilityAsset

        val call = payloadExtrinsic.encodedCallData()
        val signedExtras = payloadExtrinsic.encodedIncludedInExtrinsic()
        val additionalSigned = payloadExtrinsic.encodedIncludedInSignature()

        val metadataVersion = rpcCalls.getRuntimeVersion(chainId)

        val metadata = chainRegistry.getRawMetadata(chainId)

        return MetadataShortener.generate_extrinsic_proof(
            call,
            signedExtras,
            additionalSigned,
            metadata.metadataContent,
            metadataVersion.specVersion,
            metadataVersion.specName,
            chain.addressPrefix,
            mainAsset.precision.value,
            mainAsset.symbol.value
        )
    }
}
