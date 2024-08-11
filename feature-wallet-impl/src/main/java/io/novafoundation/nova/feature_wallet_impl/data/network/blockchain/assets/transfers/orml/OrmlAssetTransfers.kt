package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.firstExistingModuleName
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class OrmlAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        ormlTransfer(
            chainAsset = transfer.originChainAsset,
            target = transfer.originChain.accountIdOrDefault(transfer.recipient),
            amount = transfer.amountInPlanks
        )
    }

    override suspend fun transferFunctions(chainAsset: Chain.Asset) = listOf(
        Modules.CURRENCIES to "transfer",
        Modules.TOKENS to "transfer"
    )

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        // flag from chains json AND existence of module & function in runtime metadata
        return chainAsset.requireOrml().transfersEnabled && super.areTransfersEnabled(chainAsset)
    }

    private fun ExtrinsicBuilder.ormlTransfer(
        chainAsset: Chain.Asset,
        target: AccountId,
        amount: BigInteger
    ) {
        call(
            moduleName = runtime.metadata.firstExistingModuleName(Modules.CURRENCIES, Modules.TOKENS),
            callName = "transfer",
            arguments = mapOf(
                "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "currency_id" to chainAsset.ormlCurrencyId(runtime),
                "amount" to amount
            )
        )
    }
}
