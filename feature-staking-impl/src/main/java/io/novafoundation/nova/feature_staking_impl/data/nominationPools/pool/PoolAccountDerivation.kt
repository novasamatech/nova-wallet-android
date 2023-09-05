package io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.nominationPools
import io.novafoundation.nova.common.utils.startsWith
import io.novafoundation.nova.common.utils.toByteArray
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation.PoolAccountType
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.scale.dataType.uint32

private const val PREFIX = "modl"

class RealPoolAccountDerivation(
    private val localDataSource: StorageDataSource,
) : PoolAccountDerivation {

    override suspend fun derivePoolAccount(poolId: PoolId, derivationType: PoolAccountType, chainId: ChainId): AccountId {
        val commonPrefix = poolAccountPrefix(derivationType, chainId)
        val poolIdBytes = uint32.toByteArray(poolId.value.toInt().toUInt())

        return (commonPrefix + poolIdBytes).truncateToAccountId()
    }

    override suspend fun derivePoolAccountsRange(numberOfPools: Int, derivationType: PoolAccountType, chainId: ChainId): Map<PoolId, AccountIdKey> {
        val commonPrefix = poolAccountPrefix(derivationType, chainId)

        return (1..numberOfPools).associateBy(
            keySelector = { PoolId(it.toBigInteger()) },
            valueTransform = { poolId ->
                val poolIdBytes = uint32.toByteArray(poolId.toUInt())

                (commonPrefix + poolIdBytes).truncateToAccountId().intoKey()
            }
        )
    }

    override suspend fun poolAccountFilter(derivationType: PoolAccountType, chainId: ChainId): Filter<AccountId> {
        val poolAccountPrefix = poolAccountPrefix(derivationType, chainId)

        return IsPoolAccountFilter(poolAccountPrefix)
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

    private suspend fun poolAccountPrefix(derivationType: PoolAccountType, chainId: ChainId): ByteArray {
        val prefixBytes = PREFIX.encodeToByteArray()
        val palletId = palletId(chainId)
        val derivationTypeIndex = derivationType.derivationIndex

        return prefixBytes + palletId + derivationTypeIndex
    }
}

private class IsPoolAccountFilter(private val prefix: ByteArray): Filter<AccountId> {

    override fun shouldInclude(model: AccountId): Boolean {
       return model.startsWith(prefix)
    }
}
