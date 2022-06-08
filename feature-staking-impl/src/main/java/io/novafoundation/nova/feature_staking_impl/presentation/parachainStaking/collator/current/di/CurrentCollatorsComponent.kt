package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.CurrentCollatorsFragment

@Subcomponent(
    modules = [
        CurrentCollatorsModule::class
    ]
)
@ScreenScope
interface CurrentCollatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): CurrentCollatorsComponent
    }

    fun inject(fragment: CurrentCollatorsFragment)
}
