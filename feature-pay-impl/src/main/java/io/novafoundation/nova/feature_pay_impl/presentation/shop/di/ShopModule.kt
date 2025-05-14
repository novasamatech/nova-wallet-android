package io.novafoundation.nova.feature_pay_impl.presentation.shop.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.shop.ShopViewModel

@Module(includes = [ViewModelModule::class])
class ShopModule {

    @Provides
    @IntoMap
    @ViewModelKey(ShopViewModel::class)
    fun provideViewModel(
        router: PayRouter,
        shopInteractor: ShopInteractor,
        shopBrandsInteractor: ShopBrandsInteractor,
        resourceManager: ResourceManager
    ): ViewModel {
        return ShopViewModel(
            router,
            shopInteractor,
            shopBrandsInteractor,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ShopViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ShopViewModel::class.java)
    }
}
