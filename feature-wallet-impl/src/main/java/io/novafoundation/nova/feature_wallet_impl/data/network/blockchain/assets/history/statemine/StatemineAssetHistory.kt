package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.statemine

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.oneOf
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.filterOwn
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.planksToFiatOrNull
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.SubstrateAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.module

class StatemineAssetHistory(
    private val chainRegistry: ChainRegistry,
    private val eventsRepository: EventsRepository,
    private val walletRepository: WalletRepository,
    walletOperationsApi: SubQueryOperationsApi,
    cursorStorage: TransferCursorStorage,
    coinPriceRepository: CoinPriceRepository
) : SubstrateAssetHistory(walletOperationsApi, cursorStorage, coinPriceRepository) {

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId,
        currency: Currency
    ): Result<List<TransferExtrinsic>> = runCatching {
        val runtime = chainRegistry.getRuntime(chain.id)
        val extrinsicsWithEvents = eventsRepository.getExtrinsicsWithEvents(chain.id, blockHash)

        val token = walletRepository.getAsset(accountId, chainAsset)?.token

        extrinsicsWithEvents.filter { it.extrinsic.call.isTransfer(runtime, chainAsset) }
            .map { extrinsicWithEvents ->
                val extrinsic = extrinsicWithEvents.extrinsic

                val amount = bindNumber(extrinsic.call.arguments["amount"])
                TransferExtrinsic(
                    senderId = bindAccountIdentifier(extrinsic.signature!!.accountIdentifier),
                    recipientId = bindAccountIdentifier(extrinsic.call.arguments["target"]),
                    amountInPlanks = amount,
                    fiatAmount = token?.planksToFiatOrNull(amount),
                    hash = extrinsicWithEvents.extrinsicHash,
                    chainAsset = chainAsset,
                    status = extrinsicWithEvents.status()
                )
            }.filterOwn(accountId)
    }

    override fun availableOperationFilters(asset: Chain.Asset): Set<TransactionFilter> {
        return setOfNotNull(
            TransactionFilter.TRANSFER,
            TransactionFilter.EXTRINSIC.takeIf { asset.isUtilityAsset }
        )
    }

    private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot, chainAsset: Chain.Asset): Boolean {
        val statemineType = chainAsset.requireStatemine()
        val moduleName = statemineType.palletNameOrDefault()
        val module = runtime.metadata.module(moduleName)

        val matchingCall = oneOf(
            module.call("transfer"),
            module.call("transfer_keep_alive"),
        )

        if (!matchingCall) return false

        val assetId = bindNumber(arguments["id"])
        return assetId == statemineType.id
    }
}
