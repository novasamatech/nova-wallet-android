package io.novafoundation.nova.feature_assets.presentation.balance.filters.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.balance.filters.AssetFiltersFragment

@Subcomponent(
    modules = [
        AssetFiltersModule::class
    ]
)
@ScreenScope
interface AssetFiltersComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetFiltersComponent
    }

    fun inject(fragment: AssetFiltersFragment)
}
