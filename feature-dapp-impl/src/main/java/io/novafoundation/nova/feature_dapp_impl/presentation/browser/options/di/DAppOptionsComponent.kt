package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsBottomSheet
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload

@Subcomponent(
    modules = [
        DAppOptionsModule::class
    ]
)
@ScreenScope
interface DAppOptionsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance argument: DAppOptionsPayload
        ): DAppOptionsComponent
    }

    fun inject(fragment: DAppOptionsBottomSheet)
}
