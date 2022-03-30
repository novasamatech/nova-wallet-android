package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.utility

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.balances.BalanceSourceProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.BaseAssetTransfers
import io.novafoundation.nova.runtime.ext.accountIdOrDefault
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class NativeAssetTransfers(
    chainRegistry: ChainRegistry,
    balanceSourceProvider: BalanceSourceProvider,
    extrinsicService: ExtrinsicService,
    phishingValidationFactory: PhishingValidationFactory,
) : BaseAssetTransfers(chainRegistry, balanceSourceProvider, extrinsicService, phishingValidationFactory) {

    override val validationSystem: AssetTransfersValidationSystem = defaultValidationSystem(
        removeAccountBehavior = { WillRemoveAccount.WillBurnDust }
    )

    override fun ExtrinsicBuilder.transfer(transfer: AssetTransfer) {
        nativeTransfer(
            accountId = transfer.chain.accountIdOrDefault(transfer.recipient),
            amount = transfer.chainAsset.planksFromAmount(transfer.amount)
        )
    }

    override val transferFunctions = listOf(Modules.BALANCES to "transfer")
}
