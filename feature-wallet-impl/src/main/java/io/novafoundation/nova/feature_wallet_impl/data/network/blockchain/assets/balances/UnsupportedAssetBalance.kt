package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdatePoint
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class UnsupportedAssetBalance : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ) = unsupported()

    override fun isSelfSufficient(chainAsset: Chain.Asset) = unsupported()

    override suspend fun existentialDeposit(chainAsset: Chain.Asset) = unsupported()

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId) = unsupported()
    override suspend fun subscribeAccountBalanceUpdatePoint(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
    ): Flow<TransferableBalanceUpdatePoint> = unsupported()

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        return emptyFlow()
    }

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Unsupported balance source")
}
