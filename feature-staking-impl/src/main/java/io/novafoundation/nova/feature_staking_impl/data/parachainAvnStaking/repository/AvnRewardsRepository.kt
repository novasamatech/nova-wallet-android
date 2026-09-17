package io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.repository

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.data.network.runtime.binding.bindPerbillNumber
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.network.bindings.bindCommissionSetting
import io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.network.bindings.bindGrowthInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainAvnStaking.network.bindings.bindGrowthPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.math.BigInteger

class AvnGrowthSnapshot(
    val totalStakerReward: BigInteger,
    val totalStakeAccumulated: BigInteger,
    val numberOfAccumulations: BigInteger,
)

interface AvnRewardsRepository {
    suspend fun getCommission(chainId: ChainId): Perbill
    suspend fun getGrowthSnapshot(chainId: ChainId): AvnGrowthSnapshot?
}

class RealAvnRewardsRepository(
    private val storageDataSource: StorageDataSource,
) : AvnRewardsRepository {

    override suspend fun getCommission(chainId: ChainId): Perbill {
        val commission = withTimeoutOrNull(3_000) {
            runCatching {
                storageDataSource.query(chainId) {
                    runtime.metadata.parachainStaking().storage("DefaultCollatorCommission").query(binding = ::bindCommissionSetting)
                }
            }.getOrNull()
        }
        return commission?.let { bindPerbillNumber(it.current) } ?: BigDecimal.ZERO
    }

    override suspend fun getGrowthSnapshot(chainId: ChainId): AvnGrowthSnapshot? {
        // Bounded to avoid blocking the staking info screen forever when
        // the growth indexer hasn't seen its first period yet.
        return withTimeoutOrNull(3_000) {
            runCatching {
                storageDataSource.query(chainId) {
                    val period = runtime.metadata.parachainStaking().storage("GrowthPeriod")
                        .query(binding = ::bindGrowthPeriod)
                    val periodIndex = period?.index ?: return@query null

                    if (periodIndex <= BigInteger.ZERO) return@query null
                    val prevIndex = periodIndex - BigInteger.ONE

                    val growth = runtime.metadata.parachainStaking().storage("Growth")
                        .query(prevIndex, binding = ::bindGrowthInfo) ?: return@query null

                    AvnGrowthSnapshot(
                        totalStakerReward = growth.totalStakerReward,
                        totalStakeAccumulated = growth.totalStakeAccumulated,
                        numberOfAccumulations = growth.numberOfAccumulations
                    )
                }
            }.getOrNull()
        }
    }
}
