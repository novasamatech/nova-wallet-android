package io.novafoundation.nova.runtime.extrinsic.metadata

import android.util.Log
import io.novafoundation.nova.common.utils.hasSignedExtension
import io.novafoundation.nova.metadata_shortener.MetadataShortener
import io.novafoundation.nova.runtime.ext.shouldDisableMetadataHashCheck
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRawMetadata
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.DefaultSignedExtensions
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHashMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.getGenesisHashOrThrow
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersionFull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface MetadataShortenerService {

    suspend fun isCheckMetadataHashAvailable(chainId: ChainId): Boolean

    suspend fun generateExtrinsicProof(payloadExtrinsic: InheritedImplication): ExtrinsicProof

    suspend fun generateMetadataProof(chainId: ChainId): MetadataProof

    suspend fun generateDisabledMetadataProof(chainId: ChainId): MetadataProof
}

private const val MINIMUM_METADATA_VERSION_TO_CALCULATE_HASH = 15

internal class RealMetadataShortenerService(
    private val chainRegistry: ChainRegistry,
    private val rpcCalls: RpcCalls,
) : MetadataShortenerService {

    private val cache = MetadataProofCache()
    override suspend fun isCheckMetadataHashAvailable(chainId: ChainId): Boolean {
        val runtime = chainRegistry.getRuntime(chainId)
        val chain = chainRegistry.getChain(chainId)

        return shouldCalculateMetadataHash(runtime.metadata, chain)
    }

    override suspend fun generateExtrinsicProof(inheritedImplication: InheritedImplication): ExtrinsicProof {
        val chainId = inheritedImplication.getGenesisHashOrThrow().toHexString(withPrefix = false)

        val chain = chainRegistry.getChain(chainId)
        val mainAsset = chain.utilityAsset

        val call = inheritedImplication.encodedCall()
        val signedExtras = inheritedImplication.encodedExplicits()
        val additionalSigned = inheritedImplication.encodedImplicits()

        val metadataVersion = rpcCalls.getRuntimeVersion(chainId)

        val metadata = chainRegistry.getRawMetadata(chainId)

        val proof = MetadataShortener.generate_extrinsic_proof(
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

        return ExtrinsicProof(proof)
    }

    override suspend fun generateMetadataProof(chainId: ChainId): MetadataProof {
        val runtimeVersion = rpcCalls.getRuntimeVersion(chainId)
        val chain = chainRegistry.getChain(chainId)

        return cache.getOrCompute(chainId, expectedSpecVersion = runtimeVersion.specVersion) {
            val runtimeMetadata = chainRegistry.getRuntime(chainId).metadata
            val shouldIncludeHash = shouldCalculateMetadataHash(runtimeMetadata, chain)

            if (shouldIncludeHash) {
                generateMetadataProofOrDisabled(chain, runtimeVersion)
            } else {
                MetadataProof(
                    checkMetadataHash = CheckMetadataHashMode.Disabled,
                    usedVersion = runtimeVersion
                )
            }
        }
    }

    override suspend fun generateDisabledMetadataProof(chainId: ChainId): MetadataProof {
        val runtimeVersion = rpcCalls.getRuntimeVersion(chainId)

        return MetadataProof(
            checkMetadataHash = CheckMetadataHashMode.Disabled,
            usedVersion = runtimeVersion,
        )
    }

    private suspend fun generateMetadataProofOrDisabled(
        chain: Chain,
        runtimeVersion: RuntimeVersionFull
    ): MetadataProof {
        return runCatching {
            val hash = generateMetadataHash(chain, runtimeVersion)

            MetadataProof(
                checkMetadataHash = CheckMetadataHashMode.Enabled(hash),
                usedVersion = runtimeVersion
            )
        }.getOrElse { // Fallback to disabled in case something went wrong during hash generation
            Log.w("MetadataShortenerService", "Failed to generate metadata hash", it)

            MetadataProof(
                checkMetadataHash = CheckMetadataHashMode.Disabled,
                usedVersion = runtimeVersion
            )
        }
    }

    private suspend fun generateMetadataHash(
        chain: Chain,
        runtimeVersion: RuntimeVersionFull
    ): ByteArray {
        val rawMetadata = chainRegistry.getRawMetadata(chain.id)
        val mainAsset = chain.utilityAsset

        return MetadataShortener.generate_metadata_digest(
            rawMetadata.metadataContent,
            runtimeVersion.specVersion,
            runtimeVersion.specName,
            chain.addressPrefix,
            mainAsset.precision.value,
            mainAsset.symbol.value
        )
    }

    private class MetadataProofCache {

        // Quote simple synchronization - we block all chains
        // Shouldn't be a problem at the moment since we don't expect a lot of multi-chain txs/fee estimations to happen at once
        private val cacheAccessMutex: Mutex = Mutex()
        private val cache = mutableMapOf<ChainId, MetadataProof>()

        suspend fun getOrCompute(
            chainId: ChainId,
            expectedSpecVersion: Int,
            compute: suspend () -> MetadataProof
        ): MetadataProof = cacheAccessMutex.withLock {
            val cachedProof = cache[chainId]

            if (cachedProof == null || cachedProof.usedVersion.specVersion != expectedSpecVersion) {
                val newValue = compute()
                cache[chainId] = newValue

                newValue
            } else {
                cachedProof
            }
        }
    }

    private fun shouldCalculateMetadataHash(runtimeMetadata: RuntimeMetadata, chain: Chain): Boolean {
        val canBeEnabled = chain.additional.shouldDisableMetadataHashCheck().not()
        val atLeastMinimumVersion = runtimeMetadata.metadataVersion >= MINIMUM_METADATA_VERSION_TO_CALCULATE_HASH
        val hasSignedExtension = runtimeMetadata.extrinsic.hasSignedExtension(DefaultSignedExtensions.CHECK_METADATA_HASH)

        return canBeEnabled && atLeastMinimumVersion && hasSignedExtension
    }
}
