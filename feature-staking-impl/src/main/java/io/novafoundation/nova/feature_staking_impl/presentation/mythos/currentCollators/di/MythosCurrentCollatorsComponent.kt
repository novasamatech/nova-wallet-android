package io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.currentCollators.MythosCurrentCollatorsFragment

@Subcomponent(
    modules = [
        MythosCurrentCollatorsModule::class
    ]
)
@ScreenScope
interface MythosCurrentCollatorsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): MythosCurrentCollatorsComponent
    }

    fun inject(fragment: MythosCurrentCollatorsFragment)
}
