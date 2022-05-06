package io.novafoundation.nova.feature_staking_impl.data.common.repository

import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindTotalInsurance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import java.math.BigInteger

interface CommonStakingRepository {

    suspend fun getTotalIssuance(chainId: ChainId): BigInteger
}

class RealCommonStakingRepository(
    private val storageDataSource: StorageDataSource
) : CommonStakingRepository {

    override suspend fun getTotalIssuance(chainId: ChainId): BigInteger = storageDataSource.queryNonNull(
        keyBuilder = { it.metadata.balances().storage("TotalIssuance").storageKey() },
        binding = ::bindTotalInsurance,
        chainId = chainId
    )
}
