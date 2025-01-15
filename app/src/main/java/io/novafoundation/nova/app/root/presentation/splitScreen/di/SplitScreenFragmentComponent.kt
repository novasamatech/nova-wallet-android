package io.novafoundation.nova.app.root.presentation.splitScreen.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenFragment
import io.novafoundation.nova.app.root.presentation.splitScreen.SplitScreenPayload
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
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SplitScreenPayload
        ): SplitScreenFragmentComponent
    }

    fun inject(fragment: SplitScreenFragment)
}
