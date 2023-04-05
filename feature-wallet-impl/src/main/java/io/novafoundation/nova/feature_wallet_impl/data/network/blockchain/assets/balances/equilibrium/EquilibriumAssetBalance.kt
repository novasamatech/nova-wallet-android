package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.equilibrium

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.getStruct
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.updateNonLockableAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BlockchainLock
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.updateLock
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.requireEquilibrium
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map


private const val LOCK_ID = "locks"

class EquilibriumAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val substrateRemoteSource: SubstrateRemoteSource,
    private val lockDao: LockDao,
    private val assetDao: AssetDao
) : AssetBalance {

    private class BlockWithData<T>(val block: BlockHash, val data: T)

    private class ReservedAssetBalance(val assetId: Int, val reservedBalance: BigInteger)

    private class FreeAssetBalances(val assetId: Int, val balance: BigInteger)

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        if (!chainAsset.isUtilityAsset) return emptyFlow<Unit>()

        val locks = BlockchainLock(LOCK_ID, assetBalances.locks)
        lockDao.updateLock(locks, metaAccount.id, chain.id, chainAsset.id)

        return emptyFlow<Unit>()
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        TODO("Not yet implemented")
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        TODO("Not yet implemented")
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        if (!chainAsset.isUtilityAsset) return emptyFlow()

        val assetBalancesFlow = subscriptionBuilder.subscribeOnSystemAccount(chain, accountId)
        val reservedBalanceFlow = subscriptionBuilder.subscribeOnReservedBalance(chain, accountId)

        return combine(assetBalancesFlow, reservedBalanceFlow) { assetBalancesWithBlock, reservedBalancesWithBlocks ->
            val oldAssetsLocal = assetCache.getAssetsInChain(metaAccount.id, chain.id)

            val assetBalanceBlock = assetBalancesWithBlock.block
            val reservedBlocks = reservedBalancesWithBlocks.map { it.block }
            
            val assetBalances = assetBalancesWithBlock.data
            val reservedBalances = reservedBalancesWithBlocks.map { it.data }

            val transferableByAssetId = assetBalances.associateBy { it.assetId }
            val reservedByAssetId = reservedBalances.associateBy { it.assetId }

            val newAssetsLocal = chain.assets.map {
                AssetLocal(
                    it.id,
                    it.chainId,
                    metaAccount.id,
                    freeInPlanks = transferableByAssetId[it.id]?.balance ?: BigInteger.ZERO,
                    reservedInPlanks = reservedByAssetId[it.id]?.reservedBalance ?: BigInteger.ZERO,
                    frozenInPlanks = BigInteger.ZERO,
                    redeemableInPlanks = BigInteger.ZERO,
                    bondedInPlanks = BigInteger.ZERO,
                    unbondingInPlanks = BigInteger.ZERO
                )
            }

            val diff = CollectionDiffer.findDiff(newAssetsLocal, oldAssetsLocal, forceUseNewItems = false)
            assetDao.insertAssets(diff.newOrUpdated)

            if (diff.hasDifference) {
                BalanceSyncUpdate.CauseFetchable(change.block)
            } else {
                BalanceSyncUpdate.NoCause
            }
        }
    }

    private suspend fun SharedRequestsBuilder.subscribeOnSystemAccount(chain: Chain, accountId: AccountId): Flow<BlockWithData<List<FreeAssetBalances>>> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            runtime.metadata.system().storage("Account").storageKey(runtime, accountId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow()
        }

        return subscribe(key)
            .map {
                val balances = bindEquilibriumBalances(chain, it.value, runtime)
                BlockWithData(it.block, balances)
            }
    }


    private suspend fun SharedRequestsBuilder.subscribeOnReservedBalance(chain: Chain, accountId: AccountId): Flow<List<BlockWithData<ReservedAssetBalance>>> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val storage = runtime.metadata
            .module("EqBalances")
            .storage("Reserved")

        return chain.assets
            .map { asset ->
                val equilibriumType = asset.requireEquilibrium()

                val key = try {
                    storage.storageKey(runtime, accountId, equilibriumType.id)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")
                    return emptyFlow()
                }

                subscribe(key)
                    .map {
                        val reservedBalance = ReservedAssetBalance(asset.id, bindReservedBalance(it.value, runtime, storage))
                        BlockWithData(it.block, reservedBalance)
                    }
            }.combine()
    }

    private fun bindReservedBalance(raw: String?, runtime: RuntimeSnapshot, storage: StorageEntry): BigInteger {
        val type = storage.returnType()

        return raw?.let { type.fromHexOrNull(runtime, it).cast<BigInteger>() } ?: BigInteger.ZERO
    }

    @UseCaseBinding
    private fun bindEquilibriumBalances(chain: Chain, scale: String?, runtime: RuntimeSnapshot): List<FreeAssetBalances> {
        if (scale == null) return emptyList()

        val type = runtime.metadata.system().storage("Account").returnType()

        val v0Data = type.fromHexOrNull(runtime, scale)
            .cast<Struct.Instance>()
            .getStruct("data")
            .getStruct("V0")

        val balances = v0Data.getList("balance")

        val onChainAssetIdToAsset = chain.assets
            .associateBy { it.requireEquilibrium().id }

        return balances.mapNotNull { assetBalance ->
            val (onChainAssetId, balance) = bindAccountData(assetBalance.castToList())
            val asset = onChainAssetIdToAsset[onChainAssetId]

            asset?.let { FreeAssetBalances(it.id, balance) }
        }
    }

    @HelperBinding
    private fun bindAccountData(dynamicInstance: List<Any?>): Pair<BigInteger, BigInteger> {
        val onChainAssetId = dynamicInstance.first().cast<BigInteger>()
        val balance = dynamicInstance.second().castToStruct()
        val positiveBalance = balance.get<BigInteger>("Positive") ?: BigInteger.ZERO

        return onChainAssetId to positiveBalance
    }
}
