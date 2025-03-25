package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup.SetupUnbondMythosFragment

@Subcomponent(
    modules = [
        SetupUnbondMythosModule::class
    ]
)
@ScreenScope
interface SetupUnbondMythosComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SetupUnbondMythosComponent
    }

    fun inject(fragment: SetupUnbondMythosFragment)
}
