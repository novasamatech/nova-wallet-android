package io.novafoundation.nova.feature_pay_impl.domain.brand

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_pay_impl.data.ShopBrandsRepository
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.ShopBrand
import io.novafoundation.nova.feature_pay_impl.domain.cards.BrandedShopCard
import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCardsUseCase
import io.novafoundation.nova.feature_pay_impl.domain.common.ShopAccountSeedAccessPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ShopBrandsInteractor {

    fun observeAccountAvailableForShopping(): Flow<Boolean>

    suspend fun loadBrands(query: String, offset: PageOffset.Loadable): Result<DataPage<ShopBrand>>

    fun purchasedCards(): Flow<List<BrandedShopCard>>

    suspend fun syncPurchasedCards()
}

class RealShopBrandsInteractor(
    private val brandsRepository: ShopBrandsRepository,
    private val shopCardsUseCase: ShopCardsUseCase,
    private val accountRepository: AccountRepository,
    private val shopAccountSeedAccessPolicy: ShopAccountSeedAccessPolicy,
) : ShopBrandsInteractor {

    override fun observeAccountAvailableForShopping(): Flow<Boolean> {
        return accountRepository.selectedMetaAccountFlow().map {
            shopAccountSeedAccessPolicy.isMetaAccountValid(it)
        }
    }

    override suspend fun loadBrands(query: String, offset: PageOffset.Loadable): Result<DataPage<ShopBrand>> {
        return brandsRepository.getBrands(query, pageOffset = offset)
    }

    override fun purchasedCards(): Flow<List<BrandedShopCard>> {
        return shopCardsUseCase.raiseCards()
    }

    override suspend fun syncPurchasedCards() {
        shopCardsUseCase.syncRaise()
    }
}
