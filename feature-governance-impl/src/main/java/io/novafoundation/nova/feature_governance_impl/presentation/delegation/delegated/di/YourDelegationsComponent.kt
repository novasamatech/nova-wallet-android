package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated.YourDelegationsFragment

@Subcomponent(
    modules = [
        YourDelegationsModule::class
    ]
)
@ScreenScope
interface YourDelegationsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): YourDelegationsComponent
    }

    fun inject(fragment: YourDelegationsFragment)
}
