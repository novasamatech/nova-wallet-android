package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface BalanceSource {

    suspend fun existentialDeposit(
        chain: Chain,
        chainAsset: Chain.Asset
    ): BigInteger

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
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?>

    suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>>
}
