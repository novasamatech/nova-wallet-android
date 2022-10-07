package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

interface TotalIssuanceRepository {

    suspend fun getTotalIssuance(chainId: ChainId): BigInteger
}

internal class RealTotalIssuanceRepository(
    private val storageDataSource: StorageDataSource
) : TotalIssuanceRepository {

    override suspend fun getTotalIssuance(chainId: ChainId): BigInteger {
        return storageDataSource.query(chainId) {
            runtime.metadata.balances().storage("TotalIssuance").query(binding = ::bindNumber)
        }
    }
}
