package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
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

    /**
     * Derives pool accounts with poolId from range 1..[numberOfPools] (end-inclusive)
     */
    suspend fun derivePoolAccountsRange(numberOfPools: Int, derivationType: PoolAccountType, chainId: ChainId): Map<PoolId, AccountIdKey>
}

suspend fun PoolAccountDerivation.bondedAccountOf(poolId: PoolId, chainId: ChainId): AccountId {
    return derivePoolAccount(poolId, PoolAccountType.BONDED, chainId)
}

suspend fun PoolAccountDerivation.deriveAllBondedPools(lastPoolId: PoolId, chainId: ChainId): Map<PoolId, AccountIdKey> {
    return derivePoolAccountsRange(numberOfPools = lastPoolId.value.toInt(), PoolAccountType.BONDED, chainId)
}

private const val PREFIX = "modl"

class RealPoolAccountDerivation(
    private val localDataSource: StorageDataSource,
) : PoolAccountDerivation {

    override suspend fun derivePoolAccount(poolId: PoolId, derivationType: PoolAccountType, chainId: ChainId): AccountId {
        val prefixBytes = PREFIX.encodeToByteArray()
        val palletId = palletId(chainId)
        val poolIdBytes = uint32.toByteArray(poolId.value.toInt().toUInt())

        return (prefixBytes + palletId + derivationType.derivationIndex + poolIdBytes).truncateToAccountId()
    }

    override suspend fun derivePoolAccountsRange(numberOfPools: Int, derivationType: PoolAccountType, chainId: ChainId): Map<PoolId, AccountIdKey> {
        val prefixBytes = PREFIX.encodeToByteArray()
        val palletId = palletId(chainId)
        val derivationTypeIndex = derivationType.derivationIndex
        val commonPrefix = prefixBytes + palletId + derivationTypeIndex

        return (1..numberOfPools).associateBy(
            keySelector = { PoolId(it.toBigInteger()) },
            valueTransform = { poolId ->
                val poolIdBytes = uint32.toByteArray(poolId.toUInt())

                (commonPrefix + poolIdBytes).truncateToAccountId().intoKey()
            }
        )
    }

    private fun ByteArray.truncateToAccountId(): AccountId = copyOf(newSize = 32)

    private suspend fun palletId(chainId: ChainId): ByteArray {
        return localDataSource.query(chainId) {
            metadata.nominationPools().constant("PalletId").value
        }
    }

    private val PoolAccountType.derivationIndex: Byte
        get() = when (this) {
            PoolAccountType.BONDED -> 0
            PoolAccountType.REWARD -> 1
        }
}
