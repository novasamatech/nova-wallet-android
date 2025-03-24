package io.novafoundation.nova.feature_wallet_api.data.network.priceApi

import java.math.BigDecimal

class CoinRangeResponse(val prices: List<List<BigDecimal>>) {

    class Price(val millis: Long, val price: BigDecimal)
}
