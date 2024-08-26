package io.novafoundation.nova.feature_assets.presentation.novacard.overview.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.NovaCardFragment

@Subcomponent(
    modules = [
        NovaCardModule::class
    ]
)
@ScreenScope
interface NovaCardComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): NovaCardComponent
    }

    fun inject(fragment: NovaCardFragment)
}
