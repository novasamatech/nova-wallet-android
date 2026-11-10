package io.novafoundation.nova.app.root.presentation.main.swaps.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.main.swaps.SwapsTabFragment
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(modules = [SwapsTabModule::class])
@ScreenScope
interface SwapsTabComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): SwapsTabComponent
    }

    fun inject(fragment: SwapsTabFragment)
}
