package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerPayload

@Subcomponent(
    modules = [
        ConfirmSetControllerModule::class
    ]
)
@ScreenScope
interface ConfirmSetControllerComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmSetControllerPayload,
        ): ConfirmSetControllerComponent
    }

    fun inject(fragment: ConfirmSetControllerFragment)
}
