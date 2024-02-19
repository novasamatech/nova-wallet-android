package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.vesting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage

interface TuringStakingRewardsRepository {

    suspend fun additionalIssuance(chainId: ChainId): Balance
}

class RealTuringStakingRewardsRepository(
    private val localStorage: StorageDataSource
) : TuringStakingRewardsRepository {

    override suspend fun additionalIssuance(chainId: ChainId): Balance {
        return localStorage.query(chainId) {
            runtime.metadata.vesting().storage("TotalUnvestedAllocation").query(binding = ::bindNumber)
        }
    }
}
