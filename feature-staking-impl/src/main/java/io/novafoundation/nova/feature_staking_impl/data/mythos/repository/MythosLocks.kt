package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythosStakingFreezeIds
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class MythosLocks(
    val releasing: Balance,
    val staked: Balance
)

val MythosLocks.total: Balance
    get() = releasing + staked

fun BalanceLocksRepository.observeMythosLocks(metaId: Long, chain: Chain, chainAsset: Chain.Asset): Flow<MythosLocks> {
    return observeBalanceLocks(metaId, chain, chainAsset).map { locks ->
        MythosLocks(
            releasing = locks.findAmountOrZero(MythosStakingFreezeIds.RELEASING),
            staked = locks.findAmountOrZero(MythosStakingFreezeIds.STAKING)
        )
    }.distinctUntilChanged()
}

private fun List<BalanceLock>.findAmountOrZero(id: BalanceLockId): Balance {
    return findById(id)?.amountInPlanks.orZero()
}
