package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload

@Subcomponent(
    modules = [
        ConfirmRebondModule::class
    ]
)
@ScreenScope
interface ConfirmRebondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmRebondPayload,
        ): ConfirmRebondComponent
    }

    fun inject(fragment: ConfirmRebondFragment)
}
