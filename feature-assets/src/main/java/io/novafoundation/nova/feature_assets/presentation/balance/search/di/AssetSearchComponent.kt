package io.novafoundation.nova.feature_assets.presentation.balance.search.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.balance.search.AssetSearchFragment

@Subcomponent(
    modules = [
        AssetSearchModule::class
    ]
)
@ScreenScope
interface AssetSearchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetSearchComponent
    }

    fun inject(fragment: AssetSearchFragment)
}
