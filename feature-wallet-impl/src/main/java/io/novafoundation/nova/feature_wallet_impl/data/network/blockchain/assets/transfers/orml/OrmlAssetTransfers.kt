package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.firstExistingCall
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.findAssetByOrmlCurrencyId
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigInteger

open class OrmlAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

    open val transferFunctions = listOf(
        Modules.CURRENCIES to "transfer",
        Modules.TOKENS to "transfer"
    )

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        ormlTransfer(
            chainAsset = transfer.originChainAsset,
            target = transfer.originChain.accountIdOrDefault(transfer.recipient),
            amount = transfer.amountInPlanks
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset) = transferFunctions

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        // flag from chains json AND existence of module & function in runtime metadata
        return chainAsset.requireOrml().transfersEnabled && super.areTransfersEnabled(chainAsset)
    }

    override suspend fun parseTransfer(call: GenericCall.Instance, chain: Chain): TransferParsedFromCall? {
        val isOurs = transferFunctions.any { call.instanceOf(it.first, it.second) }
        if (!isOurs) return null

        val onChainAssetId = call.arguments["currency_id"]
        val chainAsset = determineAsset(chain, onChainAssetId) ?: return null
        val amount = bindNumber(call.arguments["amount"])
        val destination = bindAccountIdentifier(call.arguments["dest"]).intoKey()

        return TransferParsedFromCall(
            amount = chainAsset.withAmount(amount),
            destination = destination
        )
    }

    private suspend fun determineAsset(chain: Chain, onChainAssetId: Any?): Chain.Asset? {
        val runtime = chainRegistry.getRuntime(chain.id)
        return chain.findAssetByOrmlCurrencyId(runtime, onChainAssetId)
    }

    private fun ExtrinsicBuilder.ormlTransfer(
        chainAsset: Chain.Asset,
        target: AccountId,
        amount: BigInteger
    ) {
        val (moduleIndex, callIndex) = runtime.metadata.firstExistingCall(transferFunctions).index

        call(
            moduleIndex = moduleIndex,
            callIndex = callIndex,
            arguments = mapOf(
                "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "currency_id" to chainAsset.ormlCurrencyId(runtime),
                "amount" to amount
            )
        )
    }
}
