package io.novafoundation.nova.feature_pay_impl.presentation.shop.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_pay_impl.presentation.shop.main.ShopFragment

@Subcomponent(
    modules = [
        ShopModule::class
    ]
)
@ScreenScope
interface ShopComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): ShopComponent
    }

    fun inject(welcomeFragment: ShopFragment)
}
