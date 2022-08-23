package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.core_db.model.AssetWithToken
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapTokenLocalToToken(
    tokenLocal: TokenLocal,
    chainAsset: Chain.Asset,
): Token {
    return with(tokenLocal) {
        Token(
            configuration = chainAsset,
            rate = rate,
            recentRateChange = recentRateChange
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
