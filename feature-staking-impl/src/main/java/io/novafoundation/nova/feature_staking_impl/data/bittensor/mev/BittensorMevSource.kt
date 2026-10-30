package io.novafoundation.nova.feature_staking_impl.data.bittensor.mev

import java.math.BigInteger
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindByteArray
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevFunds
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevTiming
import io.novafoundation.nova.runtime.extrinsic.Mortality
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.callOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface BittensorMevSource {

    suspend fun nextKey(chain: Chain): ByteArray?

    suspend fun funds(chain: Chain, accountId: ByteArray): BittensorMevFunds

    suspend fun timing(chain: Chain, address: String): BittensorMevTiming
}

class RemoteBittensorMevSource(private val storage: StorageDataSource, private val rpc: RpcCalls) : BittensorMevSource {

    override suspend fun nextKey(chain: Chain): ByteArray? = storage.query(chain.id, applyStorageDefault = false) {
        val pallet = runtime.metadata.moduleOrNull("MevShield") ?: return@query null
        if (pallet.callOrNull("submit_encrypted") == null) return@query null
        val entry = pallet.storageOrNull("NextKey") ?: return@query null
        entry.query(binding = { it?.let(::bindByteArray) })?.also {
            require(it.size == BittensorMevProtocol.PUBLIC_KEY_BYTES) { "Invalid MEV public key length" }
        }
    }

    override suspend fun funds(chain: Chain, accountId: ByteArray): BittensorMevFunds = storage.query(chain.id) {
        val balance = runtime.metadata.module("System").storage("Account")
            .query(accountId, binding = ::bindAccountInfo).data
        val deposit = runtime.metadata.module("Balances").numberConstant("ExistentialDeposit", runtime)
        BittensorMevFunds((balance.free - balance.frozen).coerceAtLeast(BigInteger.ZERO), deposit)
    }

    override suspend fun timing(chain: Chain, address: String): BittensorMevTiming = coroutineScope {
        val nonce = async { rpc.getNonce(chain.id, address) }
        val finalized = async {
            val hash = rpc.getFinalizedHead(chain.id)
            hash to rpc.getBlockHeader(chain.id, hash).number
        }
        val best = async { rpc.getBlockHeader(chain.id).number }
        val (hash, birth) = finalized.await()
        check(best.await() - birth <= BittensorMevProtocol.MAX_FINALITY_LAG) { "Finality is too far behind for MEV protection" }
        val era = Era.getEraFromBlockPeriod(birth, BittensorMevProtocol.ERA_BLOCKS)
        BittensorMevTiming(nonce.await(), birth, Mortality(era, hash))
    }
}
