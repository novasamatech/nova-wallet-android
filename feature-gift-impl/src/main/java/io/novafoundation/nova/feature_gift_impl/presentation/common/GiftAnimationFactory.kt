package io.novafoundation.nova.feature_gift_impl.presentation.common

import androidx.annotation.RawRes
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_gift_impl.R

private enum class GiftKnownTicker {
    DOT, KSM, USDT, HDX, AZERO, ASTR, UNKNOW;

    companion object {
        fun tickerOrUnknown(symbol: TokenSymbol): GiftKnownTicker {
            return GiftKnownTicker.entries.firstOrNull { it.name.contains(symbol.value) } ?: UNKNOW
        }
    }
}

interface GiftAnimationFactory {

    @RawRes
    fun getAnimationForAsset(symbol: TokenSymbol): Int
}

class PackingGiftAnimationFactory : GiftAnimationFactory {

    override fun getAnimationForAsset(symbol: TokenSymbol): Int {
        val ticker = GiftKnownTicker.tickerOrUnknown(symbol)
        return when (ticker) {
            GiftKnownTicker.DOT -> R.raw.dot_packing
            GiftKnownTicker.KSM -> R.raw.ksm_packing
            GiftKnownTicker.USDT -> R.raw.usdt_packing
            GiftKnownTicker.HDX -> R.raw.hdx_packing
            GiftKnownTicker.AZERO -> R.raw.azero_packing
            GiftKnownTicker.ASTR -> R.raw.astr_packing
            GiftKnownTicker.UNKNOW -> R.raw.default_packing
        }
    }
}

class UnpackingGiftAnimationFactory : GiftAnimationFactory {

    override fun getAnimationForAsset(symbol: TokenSymbol): Int {
        val ticker = GiftKnownTicker.tickerOrUnknown(symbol)
        return when (ticker) {
            GiftKnownTicker.DOT -> R.raw.dot_upacking
            GiftKnownTicker.KSM -> R.raw.ksm_unpacking
            GiftKnownTicker.USDT -> R.raw.usdt_unpacking
            GiftKnownTicker.HDX -> R.raw.hdx_unpacking
            GiftKnownTicker.AZERO -> R.raw.azero_unpacking
            GiftKnownTicker.ASTR -> R.raw.astr_unpacking
            GiftKnownTicker.UNKNOW -> R.raw.default_unpacking
        }
    }
}
