package io.novafoundation.nova.feature_pay_impl.presentation.shop.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.BrandsPaginationMixinFactory
import io.novafoundation.nova.feature_pay_impl.presentation.shop.search.ShopSearchViewModel

@Module(includes = [ViewModelModule::class])
class ShopSearchModule {

    @Provides
    @IntoMap
    @ViewModelKey(ShopSearchViewModel::class)
    fun provideViewModel(
        router: PayRouter,
        brandsPaginationMixinFactory: BrandsPaginationMixinFactory
    ): ViewModel {
        return ShopSearchViewModel(
            router,
            brandsPaginationMixinFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ShopSearchViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ShopSearchViewModel::class.java)
    }
}
