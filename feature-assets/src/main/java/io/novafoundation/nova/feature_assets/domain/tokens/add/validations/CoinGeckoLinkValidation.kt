package io.novafoundation.nova.feature_assets.domain.tokens.add.validations

import android.text.TextUtils
import io.novafoundation.nova.common.utils.asQueryParam
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.data.network.coingecko.CoinGeckoLinkParser
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.CoingeckoApi

class CoinGeckoLinkValidationFactory(
    private val coingeckoApi: CoingeckoApi,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
) {
    fun <P, E> create(
        optional: Boolean,
        link: (P) -> String?,
        error: (P) -> E,
    ): CoinGeckoLinkValidation<P, E> {
        return CoinGeckoLinkValidation(
            coingeckoApi,
            coinGeckoLinkParser,
            optional,
            link,
            error,
        )
    }
}

class CoinGeckoLinkValidation<P, E>(
    private val coinGeckoApi: CoingeckoApi,
    private val coinGeckoLinkParser: CoinGeckoLinkParser,
    private val optional: Boolean,
    private val link: (P) -> String?,
    private val error: (P) -> E,
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        if (optional && TextUtils.isEmpty(link(value))) {
            return valid()
        }

        return try {
            val link = link(value)!!
            val coinGeckoContent = coinGeckoLinkParser.parse(link).getOrThrow()
            val priceId = coinGeckoContent.priceId
            val result = coinGeckoApi.getAssetPrice(setOf(priceId).asQueryParam(), "usd", false)
            result.isNotEmpty().isTrueOrError { error(value) }
        } catch (e: Exception) {
            validationError(error(value))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.validCoinGeckoLink(
    coinGeckoLinkValidationFactory: CoinGeckoLinkValidationFactory,
    optional: Boolean,
    link: (P) -> String?,
    error: (P) -> E,
) = validate(
    coinGeckoLinkValidationFactory.create(
        optional,
        link,
        error,
    )
)
