package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.originFeeInUsedAsset
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.doNotCrossExistentialDeposit
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInCommissionAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notPhishingRecipient
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
import java.math.BigInteger

abstract class BaseAssetTransfers(
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val phishingValidationFactory: PhishingValidationFactory,
) : AssetTransfers {

    protected abstract fun ExtrinsicBuilder.transfer(transfer: AssetTransfer)

    /**
     * Format: [(Module, Function)]
     * Transfers will be enabled if at least one function exists
     */
    protected abstract val transferFunctions: List<Pair<String, String>>

    override suspend fun performTransfer(transfer: AssetTransfer): Result<String> {
        val senderAccountId = transfer.sender.accountIdIn(transfer.originChain)!!

        return extrinsicService.submitExtrinsicWithAnySuitableWallet(transfer.originChain, senderAccountId) {
            transfer(transfer)
        }
    }

    override suspend fun calculateFee(transfer: AssetTransfer): BigInteger {
        return extrinsicService.estimateFee(transfer.originChain) {
            transfer(transfer)
        }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return transferFunctions.any { (module, function) ->
            runtime.metadata.moduleOrNull(module)?.callOrNull(function) != null
        }
    }

    protected fun defaultValidationSystem(): AssetTransfersValidationSystem = ValidationSystem {
        validAddress()

        notPhishingRecipient(phishingValidationFactory)

        positiveAmount()

        sufficientTransferableBalanceToPayOriginFee()
        sufficientBalanceInUsedAsset()

        sufficientCommissionBalanceToStayAboveED(assetSourceRegistry)

        notDeadRecipientInUsedAsset(assetSourceRegistry)
        notDeadRecipientInCommissionAsset(assetSourceRegistry)

        doNotCrossExistentialDeposit()
    }

    protected fun AssetTransfersValidationSystemBuilder.sufficientBalanceInUsedAsset() = sufficientBalance(
        amount = { it.transfer.amount },
        available = { it.originUsedAsset.transferable },
        error = { _, _ -> AssetTransferValidationFailure.NotEnoughFunds.InUsedAsset },
        fee = { it.originFeeInUsedAsset }
    )

    protected fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDeposit() = doNotCrossExistentialDeposit(
        assetSourceRegistry = assetSourceRegistry,
        fee = { it.originFeeInUsedAsset },
        extraAmount = { it.transfer.amount },
    )
}
