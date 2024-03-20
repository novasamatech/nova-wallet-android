package io.novafoundation.nova.feature_governance_impl.data.repository.v1

import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.democracy
import io.novafoundation.nova.common.utils.hasStorage
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.data.repository.HexHash
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_impl.data.repository.v2.Gov2PreImageRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.metadata.storage

class GovV1PreImageRepository(
    private val remoteStorageSource: StorageDataSource,
    private val v2Delegate: Gov2PreImageRepository,
) : PreImageRepository {

    override suspend fun getPreimageFor(request: PreImageRequest, chainId: ChainId): PreImage? {
        return remoteStorageSource.query(chainId) {
            // The most recent democracy pallet version stores preimages the same way as gov v2 does
            // However, those changes are not yet live on Kusama & Polkadot
            if (runtime.metadata.democracy().hasStorage("Preimages")) {
                runtime.metadata.democracy().storage("Preimages").query(
                    request.hash,
                    binding = { bindPreimage(it, runtime) }
                )
            } else {
                // just in case something will fail during v2 delegate execution
                runCatching { v2Delegate.getPreimageFor(request, chainId) }.getOrNull()
            }
        }
    }

    override suspend fun getPreimagesFor(requests: Collection<PreImageRequest>, chainId: ChainId): Map<HexHash, PreImage?> {
        return remoteStorageSource.query(chainId) {
            if (runtime.metadata.democracy().hasStorage("Preimages")) {
                // Since it democracy pallet preimages stored un-sized - we do not implement bulk fetch due to possible big calls being stored
                // We cant efficiently use `state_getStorageSize` since it only allows to query one key at the time
                emptyMap()
            } else {
                runCatching { v2Delegate.getPreimagesFor(requests, chainId) }.getOrDefault(emptyMap())
            }
        }
    }

    private fun bindPreimage(
        decoded: Any?,
        runtime: RuntimeSnapshot,
    ): PreImage? = runCatching {
        val asDictEnum = decoded.castToDictEnum()

        when (asDictEnum.name) {
            "Available" -> {
                val valueStruct = asDictEnum.value.castToStruct()
                val callData = bindByteArray(valueStruct["data"])

                val runtimeCall = GenericCall.fromByteArray(runtime, callData)

                PreImage(encodedCall = callData, call = runtimeCall)
            }

            else -> null
        }
    }.getOrNull()
}
