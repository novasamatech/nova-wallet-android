package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.orml

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.ext.requireOrml
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class OrmlAssetTransfers(
    chainRegistry: ChainRegistry,
    balanceSourceProvider: BalanceSourceProvider,
    extrinsicService: ExtrinsicService
) : BaseAssetTransfers(chainRegistry, balanceSourceProvider, extrinsicService) {

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        ormlTransfer(
            chainAsset = transfer.chainAsset,
            target = transfer.recipient,
            amount = transfer.amountInPlanks
        )
    }

    override val transferFunction: Pair<String, String> = Modules.CURRENCIES to "transfer"

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        // flag from chains json AND existence of module & function in runtime metadata
        return chainAsset.requireOrml().transfersEnabled && super.areTransfersEnabled(chainAsset)
    }

    override val validationSystem: AssetTransfersValidationSystem = defaultValidationSystem(
        removeAccountBehavior = { WillRemoveAccount.WillBurnDust }
    )

    private fun ExtrinsicBuilder.ormlTransfer(
        chainAsset: Chain.Asset,
        target: AccountId,
        amount: BigInteger
    ) {
        call(
            moduleName = Modules.CURRENCIES,
            callName = "transfer",
            arguments = mapOf(
                "dest" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "currency_id" to chainAsset.ormlCurrencyId(runtime),
                "amount" to amount
            )
        )
    }
}
