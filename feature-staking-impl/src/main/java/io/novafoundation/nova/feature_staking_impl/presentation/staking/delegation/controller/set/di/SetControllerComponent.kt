package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.set.SetControllerFragment

@Subcomponent(
    modules = [
        SetControllerModule::class
    ]
)
@ScreenScope
interface SetControllerComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance fragment: Fragment): SetControllerComponent
    }

    fun inject(fragment: SetControllerFragment)
}
