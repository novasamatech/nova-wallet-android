package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.novacard.waiting.WaitingNovaCardTopUpFragment

@Subcomponent(
    modules = [
        WaitingNovaCardTopUpModule::class
    ]
)
@ScreenScope
interface WaitingNovaCardTopUpComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): WaitingNovaCardTopUpComponent
    }

    fun inject(fragment: WaitingNovaCardTopUpFragment)
}
