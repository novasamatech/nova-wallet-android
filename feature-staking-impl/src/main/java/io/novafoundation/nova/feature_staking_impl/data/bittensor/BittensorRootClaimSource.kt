package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

interface BittensorRootClaimSource {
    suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims
}

class RealBittensorRootClaimSource(
    private val storage: StorageDataSource,
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry
) : BittensorRootClaimSource {
    override suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims = withTimeout(25_000) {
        val hash = rpcCalls.getBlockHash(chainId)
        val socket = chainRegistry.getSocket(chainId)
        storage.query(
            chainId,
            at = hash,
            applyStorageDefault = false
        ) {
            coroutineScope {
                val pallet = runtime.metadata.module("SubtensorModule")
                val coldkeySwap = async {
                    pallet.storage("ColdkeySwapAnnouncements")
                        .query(
                            coldkey,
                            binding = { it }
                        )
                }
                val safeMode = async {
                    runtime.metadata.moduleOrNull("SafeMode")
                        ?.storageOrNull("EnteredUntil")
                        ?.query(binding = { it })
                }
                val threshold = async {
                    pallet.storage("RootClaimableThreshold")
                        .query(
                            BigInteger.ZERO,
                            binding = { value ->
                                value?.let { bindNumber(it.castToStruct()["bits"]) }
                            }
                        )
                }
                val decoded = callBittensorRuntimeApi(
                    socket = socket,
                    blockHash = hash,
                    section = "BetaBasketRuntimeApi",
                    method = "get_root_basket_claim_previews",
                    arguments = listOf(coldkey)
                )
                val previews = bindBittensorRootClaimPreviews(decoded)
                BittensorRootClaims(
                    previews = previews,
                    minimum = bittensorMinimumRootClaim(threshold.await()),
                    coldkeySwapPending = coldkeySwap.await() != null,
                    safeModeActive = safeMode.await() != null
                )
            }
        }
    }
}
