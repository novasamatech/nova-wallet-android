package io.novafoundation.nova.feature_pay_impl.presentation.shop.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_pay_impl.presentation.shop.search.ShopSearchFragment

@Subcomponent(
    modules = [
        ShopSearchModule::class
    ]
)
@ScreenScope
interface ShopSearchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ShopSearchComponent
    }

    fun inject(welcomeFragment: ShopSearchFragment)
}
