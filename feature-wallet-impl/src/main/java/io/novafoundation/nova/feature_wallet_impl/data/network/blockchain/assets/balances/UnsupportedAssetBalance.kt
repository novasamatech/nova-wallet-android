package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class UnsupportedAssetBalance : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ) = unsupported()

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset) = unsupported()

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset) = unsupported()

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId) = unsupported()

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash> {
        return emptyFlow()
    }

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Unsupported balance source")
}
