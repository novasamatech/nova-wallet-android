package io.novafoundation.nova.feature_settings_impl.presentation.assetIcons.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.assetIcons.AppearanceFragment

@Subcomponent(
    modules = [
        AppearanceModule::class
    ]
)
@ScreenScope
interface AppearanceComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): AppearanceComponent
    }

    fun inject(fragment: AppearanceFragment)
}
