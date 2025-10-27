package io.novafoundation.nova.feature_assets.presentation.gifts.assets.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.gifts.assets.AssetGiftsFlowFragment

@Subcomponent(
    modules = [
        AssetGiftsFlowModule::class
    ]
)
@ScreenScope
interface AssetGiftsFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetGiftsFlowComponent
    }

    fun inject(fragment: AssetGiftsFlowFragment)
}
