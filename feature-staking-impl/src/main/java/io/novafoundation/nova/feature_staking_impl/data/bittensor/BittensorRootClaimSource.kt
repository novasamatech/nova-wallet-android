package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.createRequest
import io.novasama.substrate_sdk_android.runtime.metadata.decodeOutput
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.runtimeApi
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.math.BigInteger

interface BittensorRootClaimSource {
    suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims
}

class RealBittensorRootClaimSource(
    private val storage: StorageDataSource,
    private val rpc: RpcCalls,
    private val chains: ChainRegistry
) : BittensorRootClaimSource {
    override suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims = withTimeout(25_000) {
        val hash = rpc.getBlockHash(chainId)
        val socket = chains.getSocket(chainId)
        storage.query(chainId, at = hash, applyStorageDefault = false) {
            coroutineScope {
                val pallet = runtime.metadata.module("SubtensorModule")
                val coldkeySwap = async { pallet.storage("ColdkeySwapAnnouncements").query(coldkey, binding = { it }) }
                val safeMode = async { runtime.metadata.moduleOrNull("SafeMode")?.storageOrNull("EnteredUntil")?.query(binding = { it }) }
                val threshold = async {
                    pallet.storage("RootClaimableThreshold").query(BigInteger.ZERO, binding = { value ->
                        value?.let { bindNumber(it.castToStruct()["bits"]) }
                    })
                }
                val method = runtime.metadata.runtimeApi("BetaBasketRuntimeApi").method("get_root_basket_claim_previews")
                require(method.inputs.size == 1)
                val request = method.createRequest(runtime, mapOf(method.inputs.single().name to coldkey))
                val atBlock = RuntimeRequest(request.method, request.params + hash)
                val encoded = requireNotNull(socket.executeAsync(atBlock, mapper = pojo<String>()).result)
                val previews = bindBittensorRootClaimPreviews(method.decodeOutput(runtime, encoded))
                require(coldkeySwap.await() == null) { "Coldkey swap is pending" }
                require(safeMode.await() == null) { "The chain is in safe mode" }
                BittensorRootClaims(previews, bittensorMinimumRootClaim(threshold.await()))
            }
        }
    }
}
