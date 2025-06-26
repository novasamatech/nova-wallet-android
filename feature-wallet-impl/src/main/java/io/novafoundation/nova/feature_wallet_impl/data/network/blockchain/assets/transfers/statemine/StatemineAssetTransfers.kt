package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdentifier
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.model.TransferParsedFromCall
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.statemine.canAcceptFunds
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.bindAssetAccountOrEmpty
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.common.statemineModule
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.findAssetByStatemineAssetId
import io.novafoundation.nova.runtime.ext.findStatemineAssets
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

class StatemineAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicServiceFactory: ExtrinsicService.Factory,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val remoteStorage: StorageDataSource
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicServiceFactory, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

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

    override suspend fun recipientCanAcceptTransfer(chainAsset: Chain.Asset, recipient: AccountId): Boolean {
        val statemineType = chainAsset.requireStatemine()

        val assetAccount = remoteStorage.query(chainAsset.chainId) {
            runtime.metadata.statemineModule(statemineType).storage("Account").query(
                statemineType.prepareIdForEncoding(runtime),
                recipient,
                binding = ::bindAssetAccountOrEmpty
            )
        }

        return assetAccount.canAcceptFunds
    }

    override suspend fun parseTransfer(call: GenericCall.Instance, chain: Chain): TransferParsedFromCall? {
        if (!checkIsOurCall(call, chain)) return null

        val onChainAssetId = call.arguments["id"]
        val chainAsset = determineAsset(chain, onChainAssetId) ?: return null
        val amount = bindNumber(call.arguments["amount"])
        val destination = bindAccountIdentifier(call.arguments["target"]).intoKey()

        return TransferParsedFromCall(
            amount = chainAsset.withAmount(amount),
            destination = destination
        )
    }

    private suspend fun determineAsset(chain: Chain, onChainAssetId: Any?): Chain.Asset? {
        val runtime = chainRegistry.getRuntime(chain.id)
        return chain.findAssetByStatemineAssetId(runtime, onChainAssetId)
    }

    private fun checkIsOurCall(call: GenericCall.Instance, chain: Chain): Boolean {
        if (call.function.name != "transfer") return false

        val allStatemineAssetsOnChain = chain.findStatemineAssets()
        return allStatemineAssetsOnChain.any { it.requireStatemine().palletNameOrDefault() == call.module.name }
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
                "id" to assetType.prepareIdForEncoding(runtime),
                "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "amount" to amount
            )
        )
    }
}
