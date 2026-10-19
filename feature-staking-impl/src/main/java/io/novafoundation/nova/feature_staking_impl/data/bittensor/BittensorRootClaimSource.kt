package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

interface BittensorRootClaimSource {
    suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims

    fun observe(chainId: String, coldkey: AccountId): Flow<BittensorRootClaimSnapshot>
}

class RealBittensorRootClaimSource(
    private val storage: StorageDataSource,
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val computationalCache: ComputationalCache,
    private val logger: Logger,
    private val clock: BittensorClaimClock
) : BittensorRootClaimSource {

    override fun observe(chainId: String, coldkey: AccountId): Flow<BittensorRootClaimSnapshot> = flow {
        coroutineScope {
            val key = "Bittensor.Claims.$chainId.${coldkey.blake2b256().toHexString()}"
            val shared = computationalCache.useSharedFlow(key, this) {
                val blocks = flowOfAll { chainStateRepository.currentRemoteBlockNumberFlow(chainId) }
                observeBittensorRootClaims(blocks, { claims(chainId, coldkey) }, logger)
            }
            emitAll(shared)
        }
    }

    override suspend fun claims(chainId: String, coldkey: AccountId): BittensorRootClaims = withTimeoutOrNull(25_000) {
        val readStartedAt = clock.nowMillis()
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
                val rootStake = async {
                    val positions = callBittensorRuntimeApi(
                        socket = socket,
                        blockHash = hash,
                        section = "StakeInfoRuntimeApi",
                        method = "get_stake_info_for_coldkey",
                        arguments = listOf(coldkey)
                    )
                    bindBittensorRootPositions(positions).fold(BigInteger.ZERO) { total, position -> total + position.stakeInPlanks }
                }
                val holdDuration = async {
                    val blocks = pallet.storage("RootStakeUnlockInterval")
                        .query(binding = ::bindNumberOrZero)
                    blocks * chainStateRepository.expectedBlockTimeInMillis(chainId)
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
                    safeModeActive = safeMode.await() != null,
                    rootStake = rootStake.await(),
                    rootHoldDurationMillis = holdDuration.await(),
                    readStartedAtMillis = readStartedAt
                )
            }
        }
    } ?: error("Root claim availability timed out")
}
