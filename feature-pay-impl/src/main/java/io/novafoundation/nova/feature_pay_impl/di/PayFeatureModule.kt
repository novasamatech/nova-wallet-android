package io.novafoundation.nova.feature_pay_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_pay_impl.data.RealShopRepository
import io.novafoundation.nova.feature_pay_impl.data.ShopRepository
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor

@Module
class PayFeatureModule {

    @Provides
    fun provideShopRepository(): ShopRepository = RealShopRepository()

    @Provides
    fun provideShopInteractor(
        repository: ShopRepository,
        accountRepository: AccountRepository
    ) = ShopInteractor(repository, accountRepository)
}
