package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.orml

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class OrmlBalanceSource(
    private val assetCache: AssetCache,
    private val chainRegistry: ChainRegistry,
) : BalanceSource {

    override suspend fun existentialDeposit(chain: Chain, chainAsset: Chain.Asset): BigInteger {
        return BigInteger.ZERO // TODO
    }

    override suspend fun queryTotalBalance(chain: Chain, chainAsset: Chain.Asset, accountId: AccountId): BigInteger {
        return BigInteger.ZERO // TODO
    }

    override suspend fun startSyncingBalance(
        chain: Chain,
        chainAsset: Chain.Asset,
        metaAccount: MetaAccount,
        accountId: AccountId,
        subscriptionBuilder: SubscriptionBuilder
    ): Flow<BlockHash?> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val key = runtime.metadata.tokens().storage("Accounts").storageKey(runtime, accountId, chainAsset.currencyId(runtime))

        return subscriptionBuilder.subscribe(key)
            .map {
                val ormlAccountData = bindOrmlAccountDataOrEmpty(it.value, runtime)

                val assetChanged = updateAssetBalance(metaAccount.id, chainAsset, ormlAccountData)

                it.block.takeIf { assetChanged }
            }
    }

    override suspend fun fetchOperationsForBalanceChange(chain: Chain, blockHash: String, accountId: AccountId): Result<List<TransferExtrinsic>> {
        return Result.success(emptyList()) // TODO
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

    private fun bindOrmlAccountDataOrEmpty(scale: String?, runtime: RuntimeSnapshot): OrmlAccountData {
        return scale?.let { bindOrmlAccountData(it, runtime) } ?: OrmlAccountData.empty()
    }

    private fun Chain.Asset.currencyId(runtime: RuntimeSnapshot): Any? {
        val ormlConfig = requireOrml()

        val currencyIdType = runtime.typeRegistry[ormlConfig.currencyIdType]
            ?: error("Cannot find type ${ormlConfig.currencyIdType}")

        return currencyIdType.fromHex(runtime, ormlConfig.currencyIdScale)
    }
}
