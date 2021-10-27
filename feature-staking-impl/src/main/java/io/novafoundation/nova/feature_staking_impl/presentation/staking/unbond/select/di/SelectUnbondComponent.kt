package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.select.SelectUnbondFragment

@Subcomponent(
    modules = [
        SelectUnbondModule::class
    ]
)
@ScreenScope
interface SelectUnbondComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SelectUnbondComponent
    }

    fun inject(fragment: SelectUnbondFragment)
}
