package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.source

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.assets
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.TransferExtrinsicWithStatus
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.AssetAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.bindAssetAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.bindings.bindAssetDetails
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class StatemineBalanceSource(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
) : BalanceSource {

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash> {
        val statemineType = chainAsset.type
        require(statemineType is Chain.Asset.Type.Statemine)

        val runtime = chainRegistry.getRuntime(chain.id)

        val assetDetailsKey = runtime.metadata.assets().storage("Asset").storageKey(runtime, statemineType.id)
        val assetAccountKey = runtime.metadata.assets().storage("Account").storageKey(runtime, statemineType.id, accountId)

        val isFrozenFlow = subscriptionBuilder.subscribe(assetDetailsKey)
            .map { bindAssetDetails(it.value!!, runtime).isFrozen }

        return combine(
            subscriptionBuilder.subscribe(assetAccountKey),
            isFrozenFlow
        ) { balanceStorageChange, isAssetFrozen ->
            val assetAccount = balanceStorageChange.value?.let { bindAssetAccount(it, runtime) } ?: AssetAccount.empty()

            updateAssetBalance(metaAccount.id, chainAsset, isAssetFrozen, assetAccount)

            balanceStorageChange.block
        }
    }

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsicWithStatus>> {
        // TODO statemine realtime transfer history
        return Result.success(emptyList())
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        isAssetFrozen: Boolean,
        assetAccount: AssetAccount
    ) = assetCache.updateAsset(metaId, chainAsset) {
        if (isAssetFrozen || assetAccount.isFrozen) {
            it.copy(
                miscFrozenInPlanks = assetAccount.balance,
                freeInPlanks = BigInteger.ZERO
            )
        } else {
            it.copy(
                miscFrozenInPlanks = BigInteger.ZERO,
                freeInPlanks = assetAccount.balance
            )
        }
    }
}
