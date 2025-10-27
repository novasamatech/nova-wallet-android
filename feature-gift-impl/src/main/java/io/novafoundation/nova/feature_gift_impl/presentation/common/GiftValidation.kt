package io.novafoundation.nova.feature_gift_impl.presentation.common

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.presentation.send.common.buildAssetTransfer
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

fun buildGiftValidationPayload(
    createGiftModel: CreateGiftModel,
    asset: Asset,
    transferMax: Boolean,
    feePaymentCurrency: FeePaymentCurrency,
    fee: GiftFee,
): AssetTransferPayload {
    val transferAmount = createGiftModel.amount + createGiftModel.chainAsset.amountFromPlanks(fee.claimGiftFee.amount)

    val transfer = buildTransfer(
        metaAccount = createGiftModel.metaAccount,
        chain = createGiftModel.chain,
        chainAsset = createGiftModel.chainAsset,
        amount = transferAmount,
        transferringMaxAmount = transferMax,
        feePaymentCurrency = feePaymentCurrency,
        address = createGiftModel.chain.addressOf(createGiftModel.giftAccount),
    )

    val originFee = OriginFee(
        submissionFee = fee.createGiftFee,
        deliveryFee = null
    )

    return AssetTransferPayload(
        transfer = WeightedAssetTransfer(
            assetTransfer = transfer,
            fee = originFee
        ),
        crossChainFee = null,
        originFee = originFee,
        originCommissionAsset = asset,
        originUsedAsset = asset
    )
}

private fun buildTransfer(
    metaAccount: MetaAccount,
    chain: Chain,
    chainAsset: Chain.Asset,
    feePaymentCurrency: FeePaymentCurrency,
    amount: BigDecimal,
    transferringMaxAmount: Boolean,
    address: String,
): AssetTransfer {
    val chainWithAsset = ChainWithAsset(chain, chainAsset)
    return buildAssetTransfer(
        metaAccount = metaAccount,
        feePaymentCurrency = feePaymentCurrency,
        origin = chainWithAsset,
        destination = chainWithAsset,
        amount = amount,
        transferringMaxAmount = transferringMaxAmount,
        address = address
    )
}
