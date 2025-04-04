package io.novafoundation.nova.feature_assets.presentation.topup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressFragment
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressPayload

@Subcomponent(
    modules = [
        TopUpAddressModule::class
    ]
)
@ScreenScope
interface TopUpAddressComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: TopUpAddressPayload,
        ): TopUpAddressComponent
    }

    fun inject(fragment: TopUpAddressFragment)
}
