package io.novafoundation.nova.feature_assets.domain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLocks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface BalanceLocksInteractor {

    fun balanceLocksFlow(chainId: ChainId, chainAssetId: Int): Flow<BalanceLocks?>

    fun runBalanceLocksUpdate(): Flow<Updater.SideEffect>
}
