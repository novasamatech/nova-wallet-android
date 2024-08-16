package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfers
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.doNotCrossExistentialDepositInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInCommissionAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notDeadRecipientInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.notPhishingRecipient
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.positiveAmount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.recipientIsNotSystemAccount
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.sufficientTransferableBalanceToPayOriginFee
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.transfers.validations.validAddress
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.recipientCanAcceptTransfer
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.callOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import kotlinx.coroutines.CoroutineScope

abstract class BaseAssetTransfers(
    internal val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicServiceFactory: ExtrinsicService.Factory,
    private val phishingValidationFactory: PhishingValidationFactory,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) : AssetTransfers {

    protected abstract fun ExtrinsicBuilder.transfer(transfer: AssetTransfer)

    /**
     * Format: [(Module, Function)]
     * Transfers will be enabled if at least one function exists
     */
    protected abstract suspend fun transferFunctions(chainAsset: Chain.Asset): List<Pair<String, String>>

    override suspend fun performTransfer(transfer: WeightedAssetTransfer, coroutineScope: CoroutineScope): Result<ExtrinsicSubmission> {
        return extrinsicServiceFactory
            .create(coroutineScope)
            .submitExtrinsic(transfer.originChain, transfer.sender.intoOrigin()) {
                transfer(transfer)
            }
    }

    override suspend fun calculateFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): Fee {
        return extrinsicServiceFactory
            .create(coroutineScope)
            .estimateFee(transfer.originChain, TransactionOrigin.SelectedWallet) {
                transfer(transfer)
            }
    }

    override suspend fun areTransfersEnabled(chainAsset: Chain.Asset): Boolean {
        val runtime = chainRegistry.getRuntime(chainAsset.chainId)

        return transferFunctions(chainAsset).any { (module, function) ->
            runtime.metadata.moduleOrNull(module)?.callOrNull(function) != null
        }
    }

    override fun getValidationSystem(coroutineScope: CoroutineScope) = ValidationSystem {
        validAddress()
        recipientIsNotSystemAccount()

        notPhishingRecipient(phishingValidationFactory)

        positiveAmount()

        sufficientBalanceInUsedAsset()
        sufficientTransferableBalanceToPayOriginFee()

        sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

        notDeadRecipientInUsedAsset(assetSourceRegistry)
        notDeadRecipientInCommissionAsset(assetSourceRegistry)

        doNotCrossExistentialDeposit()

        recipientCanAcceptTransfer(assetSourceRegistry)
    }

    private fun AssetTransfersValidationSystemBuilder.doNotCrossExistentialDeposit() = doNotCrossExistentialDepositInUsedAsset(
        assetSourceRegistry = assetSourceRegistry,
        extraAmount = { it.transfer.amount },
    )
}
