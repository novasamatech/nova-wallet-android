package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.RealtimeHistoryUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
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

    suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean

    suspend fun existentialDeposit(
        chain: Chain,
        chainAsset: Chain.Asset
    ): BigInteger

    suspend fun queryAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): AccountBalance

    suspend fun subscribeTransferableAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        sharedSubscriptionBuilder: SharedRequestsBuilder,
    ): Flow<Balance>

    suspend fun queryTotalBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): BigInteger

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
