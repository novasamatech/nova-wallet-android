package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.hydrationEvm

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.updateFromChainBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.TransferableBalanceUpdatePoint
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml.OrmlAssetBalance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.currencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.currentRemoteBlockNumberFlow
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.math.BigInteger
import javax.inject.Inject

/**
 * Balance source implementation for Hydration ERC20 tokens that are "mostly exposed" via Orml on Substrate side
 * In particular, we can transact, listen for events, but cannot subscribe storage for balance updates
 * as balance stays in the smart-contract storage on evm-side. Instead, we use runtime api to poll update once a block
 */
@FeatureScope
class HydrationEvmOrmlAssetBalance @Inject constructor(
    private val defaultDelegate: OrmlAssetBalance,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val chainRegistry: ChainRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val assetCache: AssetCache,
    private val rpcCalls: RpcCalls,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        return emptyFlow<Nothing>()
    }

    override fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return defaultDelegate.isSelfSufficient(chainAsset)
    }

    override suspend fun existentialDeposit(chainAsset: Chain.Asset): BigInteger {
        return defaultDelegate.existentialDeposit(chainAsset)
    }

    override suspend fun queryAccountBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): ChainAssetBalance {
        return fetchBalance(chainAsset, accountId)
    }

    override suspend fun subscribeAccountBalanceUpdatePoint(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): Flow<TransferableBalanceUpdatePoint> {
        return balanceUpdateFlow(chainAsset, accountId, subscriptionBuilder = null)
            .mapNotNull { (it.update as? BalanceSyncUpdate.CauseFetchable)?.blockHash }
            .map(::TransferableBalanceUpdatePoint)
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        return balanceUpdateFlow(chainAsset, accountId, subscriptionBuilder).map { (balance, syncUpdate) ->
            assetCache.updateFromChainBalance(metaAccount.id, balance)

            syncUpdate
        }
    }

    private suspend fun balanceUpdateFlow(
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder?
    ): Flow<BalancePollingUpdate> {
        val blockNumberFlow = chainStateRepository.currentRemoteBlockNumberFlow(chainAsset.chainId, subscriptionBuilder)

        return flow {
            val initialBalance = fetchBalance(chainAsset, accountId)
            val initialBalanceUpdate = BalancePollingUpdate(initialBalance, BalanceSyncUpdate.NoCause)
            emit(initialBalanceUpdate)

            var currentBalance = initialBalance

            blockNumberFlow.collect { blockNumber ->
                val newBalance = fetchBalance(chainAsset, accountId)

                if (currentBalance != newBalance) {
                    currentBalance = newBalance

                    val balanceUpdatedAt = rpcCalls.getBlockHash(chainAsset.chainId, blockNumber)
                    val syncUpdate = BalanceSyncUpdate.CauseFetchable(balanceUpdatedAt)
                    val balanceUpdate = BalancePollingUpdate(newBalance, syncUpdate)
                    emit(balanceUpdate)
                }
            }
        }
    }

    private suspend fun fetchBalance(chainAsset: Chain.Asset, accountId: AccountId): ChainAssetBalance {
        return runtimeCallsApi.forChain(chainAsset.chainId).fetchBalance(chainAsset, accountId)
    }

    private suspend fun RuntimeCallsApi.fetchBalance(chainAsset: Chain.Asset, accountId: AccountId): ChainAssetBalance {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)
        val assetId = chainAsset.requireOrml().currencyId(runtime)

        return call(
            section = "CurrenciesApi",
            method = "account",
            arguments = mapOf(
                "asset_id" to assetId,
                "who" to accountId
            ),
            returnBinding = { bindAssetBalance(it, chainAsset) }
        )
    }

    private fun bindAssetBalance(decoded: Any?, chainAsset: Chain.Asset): ChainAssetBalance {
        val asStruct = decoded.castToStruct()

        return ChainAssetBalance(
            chainAsset = chainAsset,
            free = bindNumber(asStruct["free"]),
            frozen = bindNumber(asStruct["frozen"]),
            reserved = bindNumber(asStruct["reserved"]),
            transferableMode = TransferableMode.REGULAR,
            edCountingMode = EDCountingMode.TOTAL
        )
    }

    private data class BalancePollingUpdate(
        val assetBalance: ChainAssetBalance,
        val update: BalanceSyncUpdate
    )
}
