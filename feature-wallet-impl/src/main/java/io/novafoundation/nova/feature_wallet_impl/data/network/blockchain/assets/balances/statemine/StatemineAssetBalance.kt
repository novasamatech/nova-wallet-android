package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLocks
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.updaters.insert
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class StatemineAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val remoteStorage: StorageDataSource,
    private val localStorage: StorageDataSource,
    private val storageCache: StorageCache
) : AssetBalance {

    override suspend fun queryBalanceLocks(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<BalanceLocks?> {
        return flow { emit(null) }
    }

    override suspend fun startSyncingBalanceLocks(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BalanceLocks> {
        return emptyFlow()
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return queryAssetDetails(chainAsset).isSufficient
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return queryAssetDetails(chainAsset).minimumBalance
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val statemineType = chainAsset.requireStatemine()

        val assetAccount = remoteStorage.query(chain.id) {
            runtime.metadata.statemineModule(statemineType).storage("Account").query(
                statemineType.id,
                accountId,
                binding = ::bindAssetAccountOrEmpty
            )
        }

        return assetAccount.balance
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
        val statemineType = chainAsset.requireStatemine()

        val runtime = chainRegistry.getRuntime(chain.id)
        val module = runtime.metadata.statemineModule(statemineType)

        val assetDetailsStorage = module.storage("Asset")
        val assetDetailsKey = assetDetailsStorage.storageKey(runtime, statemineType.id)

        val assetAccountStorage = module.storage("Account")
        val assetAccountKey = assetAccountStorage.storageKey(runtime, statemineType.id, accountId)

        val assetDetailsFlow = subscriptionBuilder.subscribe(assetDetailsKey)
            .onEach { storageCache.insert(it, chain.id) }

        val isFrozenFlow = assetDetailsFlow
            .map {
                val decoded = assetDetailsStorage.decodeValue(it.value, runtime)

                bindAssetDetails(decoded).isFrozen
            }

        return combine(
            subscriptionBuilder.subscribe(assetAccountKey),
            isFrozenFlow
        ) { balanceStorageChange, isAssetFrozen ->
            val assetAccountDecoded = assetAccountStorage.decodeValue(balanceStorageChange.value, runtime)
            val assetAccount = bindAssetAccountOrEmpty(assetAccountDecoded)

            val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, isAssetFrozen, assetAccount)

            balanceStorageChange.block.takeIf { assetChanged }
        }
    }

    private suspend fun queryAssetDetails(chainAsset: Chain.Asset): AssetDetails {
        val statemineType = chainAsset.requireStatemine()

        return localStorage.query(chainAsset.chainId) {
            runtime.metadata.statemineModule(statemineType).storage("Asset").query(statemineType.id, binding = ::bindAssetDetails)
        }
    }

    private fun bindAssetAccountOrEmpty(decoded: Any?): AssetAccount {
        return decoded?.let(::bindAssetAccount) ?: AssetAccount.empty()
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        isAssetFrozen: Boolean,
        assetAccount: AssetAccount
    ) = assetCache.updateAsset(metaId, chainAsset) {
        val frozenBalance = if (isAssetFrozen || assetAccount.isFrozen) {
            assetAccount.balance
        } else {
            BigInteger.ZERO
        }

        it.copy(
            frozenInPlanks = frozenBalance,
            freeInPlanks = assetAccount.balance
        )
    }

    private fun RuntimeMetadata.statemineModule(statemineType: Chain.Asset.Type.Statemine) = module(statemineType.palletNameOrDefault())
}
