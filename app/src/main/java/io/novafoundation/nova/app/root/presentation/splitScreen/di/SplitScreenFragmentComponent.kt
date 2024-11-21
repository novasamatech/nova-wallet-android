package io.novafoundation.nova.app.root.presentation.splitScreen.di

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.main.MainFragment
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenFragment
import io.novafoundation.nova.common.di.scope.ScreenScope

@Subcomponent(
    modules = [
        SplitScreenFragmentModule::class
    ]
)
@ScreenScope
interface SplitScreenFragmentComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SplitScreenFragmentComponent
    }

    fun inject(fragment: SplitScreenFragment)
}
