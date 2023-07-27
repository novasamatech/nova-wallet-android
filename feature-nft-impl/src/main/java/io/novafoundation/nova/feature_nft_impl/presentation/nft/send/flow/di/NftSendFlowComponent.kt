package io.novafoundation.nova.feature_assets.presentation.send.flow.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.flow.NftSendFlowFragment

@Subcomponent(
    modules = [
        NftSendFlowModule::class
    ]
)
@ScreenScope
interface NftSendFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): NftSendFlowComponent
    }

    fun inject(fragment: NftSendFlowFragment)
}
