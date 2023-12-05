package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.evmNative

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.updateNonLockableAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.AssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.BalanceSyncUpdate
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getCallEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.getSubscriptionEthereumApiOrThrow
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

class EvmNativeAssetBalance(
    private val assetCache: AssetCache,
    private val chainRegistry: ChainRegistry,
) : AssetBalance {

    override suspend fun startSyncingBalanceLocks(
        metaAccount: MetaAccount,
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<*> {
        // Evm native tokens doe not support locks
        return emptyFlow<Nothing>()
    }

    override suspend fun isSelfSufficient(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        // Evm native tokens do not have ED
        return BigInteger.ZERO
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val ethereumApi = chainRegistry.getCallEthereumApiOrThrow(chain.id)

        return ethereumApi.getLatestNativeBalance(chain.addressOf(accountId))
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BalanceSyncUpdate> {
        val subscriptionApi = chainRegistry.getSubscriptionEthereumApiOrThrow(chain.id)
        val callApi = chainRegistry.getCallEthereumApiOrThrow(chain.id)

        val address = chain.addressOf(accountId)

        return balanceSyncUpdateFlow(address, subscriptionApi, callApi).map { balanceUpdate ->
            assetCache.updateNonLockableAsset(metaAccount.id, chainAsset, balanceUpdate.newBalance)

            balanceUpdate.syncUpdate
        }
    }

    private fun balanceSyncUpdateFlow(
        address: String,
        subscriptionApi: Web3Api,
        callApi: Web3Api
    ): Flow<EvmNativeBalanceUpdate> {
        return flow {
            val initialBalance = callApi.getLatestNativeBalance(address)
            emit(EvmNativeBalanceUpdate(initialBalance, BalanceSyncUpdate.NoCause))

            var currentBalance = initialBalance

            val realtimeUpdates = subscriptionApi.newHeadsFlow().transform { newHead ->
                val blockHash = newHead.params.result.hash
                val newBalance = callApi.getLatestNativeBalance(address)

                if (newBalance != currentBalance) {
                    currentBalance = newBalance
                    val update = EvmNativeBalanceUpdate(newBalance, BalanceSyncUpdate.CauseFetchable(blockHash))
                    emit(update)
                }
            }

            emitAll(realtimeUpdates)
        }
    }

    private suspend fun Web3Api.getLatestNativeBalance(address: String): Balance {
        return ethGetBalance(address, DefaultBlockParameterName.LATEST).sendSuspend().balance
    }
}

private class EvmNativeBalanceUpdate(
    val newBalance: Balance,
    val syncUpdate: BalanceSyncUpdate,
)
