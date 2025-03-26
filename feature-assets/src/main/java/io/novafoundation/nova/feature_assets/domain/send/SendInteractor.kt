package io.novafoundation.nova.feature_assets.domain.send

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.isCrossChain
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.getXcmChain
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransfersConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.transferConfiguration
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendInteractor(
    private val walletRepository: WalletRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val crossChainTransactor: CrossChainTransactor,
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val parachainInfoRepository: ParachainInfoRepository,
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val extrinsicService: ExtrinsicService,
) {

    suspend fun getFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): TransferFee = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val fees = with(crossChainTransfersUseCase) {
                extrinsicService.estimateFee(transfer, cachingScope = null)
            }

            val originFee = OriginFee(
                submissionFee = fees.submissionFee,
                deliveryFee = fees.postSubmissionByAccount,
            )

            TransferFee(originFee, fees.postSubmissionFromAmount)
        } else {
            TransferFee(
                originFee = getOriginFee(transfer, coroutineScope),
                crossChainFee = null
            )
        }
    }

    suspend fun getOriginFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): OriginFee = withContext(Dispatchers.Default) {
        OriginFee(getSubmissionFee(transfer, coroutineScope), null)
    }

    suspend fun getSubmissionFee(transfer: AssetTransfer, coroutineScope: CoroutineScope): SubmissionFee = withContext(Dispatchers.Default) {
        getAssetTransfers(transfer).calculateFee(transfer, coroutineScope = coroutineScope)
    }

    suspend fun performTransfer(
        transfer: WeightedAssetTransfer,
        originFee: OriginFee,
        crossChainFee: FeeBase?,
        coroutineScope: CoroutineScope
    ): Result<*> = withContext(Dispatchers.Default) {
        if (transfer.isCrossChain) {
            val config = crossChainTransfersRepository.getConfiguration().configurationFor(transfer)!!

            with(extrinsicService) {
                crossChainTransactor.performTransfer(config, transfer, crossChainFee!!.amount)
            }
        } else {
            val submissionFee = originFee.submissionFee

            getAssetTransfers(transfer).performTransfer(transfer, coroutineScope)
                .onSuccess { submission ->
                    // Insert used fee regardless of who paid it
                    walletRepository.insertPendingTransfer(submission.hash, transfer, submissionFee)
                }
        }
    }

    fun validationSystemFor(transfer: AssetTransfer, coroutineScope: CoroutineScope) = if (transfer.isCrossChain) {
        crossChainTransactor.validationSystem
    } else {
        assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers.getValidationSystem(coroutineScope)
    }

    suspend fun areTransfersEnabled(asset: Chain.Asset) = assetSourceRegistry.sourceFor(asset).transfers.areTransfersEnabled(asset)

    private fun getAssetTransfers(transfer: AssetTransfer) = assetSourceRegistry.sourceFor(transfer.originChainAsset).transfers

    private suspend fun CrossChainTransfersConfiguration.configurationFor(transfer: AssetTransfer) = transferConfiguration(
        originChain = parachainInfoRepository.getXcmChain(transfer.originChain),
        originAsset = transfer.originChainAsset,
        destinationChain = parachainInfoRepository.getXcmChain(transfer.destinationChain),
    )
}
