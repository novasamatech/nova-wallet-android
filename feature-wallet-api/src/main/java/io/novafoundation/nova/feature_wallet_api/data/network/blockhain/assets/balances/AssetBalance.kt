package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdatePoint
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.math.BigInteger

sealed class BalanceSyncUpdate {

    class CauseFetchable(val blockHash: BlockHash) : BalanceSyncUpdate()

    class CauseFetched(val cause: RealtimeHistoryUpdate) : BalanceSyncUpdate()

    object NoCause : BalanceSyncUpdate()
}

interface AssetBalance {

    suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*>

    suspend fun startSyncingBalanceHolds(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> = emptyFlow<Nothing>()

    fun isSelfSufficient(chainAsset: Chain.Asset): Boolean

    suspend fun existentialDeposit(chainAsset: Chain.Asset): BigInteger

    suspend fun queryAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): ChainAssetBalance

    suspend fun subscribeAccountBalanceUpdatePoint(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
    ): Flow<TransferableBalanceUpdatePoint>

    /**
     * @return emits hash of the blocks where changes occurred. If no change were detected based on the upstream event - should emit null
     */
    suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate>
}

suspend fun AssetBalance.queryAccountBalanceCatching(
    chain: Chain,
    chainAsset: Chain.Asset,
    accountId: AccountId
): Result<ChainAssetBalance> {
    return runCatching { queryAccountBalance(chain, chainAsset, accountId) }
}
