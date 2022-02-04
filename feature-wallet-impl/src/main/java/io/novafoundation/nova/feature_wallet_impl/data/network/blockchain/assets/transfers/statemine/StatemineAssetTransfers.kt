package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.statemine

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.amountInPlanks
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

class StatemineAssetTransfers(
    chainRegistry: ChainRegistry,
    balanceSourceProvider: BalanceSourceProvider,
    extrinsicService: ExtrinsicService
) : BaseAssetTransfers(chainRegistry, balanceSourceProvider, extrinsicService) {

    override val validationSystem: AssetTransfersValidationSystem = defaultValidationSystem(
        removeAccountBehavior = WillRemoveAccount::WillTransferDust
    )

    override val transferFunctions = listOf(Modules.ASSETS to "transfer")

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        val chainAssetType = transfer.chainAsset.type
        require(chainAssetType is Chain.Asset.Type.Statemine)

        statemineTransfer(
            assetId = chainAssetType.id,
            target = transfer.recipient,
            amount = transfer.amountInPlanks
        )
    }

    private fun ExtrinsicBuilder.statemineTransfer(
        assetId: BigInteger,
        target: AccountId,
        amount: BigInteger
    ) {
        call(
            moduleName = Modules.ASSETS,
            callName = "transfer",
            arguments = mapOf(
                "id" to assetId,
                "target" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, target),
                "amount" to amount
            )
        )
    }
}
