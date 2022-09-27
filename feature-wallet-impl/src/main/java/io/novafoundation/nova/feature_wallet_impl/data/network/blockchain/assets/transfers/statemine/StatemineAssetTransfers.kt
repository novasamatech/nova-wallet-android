package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class StatemineAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory) {

    override val validationSystem: AssetTransfersValidationSystem = defaultValidationSystem()

    override suspend fun transferFunctions(chainAsset: Chain.Asset): List<Pair<String, String>> {
        val type = chainAsset.requireStatemine()

        return listOf(type.palletNameOrDefault() to "transfer")
    }

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        val chainAssetType = transfer.originChainAsset.type
        require(chainAssetType is Chain.Asset.Type.Statemine)

        statemineTransfer(
            assetType = chainAssetType,
            target = transfer.originChain.accountIdOrDefault(transfer.recipient),
            amount = transfer.amountInPlanks
        )
    }

    private fun ExtrinsicBuilder.statemineTransfer(
        assetType: Chain.Asset.Type.Statemine,
        target: AccountId,
        amount: BigInteger
    ) {
        call(
            moduleName = assetType.palletNameOrDefault(),
            callName = "transfer",
            arguments = mapOf(
                "id" to assetType.id,
                "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "amount" to amount
            )
        )
    }
}
