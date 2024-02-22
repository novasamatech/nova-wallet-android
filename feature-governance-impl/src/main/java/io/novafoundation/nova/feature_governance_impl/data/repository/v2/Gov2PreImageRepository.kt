package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.preImage
import io.novafoundation.nova.common.utils.storageOrFallback
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.repository.HexHash
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition
import io.novafoundation.nova.feature_governance_impl.data.preimage.PreImageSizer
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Tuple
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntryType
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

class Gov2PreImageRepository(
    private val remoteSource: StorageDataSource,
    private val preImageSizer: PreImageSizer,
) : PreImageRepository {

    override suspend fun getPreimageFor(request: PreImageRequest, chainId: ChainId): PreImage? {
        return remoteSource.query(chainId) {
            val storage = runtime.metadata.preImageForStorage()
            val shouldKnowSize = storage.shouldKnowSizeExecuting(request)

            val preImageSize = if (shouldKnowSize) {
                request.knownSize ?: fetchPreImageLength(request.hash)
            } else {
                request.knownSize
            }

            val shouldFetch = request.fetchIf.shouldFetch(actualPreImageSize = preImageSize)

            val canFetchPreimage = if (storage.requiresSize()) preImageSize != null else true
            val canReturnValue = shouldFetch && canFetchPreimage

            if (!canReturnValue) return@query null

            val key = storage.preImageStorageKey(request.hash, preImageSize)
            storage.query(key, binding = { bindPreimage(it, runtime) })
        }
    }

    override suspend fun getPreimagesFor(requests: Collection<PreImageRequest>, chainId: ChainId): Map<HexHash, PreImage?> {
        return remoteSource.query(chainId) {
            val storage = runtime.metadata.preImageForStorage()
            val shouldKnowSizes = requests.associateBy(
                keySelector = { it.hashHex },
                valueTransform = { request -> storage.shouldKnowSizeExecuting(request) }
            )
            val hashesToFetchSize = requests.mapNotNull {
                if (it.hashHex in shouldKnowSizes && it.knownSize == null) {
                    it.hash
                } else {
                    null
                }
            }
            val fetchedSizes = fetchPreImagesLength(hashesToFetchSize)

            val preKnownSizes = requests.associateBy(
                keySelector = { it.hashHex },
                valueTransform = { it.knownSize }
            )
            val allKnownSizes = preKnownSizes + fetchedSizes

            val keysToFetch = requests.mapNotNull { request ->
                val preImageSize = allKnownSizes[request.hashHex]

                val shouldFetch = request.fetchIf.shouldFetch(actualPreImageSize = preImageSize)
                val canFetchPreimage = if (storage.requiresSize()) preImageSize != null else true

                if (shouldFetch && canFetchPreimage) {
                    storage.preImageStorageKey(request.hash, preImageSize)
                } else {
                    null
                }
            }
            storage.entries(
                keysArguments = keysToFetch.wrapSingleArgumentKeys(),
                keyExtractor = { (hashAndLen: List<*>) -> bindByteArray(hashAndLen.first()).toHexString() },
                binding = { decoded, _ -> bindPreimage(decoded, runtime) }
            )
        }
    }

    private fun StorageEntry.shouldKnowSizeExecuting(request: PreImageRequest): Boolean {
        return requiresSize() || request.fetchIf == FetchCondition.SMALL_SIZE
    }

    private fun StorageEntry.preImageStorageKey(hash: ByteArray, preImageSize: BigInteger?): Any {
        return if (requiresSize()) {
            listOf(hash, preImageSize!!)
        } else {
            hash
        }
    }

    private fun FetchCondition.shouldFetch(actualPreImageSize: BigInteger?): Boolean {
        return when (this) {
            FetchCondition.ALWAYS -> true
            FetchCondition.SMALL_SIZE ->
                actualPreImageSize != null &&
                    preImageSizer.satisfiesSizeConstraint(actualPreImageSize, PreImageSizer.SizeConstraint.SMALL)
        }
    }

    private suspend fun StorageQueryContext.fetchPreImageLength(callHash: ByteArray): BigInteger? {
        return runtime.metadata.preImage().storageOrFallback("RequestStatusFor", "StatusFor")
            .query(
                callHash,
                binding = ::bindPreImageLength
            )
    }

    private suspend fun StorageQueryContext.fetchPreImagesLength(callHashes: Collection<ByteArray>): Map<HexHash, BigInteger?> {
        return runtime.metadata.preImage().storageOrFallback("RequestStatusFor", "StatusFor")
            .entries(
                keysArguments = callHashes.wrapSingleArgumentKeys(),
                keyExtractor = { (callHash: ByteArray) -> callHash.toHexString() },
                binding = { decoded, _ -> bindPreImageLength(decoded) }
            )
    }

    private fun bindPreImageLength(decoded: Any?): BigInteger? = runCatching {
        val asDictEnum = decoded.castToDictEnum()
        // every variant of RequestStatus is struct that has len field
        val valueStruct = asDictEnum.value.castToStruct()

        bindNumber(valueStruct["len"])
    }
        .getOrNull()

    private fun bindPreimage(
        decoded: Any?,
        runtime: RuntimeSnapshot,
    ): PreImage? {
        val asByteArray = decoded.castOrNull<ByteArray>() ?: return null

        val runtimeCall = runCatching {
            GenericCall.fromByteArray(runtime, asByteArray)
        }.getOrNull()

        return runtimeCall?.let {
            PreImage(
                encodedCall = asByteArray,
                call = it,
            )
        }
    }

    private fun RuntimeMetadata.preImageForStorage(): StorageEntry {
        return preImage().storage("PreimageFor")
    }

    private fun StorageEntry.requiresSize(): Boolean {
        val keys = type.cast<StorageEntryType.NMap>().keys
        val argument = keys.first()

        // for newer version of the pallet key is (CallHash, Length)
        return argument is Tuple
    }
}
