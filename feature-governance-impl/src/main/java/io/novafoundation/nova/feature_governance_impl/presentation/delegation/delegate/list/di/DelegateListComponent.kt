package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.DelegateListFragment

@Subcomponent(
    modules = [
        DelegateListModule::class
    ]
)
@ScreenScope
interface DelegateListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): DelegateListComponent
    }

    fun inject(fragment: DelegateListFragment)
}
