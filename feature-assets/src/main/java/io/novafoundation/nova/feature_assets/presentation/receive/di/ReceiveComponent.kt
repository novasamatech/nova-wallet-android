package io.novafoundation.nova.feature_assets.presentation.receive.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment

@Subcomponent(
    modules = [
        ReceiveModule::class
    ]
)
@ScreenScope
interface ReceiveComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AssetPayload,
        ): ReceiveComponent
    }

    fun inject(fragment: ReceiveFragment)
}
