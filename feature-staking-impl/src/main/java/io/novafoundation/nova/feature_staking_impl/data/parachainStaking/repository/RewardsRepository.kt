package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbill
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.InflationInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.ParachainBondConfig
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindInflationInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindParachainBondConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage

interface RewardsRepository {

    suspend fun getInflationInfo(chainId: ChainId): InflationInfo

    suspend fun getParachainBondConfig(chainId: ChainId): ParachainBondConfig

    suspend fun getCollatorCommission(chainId: ChainId): Perbill
}

class RealRewardsRepository(
    private val storageDataSource: StorageDataSource,
) : RewardsRepository {
    override suspend fun getInflationInfo(chainId: ChainId): InflationInfo {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("InflationConfig").query(binding = ::bindInflationInfo)
        }
    }

    override suspend fun getParachainBondConfig(chainId: ChainId): ParachainBondConfig {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("ParachainBondInfo").query(binding = ::bindParachainBondConfig)
        }
    }

    override suspend fun getCollatorCommission(chainId: ChainId): Perbill {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("CollatorCommission").query(binding = ::bindPerbill)
        }
    }
}
