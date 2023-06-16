package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.common.utils.toByteArray
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation.PoolAccountType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.scale.dataType.uint32

interface PoolAccountDerivation {

    enum class PoolAccountType {

        BONDED, REWARD
    }

    suspend fun derivePoolAccount(poolId: PoolId, derivationType: PoolAccountType, chainId: ChainId): AccountId
}

private const val PREFIX = "modl"

class RealPoolAccountDerivation(
    private val localDataSource: StorageDataSource,
) : PoolAccountDerivation {

    override suspend fun derivePoolAccount(poolId: PoolId, derivationType: PoolAccountType, chainId: ChainId): AccountId {
        val prefixBytes = PREFIX.encodeToByteArray()
        val palletId = localDataSource.query(chainId) {
            metadata.nominationPools().constant("PalletId").value
        }
        val poolIdBytes = uint32.toByteArray(poolId.value.toInt().toUInt())

        return (prefixBytes + palletId + derivationType.derivationIndex + poolIdBytes).copyOf(newSize = 32)
    }

    private val PoolAccountType.derivationIndex: Byte
        get() = when (this) {
            PoolAccountType.BONDED -> 0
            PoolAccountType.REWARD -> 1
        }
}
