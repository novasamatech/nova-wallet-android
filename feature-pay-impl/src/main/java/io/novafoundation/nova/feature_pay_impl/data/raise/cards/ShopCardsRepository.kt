package io.novafoundation.nova.feature_pay_impl.data.raise.cards

import io.novafoundation.nova.common.utils.NetworkStateService
import io.novafoundation.nova.common.utils.recoverWithDispatcher
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.RaiseBrandsConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.RaiseCardsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.model.RaiseCardsResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseAmountConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseCurrencyConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseDateConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.convertFromApiCurrency
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.constructCredentialsOrNull
import io.novafoundation.nova.feature_pay_impl.domain.cards.BrandedShopCard
import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCard
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount
import kotlinx.coroutines.Dispatchers

interface ShopCardsRepository {

    suspend fun getCards(): Result<List<BrandedShopCard>>
}

class RealShopCardsRepository(
    private val cardsApi: RaiseCardsApi,
    private val raiseAmountConverter: RaiseAmountConverter,
    private val raiseCurrencyConverter: RaiseCurrencyConverter,
    private val raiseBrandsConverter: RaiseBrandsConverter,
    private val raiseDateConverter: RaiseDateConverter,
    private val networkStateService: NetworkStateService,
) : ShopCardsRepository {

    companion object {

        const val RAISE_MAX_PAGE_SIZE = 500
    }

    override suspend fun getCards() = networkStateService.recoverWithDispatcher(Dispatchers.IO) {
        val response = cardsApi.getCards(RAISE_MAX_PAGE_SIZE)
        response.toDomain()
    }

    private suspend fun RaiseCardsResponse.toDomain(): List<BrandedShopCard> {
        val brandsById = included.orEmpty().associateBy { it.id }

        return data.mapNotNull { card ->
            runCatching {
                val brand = brandsById[card.attributes.brandId] ?: return@mapNotNull null

                BrandedShopCard(
                    card = ShopCard(
                        fiatBalance = FiatAmount(
                            price = raiseAmountConverter.convertFromApiCurrency(card.attributes.balance),
                            currency = raiseCurrencyConverter.convertFromApiCurrency(card.attributes.currency) ?: return@mapNotNull null
                        ),
                        id = card.id,
                        credentials = card.attributes.constructCredentialsOrNull() ?: return@mapNotNull null,
                        expiration = raiseDateConverter.convertFromApiDate(card.attributes.expiresAt)
                    ),
                    brand = raiseBrandsConverter.convertBrandResponseToBrand(brand) ?: return@mapNotNull null
                )
            }.getOrNull()
        }
    }
}
