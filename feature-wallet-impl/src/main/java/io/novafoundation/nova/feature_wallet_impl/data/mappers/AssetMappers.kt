package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.core_db.model.AssetWithToken
import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TokenWithCurrency
import io.novafoundation.nova.feature_currency_api.presentation.mapper.mapCurrencyFromLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapTokenWithCurrencyToToken(
    tokenWithCurrency: TokenWithCurrency,
    chainAsset: Chain.Asset,
): Token {
    return mapTokenLocalToToken(
        tokenWithCurrency.token ?: TokenLocal.createEmpty(chainAsset.symbol, tokenWithCurrency.currency.id),
        tokenWithCurrency.currency,
        chainAsset
    )
}

fun mapTokenLocalToToken(
    tokenLocal: TokenLocal,
    currencyLocal: CurrencyLocal,
    chainAsset: Chain.Asset,
): Token {
    return with(tokenLocal) {
        Token(
            rate = rate,
            currency = mapCurrencyFromLocal(currencyLocal),
            recentRateChange = recentRateChange,
            configuration = chainAsset
        )
    }
}

fun mapAssetLocalToAsset(
    assetLocal: AssetWithToken,
    chainAsset: Chain.Asset
): Asset {
    return with(assetLocal) {
        Asset(
            token = mapTokenLocalToToken(token, assetLocal.currency, chainAsset),
            frozenInPlanks = asset.frozenInPlanks,
            freeInPlanks = asset.freeInPlanks,
            reservedInPlanks = asset.reservedInPlanks,
            bondedInPlanks = asset.bondedInPlanks,
            unbondingInPlanks = asset.unbondingInPlanks,
            redeemableInPlanks = asset.redeemableInPlanks
        )
    }
}
