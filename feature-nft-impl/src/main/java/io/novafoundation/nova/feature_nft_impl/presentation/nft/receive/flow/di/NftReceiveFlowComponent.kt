package io.novafoundation.nova.feature_assets.presentation.receive.flow.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.receive.flow.NftReceiveFlowFragment

@Subcomponent(
    modules = [
        NftReceiveFlowModule::class
    ]
)
@ScreenScope
interface NftReceiveFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NftReceiveFlowComponent
    }

    fun inject(fragment: NftReceiveFlowFragment)
}
