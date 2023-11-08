package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.firstExistingModuleName
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class OrmlAssetTransfers(
    chainRegistry: ChainRegistry,
    assetSourceRegistry: AssetSourceRegistry,
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
    enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) : BaseAssetTransfers(chainRegistry, assetSourceRegistry, extrinsicService, phishingValidationFactory, enoughTotalToStayAboveEDValidationFactory) {

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

    override val validationSystem: AssetTransfersValidationSystem = defaultValidationSystem()

    override suspend fun totalCanDropBelowMinimumBalance(chainAsset: Chain.Asset): Boolean {
        return true
    }

    override fun totalCanDropBelowMinimumBalanceFlow(chainAsset: Chain.Asset): Flow<Boolean> {
        return flowOf(true)
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
