package io.novafoundation.nova.feature_assets.presentation.send.common

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import java.math.BigDecimal

fun buildAssetTransfer(
    metaAccount: MetaAccount,
    feePaymentCurrency: FeePaymentCurrency,
    origin: ChainWithAsset,
    destination: ChainWithAsset,
    amount: BigDecimal,
    transferringMaxAmount: Boolean,
    address: String,
): AssetTransfer {
    return BaseAssetTransfer(
        sender = metaAccount,
        recipient = address,
        originChain = origin.chain,
        originChainAsset = origin.asset,
        destinationChain = destination.chain,
        destinationChainAsset = destination.asset,
        amount = amount,
        transferringMaxAmount = transferringMaxAmount,
        feePaymentCurrency = feePaymentCurrency
    )
}
