package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.feature_wallet_api.data.repository.USD_COINGECKO_ID
import io.novafoundation.nova.feature_wallet_api.data.repository.UsdRateRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class RealUsdRateRepository(
    private val tokenDao: TokenDao
) : UsdRateRepository {

    override suspend fun getUsdRate(chainAsset: Chain.Asset): BigDecimal? {
        val rate = tokenDao.getToken(chainAsset.symbol.value, USD_COINGECKO_ID)?.rate

        // The price source stores 0 for a token it has no price for
        return rate?.takeIf { it.signum() > 0 }
    }
}
