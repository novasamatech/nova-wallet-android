package io.novafoundation.nova.feature_pay_impl.domain.cards

import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.ShopCardsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface ShopCardsUseCase {

    fun raiseCards(): Flow<List<BrandedShopCard>>

    fun cardDetails(cardId: String): Flow<BrandedShopCard?>

    suspend fun syncRaise(): Result<Unit>
}

class RealShopCardsUseCase @Inject constructor(
    private val shopCardsRepository: ShopCardsRepository
) : ShopCardsUseCase {

    private val raiseCards = singleReplaySharedFlow<List<BrandedShopCard>>()

    override fun raiseCards() = raiseCards

    override fun cardDetails(cardId: String) = raiseCards.map { it.find { brandedCard -> brandedCard.card.id == cardId } }

    override suspend fun syncRaise(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            shopCardsRepository.getCards()
                .onSuccess { raiseCards.emit(it) }
                .coerceToUnit()
        }
    }
}
