package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetLocal.EDCountingModeLocal
import io.novafoundation.nova.core_db.model.AssetLocal.TransferableModeLocal
import io.novafoundation.nova.core_db.model.AssetWithToken
import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TokenWithCurrency
import io.novafoundation.nova.feature_currency_api.presentation.mapper.mapCurrencyFromLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapTokenWithCurrencyToToken(
    tokenWithCurrency: TokenWithCurrency,
    chainAsset: Chain.Asset,
): Token {
    return mapTokenLocalToToken(
        tokenWithCurrency.token ?: TokenLocal.createEmpty(chainAsset.symbol.value, tokenWithCurrency.currency.id),
        tokenWithCurrency.currency,
        chainAsset
    )
}

fun mapTokenLocalToToken(
    tokenLocal: TokenLocal?,
    currencyLocal: CurrencyLocal,
    chainAsset: Chain.Asset,
): Token {
    return Token(
        currency = mapCurrencyFromLocal(currencyLocal),
        coinRate = tokenLocal?.recentRateChange?.let { CoinRateChange(tokenLocal.recentRateChange.orZero(), tokenLocal.rate.orZero()) },
        configuration = chainAsset
    )
}

fun mapAssetLocalToAsset(
    assetLocal: AssetWithToken,
    chainAsset: Chain.Asset
): Asset {
    return with(assetLocal) {
        Asset(
            token = mapTokenLocalToToken(token, assetLocal.currency, chainAsset),
            frozenInPlanks = asset?.frozenInPlanks.orZero(),
            freeInPlanks = asset?.freeInPlanks.orZero(),
            reservedInPlanks = asset?.reservedInPlanks.orZero(),
            bondedInPlanks = asset?.bondedInPlanks.orZero(),
            unbondingInPlanks = asset?.unbondingInPlanks.orZero(),
            redeemableInPlanks = asset?.redeemableInPlanks.orZero(),
            transferableMode = mapTransferableModeFromLocal(asset?.transferableMode),
            edCountingMode = mapEdCountingModeFromLocal(asset?.edCountingMode)
        )
    }
}

private fun mapTransferableModeFromLocal(modeLocal: TransferableModeLocal?): TransferableMode {
    return when (modeLocal ?: AssetLocal.defaultTransferableMode()) {
        TransferableModeLocal.REGULAR -> TransferableMode.REGULAR
        TransferableModeLocal.HOLDS_AND_FREEZES -> TransferableMode.HOLDS_AND_FREEZES
    }
}

private fun mapEdCountingModeFromLocal(modeLocal: EDCountingModeLocal?): EDCountingMode {
    return when (modeLocal ?: AssetLocal.defaultEdCountingMode()) {
        EDCountingModeLocal.TOTAL -> EDCountingMode.TOTAL
        EDCountingModeLocal.FREE -> EDCountingMode.FREE
    }
}
