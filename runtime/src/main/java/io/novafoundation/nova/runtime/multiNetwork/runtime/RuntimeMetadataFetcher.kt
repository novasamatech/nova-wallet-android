package io.novafoundation.nova.runtime.multiNetwork.runtime

import android.util.Log
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.rpc.StateCallRequest
import io.novafoundation.nova.runtime.network.rpc.stateCall
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.metadata.GetMetadataRequest
import io.novasama.substrate_sdk_android.scale.dataType.list
import io.novasama.substrate_sdk_android.scale.dataType.toHex
import io.novasama.substrate_sdk_android.scale.dataType.uint32
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo

private const val LATEST_SUPPORTED_METADATA_VERSION = 15

class RuntimeMetadataFetcher {

    suspend fun fetchRawMetadata(
        chainId: ChainId,
        socketService: SocketService
    ): RawRuntimeMetadata {
        return runCatching {
            socketService.fetchNewestMetadata()
        }.onSuccess {
            Log.d("RuntimeMetadataFetcher", "Fetched metadata via runtime call for $chainId")
        }.getOrElse {
            Log.d("RuntimeMetadataFetcher", "Failed to sync metadata via runtime call, fall-backing to legacy rpc call for chain $chainId", it)

            socketService.fetchLegacyMetadata()
        }
    }

    private suspend fun SocketService.fetchNewestMetadata(): RawRuntimeMetadata {
        val availableVersions = getAvailableMetadataVersions()
        val latestSupported = availableVersions.sorted().last { it <= LATEST_SUPPORTED_METADATA_VERSION }

        return RawRuntimeMetadata(
            metadataContent = getMetadataAtVersion(latestSupported),
            isOpaque = true
        )
    }

    private suspend fun SocketService.fetchLegacyMetadata(): RawRuntimeMetadata {
        val result = executeAsync(GetMetadataRequest, mapper = pojo<String>().nonNull())

        return RawRuntimeMetadata(
            metadataContent = result.fromHex(),
            isOpaque = false
        )
    }

    private suspend fun SocketService.getAvailableMetadataVersions(): List<Int> {
        val request = StateCallRequest(runtimeRpcName = "Metadata_metadata_versions", "0x")
        return stateCall(request, returnType = list(uint32)).map { it.toInt() }
    }

    private suspend fun SocketService.getMetadataAtVersion(version: Int): ByteArray {
        val versionEncoded = uint32.toHex(version.toUInt())
        val request = StateCallRequest("Metadata_metadata_at_version", versionEncoded)
        val response = stateCall(request)
        requireNotNull(response) {
            "Non existent metadata"
        }

        return response.fromHex()
    }
}
