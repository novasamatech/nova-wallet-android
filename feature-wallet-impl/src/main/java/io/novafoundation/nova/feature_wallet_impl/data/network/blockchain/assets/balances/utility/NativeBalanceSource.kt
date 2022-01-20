package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.utility

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.oneOf
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.bindAccountInfoOrDefault
import io.novafoundation.nova.feature_wallet_api.data.cache.updateAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.filterOwn
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class NativeBalanceSource(
    private val chainRegistry: ChainRegistry,
    private val assetCache: AssetCache,
    private val eventsRepository: EventsRepository,
    private val substrateRemoteSource: SubstrateRemoteSource,
) : BalanceSource {

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        val runtime = chainRegistry.getRuntime(chain.id)

        return runtime.metadata.balances().numberConstant("ExistentialDeposit", runtime)
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val accountInfo = substrateRemoteSource.getAccountInfo(chain.id, accountId)

        return accountInfo.data.free + accountInfo.data.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = try {
            runtime.metadata.system().storage("Account").storageKey(runtime, accountId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to construct account storage key: ${e.message} in ${chain.name}")

            return emptyFlow()
        }

        return subscriptionBuilder.subscribe(key)
            .map { change ->
                runCatching { bindAccountInfoOrDefault(change.value, runtime) }
                    .onFailure { Log.e(LOG_TAG, "Failed to update balance in ${chain.name}") }
                    .onSuccess {
                        assetCache.updateAsset(metaAccount.id, chain.utilityAsset, it)
                    }

                change.block
            }
    }

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> = runCatching {
        val runtime = chainRegistry.getRuntime(chain.id)
        val extrinsicsWithEvents = eventsRepository.getExtrinsicsWithEvents(chain.id, blockHash)

        extrinsicsWithEvents.filter { it.extrinsic.call.isTransfer(runtime) }
            .map {
                val extrinsic = it.extrinsic

                TransferExtrinsic(
                    senderId = bindAccountIdentifier(extrinsic.signature!!.accountIdentifier),
                    recipientId = bindAccountIdentifier(extrinsic.call.arguments["dest"]),
                    amountInPlanks = bindNumber(extrinsic.call.arguments["value"]),
                    hash = it.extrinsicHash,
                    chainAsset = chain.utilityAsset,
                    status = it.status()
                )
            }.filterOwn(accountId)
    }

    private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
        val balances = runtime.metadata.balances()

        return oneOf(
            balances.call("transfer"),
            balances.call("transfer_keep_alive")
        )
    }
}
