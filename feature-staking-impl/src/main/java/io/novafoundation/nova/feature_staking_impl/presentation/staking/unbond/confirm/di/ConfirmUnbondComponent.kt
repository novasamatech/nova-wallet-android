package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload

@Subcomponent(
    modules = [
        ConfirmUnbondModule::class
    ]
)
@ScreenScope
interface ConfirmUnbondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmUnbondPayload,
        ): ConfirmUnbondComponent
    }

    fun inject(fragment: ConfirmUnbondFragment)
}
