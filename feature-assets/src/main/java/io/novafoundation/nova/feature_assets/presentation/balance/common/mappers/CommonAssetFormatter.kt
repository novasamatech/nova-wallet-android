package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatter
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionPartStyling
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel

abstract class CommonAssetFormatter(
    private val maskableValueFormatter: MaskableValueFormatter,
    private val amountFormatter: AmountFormatter
) {

    protected fun mapAssetToAssetModel(
        asset: Asset,
        balance: AssetBalance.Amount
    ): AssetModel {
        return AssetModel(
            token = mapTokenToTokenModel(asset.token),
            amount = maskableValueFormatter.format {
                amountFormatter.formatAmountToAmountModel(
                    amount = balance.amount,
                    token = asset.token,
                    config = AmountConfig(
                        includeAssetTicker = false,
                        tokenFractionPartStyling = FractionPartStyling.Styled(R.dimen.asset_balance_fraction_size)
                    )
                )
            }
        )
    }
}
