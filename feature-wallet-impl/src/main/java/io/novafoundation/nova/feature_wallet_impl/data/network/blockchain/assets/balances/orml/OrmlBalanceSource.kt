package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.currencies
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.filterOwn
import io.novafoundation.nova.runtime.ext.findAssetByOrmlCurrencyId
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class OrmlBalanceSource(
    private val assetCache: AssetCache,
    private val storageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val eventsRepository: EventsRepository,
) : BalanceSource {

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return chainAsset.requireOrml().existentialDeposit
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        val ormlAccountData = storageSource.query(
            chainId = chain.id,
            keyBuilder = { it.ormlBalanceKey(accountId, chainAsset) },
            binding = { scale, runtime -> bindOrmlAccountDataOrEmpty(scale, runtime) }
        )

        return ormlAccountData.free + ormlAccountData.reserved
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
        val runtime = chainRegistry.getRuntime(chain.id)

        return subscriptionBuilder.subscribe(runtime.ormlBalanceKey(accountId, chainAsset))
            .map {
                val ormlAccountData = bindOrmlAccountDataOrEmpty(it.value, runtime)

                val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, ormlAccountData)

                it.block.takeIf { assetChanged }
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
            .mapNotNull { extrinsicWithEvents ->
                val extrinsic = extrinsicWithEvents.extrinsic

                val chainAsset = chain.findAssetByOrmlCurrencyId(runtime, extrinsic.call.arguments["currency_id"])

                chainAsset?.let {
                    TransferExtrinsic(
                        senderId = bindAccountIdentifier(extrinsic.signature!!.accountIdentifier),
                        recipientId = bindAccountIdentifier(extrinsic.call.arguments["dest"]),
                        amountInPlanks = bindNumber(extrinsic.call.arguments["amount"]),
                        hash = extrinsicWithEvents.extrinsicHash,
                        chainAsset = chainAsset,
                        status = extrinsicWithEvents.status()
                    )
                }
            }.filterOwn(accountId)
    }

    private suspend fun updateAssetBalance(
        metaId: Long,
        chainAsset: Chain.Asset,
        ormlAccountData: OrmlAccountData
    ) = assetCache.updateAsset(metaId, chainAsset) {
        it.copy(
            frozenInPlanks = ormlAccountData.frozen,
            freeInPlanks = ormlAccountData.free,
            reservedInPlanks = ormlAccountData.reserved
        )
    }

    private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
        val assets = runtime.metadata.currencies()

        return instanceOf(assets.call("transfer"))
    }

    private fun RuntimeSnapshot.ormlBalanceKey(accountId: AccountId, chainAsset: Chain.Asset): String {
        return metadata.tokens().storage("Accounts").storageKey(this, accountId, chainAsset.ormlCurrencyId(this))
    }

    private fun bindOrmlAccountDataOrEmpty(scale: String?, runtime: RuntimeSnapshot): OrmlAccountData {
        return scale?.let { bindOrmlAccountData(it, runtime) } ?: OrmlAccountData.empty()
    }
}
