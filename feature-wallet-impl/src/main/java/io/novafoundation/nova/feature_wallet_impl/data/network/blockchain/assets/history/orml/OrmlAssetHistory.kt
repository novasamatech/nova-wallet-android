package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.orml

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.currenciesOrNull
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.filterOwn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.runtime.ext.findAssetByOrmlCurrencyId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.status
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.metadata.call
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull

class OrmlAssetHistory(
    private val chainRegistry: ChainRegistry,
    private val eventsRepository: EventsRepository,
) : AssetHistory {

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

    override fun availableOperationFilters(asset: Chain.Asset): Set<TransactionFilter> {
        return setOf(TransactionFilter.TRANSFER)
    }

    private fun GenericCall.Instance.isTransfer(runtime: RuntimeSnapshot): Boolean {
        val transferCall = runtime.metadata.currenciesOrNull()?.callOrNull("transfer")
            ?: runtime.metadata.tokens().call("transfer")

        return instanceOf(transferCall)
    }
}
