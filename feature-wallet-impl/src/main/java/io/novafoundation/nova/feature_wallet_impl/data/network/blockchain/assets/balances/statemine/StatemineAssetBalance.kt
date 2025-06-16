package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.StatemineAssetDetails
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.transfersFrozen
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.bindAssetAccountOrEmpty
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.statemineModule
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

// TODO Migrate low-level subscription to storage to AssetsApi
class StatemineAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val remoteStorage: StorageDataSource,
    private val statemineAssetsRepository: StatemineAssetsRepository,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<List<BalanceLock>> {
        return emptyFlow()
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return chainAsset.requireStatemine().isSufficient
    }

    override suspend fun existentialDeposit(chainAsset: Chain.Asset): BigInteger {
        return queryAssetDetails(chainAsset).minimumBalance
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): ChainAssetBalance {
        val statemineType = chainAsset.requireStatemine()

        val assetAccount = remoteStorage.query(chain.id) {
            val encodableId = statemineType.prepareIdForEncoding(runtime)

            runtime.metadata.statemineModule(statemineType).storage("Account").query(
                encodableId,
                accountId,
                binding = ::bindAssetAccountOrEmpty
            )
        }

        val accountBalance = assetAccount.toAccountBalance()
        return ChainAssetBalance.default(accountBalance)
    }

    override suspend fun subscribeTransferableAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        sharedSubscriptionBuilder: SharedRequestsBuilder?
    ): Flow<TransferableBalanceUpdate> {
        val statemineType = chainAsset.requireStatemine()

        return remoteStorage.subscribe(chain.id, sharedSubscriptionBuilder) {
            val encodableId = statemineType.prepareIdForEncoding(runtime)

            runtime.metadata.statemineModule(statemineType).storage("Account").observeWithRaw(
                encodableId,
                accountId,
                binding = ::bindAssetAccountOrEmpty
            ).map {
                val transferable = it.value.transferableBalance()
                TransferableBalanceUpdate(transferable, updatedAt = it.at)
            }
        }
    }

    private fun AssetAccount.transferableBalance(): Balance {
        return TransferableMode.REGULAR.calculateTransferable(toAccountBalance())
    }

    private fun AssetAccount.toAccountBalance(): AccountBalance {
        val frozenBalance = if (isBalanceFrozen) {
            balance
        } else {
            BigInteger.ZERO
        }

        return AccountBalance(
            free = balance,
            reserved = BigInteger.ZERO,
            frozen = frozenBalance
        )
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val statemineType = chainAsset.requireStatemine()
        val encodableAssetId = statemineType.prepareIdForEncoding(runtime)

        val module = runtime.metadata.statemineModule(statemineType)

        val assetAccountStorage = module.storage("Account")
        val assetAccountKey = assetAccountStorage.storageKey(runtime, encodableAssetId, accountId)

        val assetDetailsFlow = statemineAssetsRepository.subscribeAndSyncAssetDetails(chain.id, statemineType, subscriptionBuilder)

        return combine(
            subscriptionBuilder.subscribe(assetAccountKey),
            assetDetailsFlow.map { it.status.transfersFrozen }
        ) { balanceStorageChange, isAssetFrozen ->
            val assetAccountDecoded = assetAccountStorage.decodeValue(balanceStorageChange.value, runtime)
            val assetAccount = bindAssetAccountOrEmpty(assetAccountDecoded)

            val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, isAssetFrozen, assetAccount)

            if (assetChanged) {
                BalanceSyncUpdate.CauseFetchable(balanceStorageChange.block)
            } else {
                BalanceSyncUpdate.NoCause
            }
        }
    }

    private suspend fun queryAssetDetails(chainAsset: Chain.Asset): StatemineAssetDetails {
        val statemineType = chainAsset.requireStatemine()
        return statemineAssetsRepository.getAssetDetails(chainAsset.chainId, statemineType)
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        isAssetFrozen: Boolean,
        assetAccount: AssetAccount
    ) = assetCache.updateAsset(metaId, chainAsset) {
        val frozenBalance = if (isAssetFrozen || assetAccount.isBalanceFrozen) {
            assetAccount.balance
        } else {
            BigInteger.ZERO
        }
        val freeBalance = assetAccount.balance

        it.copy(
            frozenInPlanks = frozenBalance,
            freeInPlanks = freeBalance,
            transferableMode = TransferableModeLocal.REGULAR,
            edCountingMode = EDCountingModeLocal.TOTAL,
        )
    }
}
