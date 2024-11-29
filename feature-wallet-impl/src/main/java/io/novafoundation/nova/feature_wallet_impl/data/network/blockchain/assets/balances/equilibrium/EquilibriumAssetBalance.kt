package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.equilibrium

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.HelperBinding
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getList
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.constantOrNull
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.eqBalances
import io.novafoundation.nova.common.utils.hasUpdated
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.second
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdate
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.bindEquilibriumBalanceLocks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.updateLocks
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.requireEquilibrium
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.binding.number
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class EquilibriumAssetBalance(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val lockDao: LockDao,
    private val assetDao: AssetDao,
    private val remoteStorageSource: StorageDataSource,
) : AssetBalance {

    private class ReservedAssetBalanceWithBlock(val assetId: Int, val reservedBalance: BigInteger, val block: BlockHash)

    private class FreeAssetBalancesWithBlock(val lock: BigInteger?, val assets: List<FreeAssetBalance>)

    private class FreeAssetBalance(val assetId: Int, val balance: BigInteger)

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        if (!chainAsset.isUtilityAsset) return emptyFlow<Unit>()

        val runtime = chainRegistry.getRuntime(chain.id)
        val storage = runtime.metadata.eqBalances().storage("Locked")
        val key = storage.storageKey(runtime, accountId)

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                val balanceLocks = bindEquilibriumBalanceLocks(storage.decodeValue(change.value, runtime)).orEmpty()
                lockDao.updateLocks(balanceLocks, metaAccount.id, chain.id, chainAsset.id)
            }
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return if (chainAsset.isUtilityAsset) {
            remoteStorageSource.query(chain.id) {
                runtime.metadata.eqBalances().constantOrNull("ExistentialDepositBasic")?.getAs(number())
                    .orZero()
            }
        } else {
            BigInteger.ZERO
        }
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): AccountBalance {
        val assetBalances = remoteStorageSource.query(
            chain.id,
            keyBuilder = { it.getAccountStorage().storageKey(it, accountId) },
            binding = { scale, runtimeSnapshot -> bindEquilibriumBalances(chain, scale, runtimeSnapshot) }
        )

        val onChainAssetId = chainAsset.requireEquilibrium().id
        val reservedBalance = remoteStorageSource.query(
            chain.id,
            keyBuilder = { it.getReservedStorage().storageKey(it, accountId, onChainAssetId) },
            binding = { scale, runtimeSnapshot -> bindReservedBalance(scale, runtimeSnapshot) }
        )

        val assetBalance = assetBalances.assets
            .firstOrNull { it.assetId == chainAsset.id }
            ?.balance
            .orZero()

        val lockedBalance = assetBalances.lock.orZero().takeIf { chainAsset.isUtilityAsset } ?: BigInteger.ZERO

        return AccountBalance(
            free = assetBalance,
            reserved = reservedBalance,
            frozen = lockedBalance
        )
    }

    override suspend fun subscribeTransferableAccountBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        sharedSubscriptionBuilder: SharedRequestsBuilder?
    ): Flow<TransferableBalanceUpdate> {
        TODO("Not yet implemented")
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val accountBalance = queryAccountBalance(chain, chainAsset, accountId)
        return accountBalance.free + accountBalance.reserved
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

        var oldBlockHash: String? = null

        return combine(assetBalancesFlow, reservedBalanceFlow) { (blockHash, assetBalances), reservedBalancesWithBlocks ->
            val freeByAssetId = assetBalances.assets.associateBy { it.assetId }
            val reservedByAssetId = reservedBalancesWithBlocks.associateBy { it.assetId }

            val diff = assetCache.updateAssetsByChain(metaAccount, chain) { asset: Chain.Asset ->
                val free = freeByAssetId[asset.id]?.balance.orZero()
                val reserved = reservedByAssetId[asset.id]?.reservedBalance.orZero()
                val locks = if (asset.isUtilityAsset) assetBalances.lock.orZero() else BigInteger.ZERO
                AssetLocal(
                    assetId = asset.id,
                    chainId = asset.chainId,
                    metaId = metaAccount.id,
                    freeInPlanks = free,
                    reservedInPlanks = reserved,
                    frozenInPlanks = locks,
                    transferableMode = TransferableModeLocal.REGULAR,
                    edCountingMode = EDCountingModeLocal.TOTAL,
                    redeemableInPlanks = BigInteger.ZERO,
                    bondedInPlanks = BigInteger.ZERO,
                    unbondingInPlanks = BigInteger.ZERO
                )
            }

            if (diff.hasUpdated() && oldBlockHash != blockHash) {
                oldBlockHash = blockHash
                BalanceSyncUpdate.CauseFetchable(blockHash)
            } else {
                BalanceSyncUpdate.NoCause
            }
        }
    }

    private suspend fun SharedRequestsBuilder.subscribeOnSystemAccount(chain: Chain, accountId: AccountId): Flow<Pair<BlockHash, FreeAssetBalancesWithBlock>> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            runtime.getAccountStorage().storageKey(runtime, accountId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow()
        }

        return subscribe(key)
            .map { it.block to bindEquilibriumBalances(chain, it.value, runtime) }
    }

    private suspend fun SharedRequestsBuilder.subscribeOnReservedBalance(chain: Chain, accountId: AccountId): Flow<List<ReservedAssetBalanceWithBlock>> {
        val runtime = chainRegistry.getRuntime(chain.id)

        return chain.assets
            .filter { it.type is Chain.Asset.Type.Equilibrium }
            .map { asset ->
                val equilibriumType = asset.requireEquilibrium()

                val key = try {
                    runtime.getReservedStorage().storageKey(runtime, accountId, equilibriumType.id)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Failed to construct key: ${e.message} in ${chain.name}")

                    return@map flowOf(null)
                }

                subscribe(key)
                    .map { ReservedAssetBalanceWithBlock(asset.id, bindReservedBalance(it.value, runtime), it.block) }
                    .catch<ReservedAssetBalanceWithBlock?> { emit(null) }
            }.combine()
            .map { it.filterNotNull() }
    }

    private fun bindReservedBalance(raw: String?, runtime: RuntimeSnapshot): BigInteger {
        val type = runtime.getReservedStorage().returnType()

        return raw?.let { type.fromHexOrNull(runtime, it).cast<BigInteger>() } ?: BigInteger.ZERO
    }

    @UseCaseBinding
    private fun bindEquilibriumBalances(chain: Chain, scale: String?, runtime: RuntimeSnapshot): FreeAssetBalancesWithBlock {
        if (scale == null) {
            return FreeAssetBalancesWithBlock(null, emptyList())
        }

        val type = runtime.getAccountStorage().returnType()
        val data = type.fromHexOrNull(runtime, scale)
            .castToStruct()
            .get<Any>("data").castToDictEnum()
            .value
            .castToStruct()

        val lock = data.get<BigInteger>("lock")
        val balances = data.getList("balance")

        val onChainAssetIdToAsset = chain.assets
            .associateBy { it.requireEquilibrium().id }

        val assetBalances = balances.mapNotNull { assetBalance ->
            val (onChainAssetId, balance) = bindAssetBalance(assetBalance.castToList())
            val asset = onChainAssetIdToAsset[onChainAssetId]

            asset?.let { FreeAssetBalance(it.id, balance) }
        }

        return FreeAssetBalancesWithBlock(lock, assetBalances)
    }

    @HelperBinding
    private fun bindAssetBalance(dynamicInstance: List<Any?>): Pair<BigInteger, BigInteger> {
        val onChainAssetId = bindNumber(dynamicInstance.first())
        val balance = dynamicInstance.second().castToDictEnum()
        val amount = if (balance.name == "Positive") {
            bindNumber(balance.value)
        } else {
            BigInteger.ZERO
        }
        return onChainAssetId to amount
    }

    private fun RuntimeSnapshot.getAccountStorage(): StorageEntry {
        return metadata.system().storage("Account")
    }

    private fun RuntimeSnapshot.getReservedStorage(): StorageEntry {
        return metadata.eqBalances().storage("Reserved")
    }
}
