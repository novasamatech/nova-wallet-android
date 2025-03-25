package io.novafoundation.nova.feature_assets.presentation.novacard.topup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardFragment
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload

@Subcomponent(
    modules = [
        TopUpCardModule::class
    ]
)
@ScreenScope
interface TopUpCardComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: TopUpCardPayload,
        ): TopUpCardComponent
    }

    fun inject(fragment: TopUpCardFragment)
}
