package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.TinderGovBasketFragment

@Subcomponent(
    modules = [
        TinderGovBasketModule::class
    ]
)
@ScreenScope
interface TinderGovBasketComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): TinderGovBasketComponent
    }

    fun inject(fragment: TinderGovBasketFragment)
}
