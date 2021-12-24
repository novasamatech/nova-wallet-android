package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicFragment
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload

@Subcomponent(
    modules = [
        DAppSignExtrinsicModule::class
    ]
)
@ScreenScope
interface DAppSignExtrinsicComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: DAppSignExtrinsicPayload,
        ): DAppSignExtrinsicComponent
    }

    fun inject(fragment: DAppSignExtrinsicFragment)
}
