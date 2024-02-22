package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import java.math.BigInteger

interface TotalIssuanceRepository {

    suspend fun getTotalIssuance(chainId: ChainId): BigInteger

    suspend fun getInactiveIssuance(chainId: ChainId): BigInteger
}

suspend fun TotalIssuanceRepository.getActiveIssuance(chainId: ChainId): BigInteger {
    return (getTotalIssuance(chainId) - getInactiveIssuance(chainId)).coerceAtLeast(BigInteger.ZERO)
}

internal class RealTotalIssuanceRepository(
    private val storageDataSource: StorageDataSource
) : TotalIssuanceRepository {

    override suspend fun getTotalIssuance(chainId: ChainId): BigInteger {
        return storageDataSource.query(chainId) {
            runtime.metadata.balances().storage("TotalIssuance").query(binding = ::bindNumber)
        }
    }

    override suspend fun getInactiveIssuance(chainId: ChainId): BigInteger {
        return storageDataSource.query(chainId) {
            runtime.metadata.balances().storageOrNull("InactiveIssuance")?.query(binding = ::bindNumberOrZero).orZero()
        }
    }
}
