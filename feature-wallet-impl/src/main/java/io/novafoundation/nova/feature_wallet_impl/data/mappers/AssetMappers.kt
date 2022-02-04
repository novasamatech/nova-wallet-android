package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.common.utils.formatAsChange
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.core_db.model.AssetWithToken
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import io.novafoundation.nova.feature_wallet_impl.presentation.model.TokenModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

fun mapTokenLocalToToken(
    tokenLocal: TokenLocal,
    chainAsset: Chain.Asset,
): Token {
    return with(tokenLocal) {
        Token(
            configuration = chainAsset,
            dollarRate = dollarRate,
            recentRateChange = recentRateChange
        )
    }
}

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        val rateChange = token.recentRateChange

        val changeColorRes = when {
            rateChange == null -> R.color.gray2
            rateChange.isNonNegative -> R.color.green
            else -> R.color.red
        }

        TokenModel(
            configuration = configuration,
            dollarRate = (dollarRate ?: BigDecimal.ZERO).formatAsCurrency(),
            recentRateChange = (recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = changeColorRes
        )
    }
}

fun mapAssetLocalToAsset(
    assetLocal: AssetWithToken,
    chainAsset: Chain.Asset
): Asset {
    return with(assetLocal) {
        Asset(
            token = mapTokenLocalToToken(token, chainAsset),
            frozenInPlanks = asset.frozenInPlanks,
            freeInPlanks = asset.freeInPlanks,
            reservedInPlanks = asset.reservedInPlanks,
            bondedInPlanks = asset.bondedInPlanks,
            unbondingInPlanks = asset.unbondingInPlanks,
            redeemableInPlanks = asset.redeemableInPlanks
        )
    }
}

fun mapAssetToAssetModel(asset: Asset): AssetModel {
    return with(asset) {
        AssetModel(
            token = mapTokenToTokenModel(token),
            total = total,
            bonded = bonded,
            locked = locked,
            available = transferable,
            reserved = reserved,
            redeemable = redeemable,
            unbonding = unbonding,
            dollarAmount = dollarAmount
        )
    }
}
