package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftAccount
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDeposit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystem
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CreateGiftInteractor {
    fun validationSystemFor(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): AssetTransfersValidationSystem

    suspend fun getFee(
        model: CreateGiftModel,
        transferAllToCreateGift: Boolean,
        coroutineScope: CoroutineScope
    ): GiftFee

    suspend fun getExistentialDeposit(chainAsset: Chain.Asset): BigDecimal

    fun randomGiftAccount(): GiftAccount
}

class RealCreateGiftInteractor(
    private val assetSourceRegistry: AssetSourceRegistry
) : CreateGiftInteractor {

    override fun validationSystemFor(chainAsset: Chain.Asset, coroutineScope: CoroutineScope): AssetTransfersValidationSystem {
        return getAssetTransfers(chainAsset)
            .getValidationSystem(coroutineScope)
    }

    override suspend fun getFee(
        model: CreateGiftModel,
        transferAllToCreateGift: Boolean,
        coroutineScope: CoroutineScope
    ): GiftFee = withContext(Dispatchers.Default) {
        val claimGiftFee = getSubmissionFee(
            model = model,
            transferMax = true,
            coroutineScope = coroutineScope
        )
        val claimFeeAmount = model.chainAsset.amountFromPlanks(claimGiftFee.amount)

        val createGiftFee = getSubmissionFee(
            model = model.copy(amount = model.amount + claimFeeAmount),
            transferMax = transferAllToCreateGift,
            coroutineScope = coroutineScope
        )
        GiftFee(
            createGiftFee = createGiftFee,
            claimGiftFee = claimGiftFee
        )
    }

    override suspend fun getExistentialDeposit(chainAsset: Chain.Asset): BigDecimal {
        return assetSourceRegistry.existentialDeposit(chainAsset)
    }

    override fun randomGiftAccount(): GiftAccount {

    }

    private suspend fun getSubmissionFee(model: CreateGiftModel, transferMax: Boolean, coroutineScope: CoroutineScope): SubmissionFee {
        return withContext(Dispatchers.Default) {
            val transfer = model.mapToAssetTransfer(transferMax)
            getAssetTransfers(model.chainAsset).calculateFee(transfer, coroutineScope = coroutineScope)
        }
    }

    private fun getAssetTransfers(chainAsset: Chain.Asset) = assetSourceRegistry.sourceFor(chainAsset).transfers

    private fun CreateGiftModel.mapToAssetTransfer(transferMax: Boolean) = BaseAssetTransfer(
        sender = metaAccount,
        recipient = chain.addressOf(giftAccount),
        originChain = chain,
        originChainAsset = chainAsset,
        destinationChain = chain,
        destinationChainAsset = chainAsset,
        feePaymentCurrency = FeePaymentCurrency.Asset(chainAsset),
        amount = amount,
        transferringMaxAmount = transferMax
    )
}
