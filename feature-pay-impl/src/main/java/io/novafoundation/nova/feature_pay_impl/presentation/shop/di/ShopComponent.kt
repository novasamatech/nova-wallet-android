package io.novafoundation.nova.feature_pay_impl.presentation.shop.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_pay_impl.presentation.main.PayMainFragment
import io.novafoundation.nova.feature_pay_impl.presentation.shop.ShopFragment

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
