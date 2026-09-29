package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

const val USD_COINGECKO_ID = "usd"

/**
 * USD rates saved by the regular price sync, which requests USD next to the selected currency.
 * Reads the database only: works offline and never adds a request to the rate-limited price API.
 * Meant for analytics, which buckets amounts in USD regardless of the user's currency.
 */
interface UsdRateRepository {

    /** Null when no USD price was synced for this token yet, or the token has no price at all. */
    suspend fun getUsdRate(chainAsset: Chain.Asset): BigDecimal?
}

suspend fun UsdRateRepository.amountToUsd(chainAsset: Chain.Asset, amount: BigDecimal): BigDecimal? {
    return getUsdRate(chainAsset)?.multiply(amount)
}

suspend fun UsdRateRepository.planksToUsd(chainAsset: Chain.Asset, planks: BigInteger): BigDecimal? {
    return amountToUsd(chainAsset, chainAsset.amountFromPlanks(planks))
}
